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

import com.google.common.collect.Maps;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import me.grahamdennis.pubsub.appendonlylog.InMemoryAppendOnlyLog;
import me.grahamdennis.pubsub.v1.TopicName;

public final class PubSub {

    private final ConcurrentMap<TopicName, LogStream> topics;

    public static PubSub create() {
        return new PubSub(Maps.newConcurrentMap());
    }

    public PubSub(
            ConcurrentMap<TopicName, LogStream> topics) {
        this.topics = topics;
    }

    public void createTopic(TopicName topicName) {
        // topics.computeIfAbsent(topicName, (theTopicName) -> SimpleLogStream.create());
        topics.computeIfAbsent(topicName, (theTopicName) -> new AppendOnlyLogStream(InMemoryAppendOnlyLog.create()));
    }

    public Optional<LogStream> getTopic(TopicName topicName) {
        return Optional.ofNullable(topics.get(topicName));
    }

    public Iterable<TopicName> listTopics() {
        return topics.keySet();
    }

    public void stop() {
        topics.values().forEach(LogStream::stop);
    }
}
