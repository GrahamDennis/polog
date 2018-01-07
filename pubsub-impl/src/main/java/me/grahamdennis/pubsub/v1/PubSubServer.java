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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import me.grahamdennis.pubsub.core.PubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PubSubServer {
    private static final Logger log = LoggerFactory.getLogger(PubSubServer.class);

    private final PubSubImpl pubSubImpl;
    private final Server server;

    public static PubSubServer create() {
        PubSub pubSub = PubSub.create();
        PubSubImpl pubSubImpl = new PubSubImpl(pubSub);

        int port = 50053;
        Server server = ServerBuilder.forPort(port)
                .addService(pubSubImpl)
                .build();
        return new PubSubServer(pubSubImpl, server);
    }

    public PubSubServer(PubSubImpl pubSubImpl, Server server) {
        this.pubSubImpl = pubSubImpl;
        this.server = server;
    }

    private void start() throws IOException {
        server.start();

        log.info("Server started, listening on {}", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the log may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            PubSubServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    private void stop() {
        pubSubImpl.stop();
        server.shutdown();
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final PubSubServer server = PubSubServer.create();
        server.start();
        server.blockUntilShutdown();
    }
}
