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
import me.grahamdennis.pubsub.appendonlylog.AppendOnlyLog;
import me.grahamdennis.pubsub.appendonlylog.AppendOnlyLogFlowable;
import me.grahamdennis.pubsub.appendonlylog.AppendOnlyLogMessage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class AppendOnlyLogStream implements LogStream {

    private final AppendOnlyLog<String> appendOnlyLog;

    public AppendOnlyLogStream(AppendOnlyLog<String> appendOnlyLog) {
        this.appendOnlyLog = appendOnlyLog;
    }

    @Override
    public FlowableOperator<Long, String> storeToLogStreamOperator() {
        return observer -> new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) {
                observer.onSubscribe(s);
            }

            @Override
            public void onNext(String s) {
                long messageId = appendOnlyLog.append(s);
                observer.onNext(messageId);
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        };
    }

    @Override
    public Flowable<AppendOnlyLogMessage<String>> fromBeginning() {
        return afterMessageId(-1L);
    }

    @Override
    public Flowable<AppendOnlyLogMessage<String>> afterMessageId(long lastSeenMessageId) {
        return new AppendOnlyLogFlowable<>(appendOnlyLog, lastSeenMessageId + 1);
    }

    @Override
    public void stop() {
        // FIXME(gdennis): implement
    }
}
