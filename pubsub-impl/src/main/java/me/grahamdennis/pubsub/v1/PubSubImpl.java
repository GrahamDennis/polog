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

package me.grahamdennis.pubsub.v1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class PubSubImpl extends PubSubGrpc.PubSubImplBase {

    private final ConcurrentMap<Topic, List<Message>> messageStore;

    public PubSubImpl create() {
        return new PubSubImpl(Maps.newConcurrentMap());
    }

    public PubSubImpl(
            ConcurrentMap<Topic, List<Message>> messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public void createTopic(PubSubProto.Topic request, StreamObserver<PubSubProto.Topic> responseObserver) {
        // FIXME(gdennis): store topic labels.
        Topic topic = Topic.of(request.getName());

        messageStore.computeIfAbsent(topic, (theTopic) -> Lists.newArrayList());

        responseObserver.onNext(toProto(topic));
        responseObserver.onCompleted();
    }

    @Override
    public void updateTopic(PubSubProto.UpdateTopicRequest request,
            StreamObserver<PubSubProto.Topic> responseObserver) {
        // FIXME(gdennis): implement.
        super.updateTopic(request, responseObserver);
    }

    @Override
    public void getTopic(PubSubProto.GetTopicRequest request, StreamObserver<PubSubProto.Topic> responseObserver) {
        try {
            Topic topic = getTopic(request.getTopic());
            responseObserver.onNext(toProto(topic));
            responseObserver.onCompleted();
        } catch (StatusException e) {
            responseObserver.onError(e);
        }
    }

    private Topic getTopic(String name) throws StatusException {
        Topic topic = Topic.of(name);
        if (!messageStore.containsKey(topic)) {
            throw Status.NOT_FOUND.withDescription("Unknown Topic").asException();
        } else {
            return topic;
        }
    }

    @Override
    public void listTopics(PubSubProto.ListTopicsRequest request,
            StreamObserver<PubSubProto.ListTopicsResponse> responseObserver) {
        PubSubProto.ListTopicsResponse.Builder builder = PubSubProto.ListTopicsResponse.newBuilder();

        messageStore.keySet().forEach(topic -> builder.addTopics(toProto(topic)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMessage(PubSubProto.GetMessageRequest request,
            StreamObserver<PubSubProto.Message> responseObserver) {
        Topic topic = Topic.of(request.getTopic());
        List<Message> messages = messageStore.get(topic);
        if (messages == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Unknown Topic").asException());
            return;
        }

        int messageId = request.getMessageId();
        Message message;
        // FIXME(gdennis): Move logic to a TopicData class or similar.
        synchronized (messages) {
            message = messages.get(messageId);
        }
        if (message == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Unknown message").asException());
            return;
        }

        responseObserver.onNext(toProto(messageId, message));
        responseObserver.onCompleted();
    }

    @Override
    public void streamMessages(PubSubProto.StreamMessagesRequest request,
            StreamObserver<PubSubProto.Message> responseObserver) {
        Topic topic = Topic.of(request.getTopic());
        List<Message> messages = messageStore.get(topic);
        if (messages == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Unknown Topic").asException());
            return;
        }

        int lastSeenMessageId = request.getLastSeenMessageId();
        List<Message> unseenMessages;
        // FIXME(gdennis): Move logic to a TopicData class or similar.
        synchronized (messages) {
            if (lastSeenMessageId > messages.size()) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Unknown message").asException());
                return;
            }
            unseenMessages = ImmutableList.copyOf(messages.subList(lastSeenMessageId, messages.size()));
        }
        for (Message message : unseenMessages) {
            responseObserver.onNext(toProto(lastSeenMessageId++, message));
        }

        // FIXME(gdennis): listen for new messages.
        responseObserver.onCompleted();
    }

    private PubSubProto.Topic toProto(Topic topic) {
        return PubSubProto.Topic.newBuilder()
                .setName(topic.name())
                .build();
    }

    private PubSubProto.Message toProto(int messageId, Message message) {
        return PubSubProto.Message.newBuilder()
                .setMessageId(messageId)
                .setValue(message.value())
                .build();
    }
}
