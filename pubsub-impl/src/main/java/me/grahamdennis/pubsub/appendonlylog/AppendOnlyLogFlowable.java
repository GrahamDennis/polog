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

import io.reactivex.Flowable;
import org.reactivestreams.Subscriber;

public final class AppendOnlyLogFlowable<T> extends Flowable<AppendOnlyLogMessage<T>> {
    private final AppendOnlyLog<T> appendOnlyLog;
    private final long startingOffset;

    public AppendOnlyLogFlowable(AppendOnlyLog<T> appendOnlyLog, long startingOffset) {
        this.appendOnlyLog = appendOnlyLog;
        this.startingOffset = startingOffset;
    }

    @Override
    protected void subscribeActual(Subscriber<? super AppendOnlyLogMessage<T>> s) {
        s.onSubscribe(new AppendOnlyLogFlowableSubscription<>(appendOnlyLog, startingOffset, s));
    }
}
