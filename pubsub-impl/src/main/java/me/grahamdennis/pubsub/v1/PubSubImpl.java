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

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import me.grahamdennis.pubsub.core.LogStream;
import me.grahamdennis.pubsub.core.Message;
import me.grahamdennis.pubsub.core.PubSub;
import org.reactivestreams.Subscription;

public final class PubSubImpl extends PubSubGrpc.PubSubImplBase {

    private final PubSub pubSub;

    public PubSubImpl(PubSub pubSub) {
        this.pubSub = pubSub;
    }

    @Override
    public void createTopic(PubSubProto.Topic request, StreamObserver<PubSubProto.Topic> responseObserver) {
        // FIXME(gdennis): store topicName labels.
        TopicName topicName = TopicName.of(request.getName());

        pubSub.createTopic(topicName);

        success(responseObserver, toProto(topicName));
    }

    @Override
    public void updateTopic(PubSubProto.UpdateTopicRequest request,
            StreamObserver<PubSubProto.Topic> responseObserver) {
        // FIXME(gdennis): implement.
        super.updateTopic(request, responseObserver);
    }

    @Override
    public void getTopic(PubSubProto.GetTopicRequest request, StreamObserver<PubSubProto.Topic> responseObserver) {
        TopicName topicName = TopicName.of(request.getTopic());

        Optional<LogStream> maybeLogStream = pubSub.getTopic(topicName);
        if (maybeLogStream.isPresent()) {
            success(responseObserver, toProto(topicName));
        } else {
            responseObserver.onError(Errors.unknownTopic().asException());
        }
    }

    @Override
    public void listTopics(PubSubProto.ListTopicsRequest request,
            StreamObserver<PubSubProto.ListTopicsResponse> responseObserver) {
        PubSubProto.ListTopicsResponse.Builder builder = PubSubProto.ListTopicsResponse.newBuilder();

        pubSub.listTopics()
                .forEach(topicName -> builder.addTopics(toProto(topicName)));

        success(responseObserver, builder.build());
    }

    @Override
    public void getMessage(PubSubProto.GetMessageRequest request,
            StreamObserver<PubSubProto.Message> responseObserver) {
        TopicName topicName = TopicName.of(request.getTopic());

        Optional<LogStream> maybeLogStream = pubSub.getTopic(topicName);
        if (!maybeLogStream.isPresent()) {
            responseObserver.onError(Errors.unknownTopic().asException());
            return;
        }

        if (request.getMessageId() < 1) {
            responseObserver.onError(Errors.illegalMessageId().asException());
            return;
        }

        maybeLogStream.get()
                .afterMessageId(request.getMessageId() - 1)
                .firstOrError()
                .map(this::toProto)
                .subscribe(toSingleObserver(responseObserver));
    }

    @Override
    public void streamMessages(PubSubProto.StreamMessagesRequest request,
            StreamObserver<PubSubProto.Message> responseObserver) {
        TopicName topicName = TopicName.of(request.getTopic());

        Optional<LogStream> maybeLogStream = pubSub.getTopic(topicName);
        if (!maybeLogStream.isPresent()) {
            responseObserver.onError(Errors.unknownTopic().asException());
            return;
        }

        maybeLogStream.get()
                .afterMessageId(request.getLastSeenMessageId() - 1)
                .map(this::toProto)
                .subscribe(toSubscriber(responseObserver));
    }

    @Override
    public void publish(PubSubProto.PublishRequest request,
            StreamObserver<PubSubProto.PublishResponse> responseObserver) {
        TopicName topicName = TopicName.of(request.getTopic());

        Optional<LogStream> maybeLogStream = pubSub.getTopic(topicName);
        if (!maybeLogStream.isPresent()) {
            responseObserver.onError(Errors.unknownTopic().asException());
            return;
        }

        List<String> messages = request.getMessagesList()
                .stream()
                .map(PubSubProto.Message::getValue)
                .collect(Collectors.toList());

        Flowable.fromIterable(messages)
                .lift(maybeLogStream.get().storeToLogStreamOperator())
                .reduce(PubSubProto.PublishResponse.newBuilder(), PubSubProto.PublishResponse.Builder::addMessageIds)
                .map(PubSubProto.PublishResponse.Builder::build)
                .subscribe(toSingleObserver(responseObserver));
    }

    public void stop() {
        pubSub.stop();
    }

    private PubSubProto.Topic toProto(TopicName topicName) {
        return PubSubProto.Topic.newBuilder()
                .setName(topicName.name())
                .build();
    }

    private PubSubProto.Message toProto(Message message) {
        return PubSubProto.Message.newBuilder()
                .setMessageId(message.id()+1)
                .setValue(message.value())
                .build();
    }

    private <T> void success(StreamObserver<T> responseObserver, T value) {
        responseObserver.onNext(value);
        responseObserver.onCompleted();
    }

    private static <T> FlowableSubscriber<T> toSubscriber(
            StreamObserver<T> responseObserver) {
        return new FlowableSubscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                // FIXME(gdennis): do this properly
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                responseObserver.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onComplete() {
                responseObserver.onCompleted();
            }
        };
    }

    private static <T> SingleObserver<T> toSingleObserver(StreamObserver<T> responseObserver) {
        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onSuccess(T t) {
                responseObserver.onNext(t);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                responseObserver.onError(e);
            }
        };
    }

    private static class Errors {

        private static Status unknownTopic() {
            return Status.NOT_FOUND.withDescription("Unknown topic");
        }

        private static Status unknownMessage() {
            return Status.NOT_FOUND.withDescription("Unknown message id");
        }

        public static Status illegalMessageId() {
            return Status.INVALID_ARGUMENT.withDescription("Illegal message id");
        }
    }
}
