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
import me.grahamdennis.pubsub.appendonlylog.AppendOnlyLogMessage;

public interface LogStream {

    FlowableOperator<Long, String> storeToLogStreamOperator();

    Flowable<AppendOnlyLogMessage<String>> fromBeginning();
    Flowable<AppendOnlyLogMessage<String>> afterMessageId(long lastSeenMessageId);

    void stop();
}
