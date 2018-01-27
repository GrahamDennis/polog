/*
 * (c) Copyright 2018 Graham Dennis. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.grahamdennis.pubsub.appendonlylog;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ObjLongConsumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class AppendOnlyLogFlowableSubscription<T> implements Subscription {
    private static final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                    .setNameFormat("append-only-log-pool-%d")
                    .build()));

    private final AppendOnlyLog<T> appendOnlyLog;
    private final AtomicLong currentOffset;
    private final Subscriber<? super AppendOnlyLogMessage<T>> subscriber;

    private final AtomicLong requested = new AtomicLong();
    private final AtomicInteger pumpInProgress = new AtomicInteger();
    private AppendOnlyLogSubscriber appendOnlyLogSubscriber = new AppendOnlyLogSubscriber();

    private final AtomicInteger subscribedToLog = new AtomicInteger();

    private volatile boolean cancelled;
    private volatile boolean done;
    private Throwable error;

    public AppendOnlyLogFlowableSubscription(AppendOnlyLog<T> appendOnlyLog,
            long startingOffset,
            Subscriber<? super AppendOnlyLogMessage<T>> subscriber) {
        this.appendOnlyLog = appendOnlyLog;
        this.currentOffset = new AtomicLong(startingOffset);
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            BackpressureHelper.add(requested, n);
            requestPump();
        }
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            appendOnlyLog.unsubscribeToChanges(appendOnlyLogSubscriber);
        }
    }

    private void requestPump() {
        if (pumpInProgress.getAndIncrement() == 0) {
            executorService.submit(this::pump);
        }
    }

    private void pump() {
        int missed = 1;

        pump:
        do {
            if (checkTerminated()) {
                return;
            }

            if ((subscribedToLog.get() & 1) == 1) {
                continue;
            }

            long r = requested.get();
            final long initialOffset = currentOffset.get();
            long offset = initialOffset;

            while (r > 0) {
                if (checkTerminated()) {
                    return;
                }

                Optional<T> optionalValue = appendOnlyLog.getOrElseSubscribe(offset, appendOnlyLogSubscriber);
                if (optionalValue.isPresent()) {
                    AppendOnlyLogMessage<T> message = AppendOnlyLogMessage.of(offset++, optionalValue.get());
                    subscriber.onNext(message);
                    if (r != Long.MAX_VALUE) {
                        r = requested.decrementAndGet();
                    }
                } else {
                    subscribedToLog.incrementAndGet();
                    continue pump;
                }
            }

            currentOffset.compareAndSet(initialOffset, offset);
        } while ((missed = pumpInProgress.addAndGet(-missed)) != 0);
    }

    private boolean checkTerminated() {
        if (cancelled) {
            return true;
        }
        if (done) {
            Throwable e = error;
            if (e != null) {
                subscriber.onError(e);
            } else {
                subscriber.onComplete();
            }
            return true;
        }
        return false;
    }

    private class AppendOnlyLogSubscriber implements ObjLongConsumer<T> {
        private volatile boolean complete;

        @Override
        public void accept(T value, long offset) {
            if (complete) {
                return;
            }
            long r = requested.get();
            if (r > 0) {
                AppendOnlyLogMessage<T> logMessage = AppendOnlyLogMessage.of(offset, value);
                subscriber.onNext(logMessage);
                if (r != Long.MAX_VALUE) {
                    requested.decrementAndGet();
                }
            } else {
                complete = true;
                appendOnlyLog.unsubscribeToChanges(this);
                appendOnlyLogSubscriber = new AppendOnlyLogSubscriber();
                currentOffset.set(offset);
                subscribedToLog.incrementAndGet();
                requestPump();
            }
        }
    }
}
