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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class SimpleLogStream implements LogStream {

    private final ReplayProcessor<Message> messages;
    private long nextMessageId;

    public static SimpleLogStream create() {
        return new SimpleLogStream(ReplayProcessor.create());
    }

    public SimpleLogStream(ReplayProcessor<Message> messages) {
        this.messages = messages;
        this.nextMessageId = 0;
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
                long messageId = addMessage(s);
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
    public Flowable<Message> fromBeginning() {
        return messages;
    }

    @Override
    public Flowable<Message> afterMessageId(long lastSeenMessageId) {
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
            messages.onNext(Message.of(messageId, messageString));
            return messageId;
        }
    }
}
