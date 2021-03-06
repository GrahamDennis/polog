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

package me.grahamdennis.pubsub.core;

import io.reactivex.Flowable;
import io.reactivex.FlowableOperator;
import io.reactivex.processors.ReplayProcessor;
import me.grahamdennis.pubsub.appendonlylog.AppendOnlyLogMessage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class SimpleLogStream implements LogStream {

    private final ReplayProcessor<AppendOnlyLogMessage<String>> messages;
    private long nextMessageId;

    public static SimpleLogStream create() {
        return new SimpleLogStream(ReplayProcessor.create());
    }

    public SimpleLogStream(ReplayProcessor<AppendOnlyLogMessage<String>> messages) {
        this.messages = messages;
        this.nextMessageId = 0;
    }

    @Override
    public FlowableOperator<Long, String> storeToLogStreamOperator() {
        return observer -> new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                observer.onSubscribe(subscription);
            }

            @Override
            public void onNext(String message) {
                long messageId = addMessage(message);
                observer.onNext(messageId);
            }

            @Override
            public void onError(Throwable error) {
                observer.onError(error);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        };
    }

    @Override
    public Flowable<AppendOnlyLogMessage<String>> fromBeginning() {
        return messages;
    }

    @Override
    public Flowable<AppendOnlyLogMessage<String>> afterMessageId(long lastSeenMessageId) {
        return messages.skip(lastSeenMessageId + 1);
    }

    @Override
    public void stop() {
        synchronized (this) {
            messages.onComplete();
        }
    }

    private long addMessage(String messageString) {
        synchronized (this) {
            long messageId = nextMessageId++;
            messages.onNext(AppendOnlyLogMessage.of(messageId, messageString));
            return messageId;
        }
    }
}
