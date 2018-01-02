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

package io.grpc.examples.broker;

import com.google.common.collect.Maps;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.ReplaySubject;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrokerImpl extends BrokerGrpc.BrokerImplBase {
    private static final Logger log = LoggerFactory.getLogger(BrokerImpl.class);

    private final ConcurrentMap<String, ReplaySubject<Record>> recordReplaySubjectByTopic;

    public static BrokerImpl create() {
        return new BrokerImpl(Maps.newConcurrentMap());
    }

    public BrokerImpl(
            ConcurrentMap<String, ReplaySubject<Record>> recordReplaySubjectByTopic) {
        this.recordReplaySubjectByTopic = recordReplaySubjectByTopic;
    }

    @Override
    public void addRecords(AddRecordsRequest request, StreamObserver<AddRecordsResponse> responseObserver) {
        String topic = request.getTopic();
        ReplaySubject<Record> recordReplaySubject = getRecordReplaySubject(topic);

        request.getRecordsList().forEach(recordReplaySubject::onNext);

        AddRecordsResponse addRecordsResponse = AddRecordsResponse.newBuilder()
                .setTopic(topic)
                .build();

        responseObserver.onNext(addRecordsResponse);
        responseObserver.onCompleted();
    }

    private ReplaySubject<Record> getRecordReplaySubject(String topic) {
        return recordReplaySubjectByTopic.computeIfAbsent(topic, this::createReplaySubject);
    }

    private ReplaySubject<Record> createReplaySubject(String topic) {
        ReplaySubject<Record> recordReplaySubject = ReplaySubject.create();

        Disposable subscribe = recordReplaySubject.subscribe(
                record -> log.info("Received on topic {}: {}", topic, record));

        return recordReplaySubject;
    }

    @Override
    public void listRecords(ListRecordsRequest request, StreamObserver<Record> responseObserver) {
        getRecordReplaySubject(request.getTopic())
                .subscribe(new Observer<Record>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        ServerCallStreamObserver<Record> serverCallStreamObserver =
                                (ServerCallStreamObserver<Record>) responseObserver;

                        serverCallStreamObserver.setOnCancelHandler(disposable::dispose);
                    }

                    @Override
                    public void onNext(Record record) {
                        responseObserver.onNext(record);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        responseObserver.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        responseObserver.onCompleted();
                    }
                });
    }

    public void stop() {
        recordReplaySubjectByTopic.forEach((topic, recordReplaySubject) -> recordReplaySubject.onComplete());
    }
}
