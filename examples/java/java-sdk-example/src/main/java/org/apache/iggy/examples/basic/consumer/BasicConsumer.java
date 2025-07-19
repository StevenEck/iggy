/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iggy.examples.basic.consumer;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.iggy.client.blocking.IggyClient;
import org.apache.iggy.client.blocking.IggyBaseClient;
import org.apache.iggy.client.blocking.IggyClientBuilder;
import org.apache.iggy.client.blocking.tcp.IggyTcpClient;
import org.apache.iggy.message.Message;
import org.apache.iggy.message.PolledMessages;
import org.apache.iggy.message.PollingStrategy;
import org.apache.iggy.identifier.StreamId;
import org.apache.iggy.identifier.TopicId;
import org.apache.iggy.consumergroup.Consumer;
import org.apache.iggy.topic.CompressionAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasicConsumer example for the Iggy Java SDK.
 *
 * PREREQUISITES:
 * Before running this example, you need to start the Iggy server:
 *
 *    cargo run --bin iggy-server
 *
 * This will start a local Iggy server instance on the default port (8090)
 * which this example connects to.
 */

public class BasicConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BasicConsumer.class);

    // Simple Args class to mirror the Rust version's configuration
    static class Args {
        private String host = "localhost";
        private int port = 8090;
        private long streamId = 1L;
        private long topicId = 1L;
        private long consumerId = 1L;

        public static Args parseWithDefaults(String[] args) {
            Args parsedArgs = new Args();

            // Simple argument parsing
            for (int i = 0; i < args.length; i++) {
                if (i + 1 >= args.length) break;

                switch (args[i]) {
                    case "--host":
                        parsedArgs.host = args[++i];
                        break;
                    case "--port":
                        parsedArgs.port = Integer.parseInt(args[++i]);
                        break;
                    case "--stream-id":
                        parsedArgs.streamId = Long.parseLong(args[++i]);
                        break;
                    case "--topic-id":
                        parsedArgs.topicId = Long.parseLong(args[++i]);
                        break;
                    case "--consumer-id":
                        parsedArgs.consumerId = Long.parseLong(args[++i]);
                        break;
                }
            }

            return parsedArgs;
        }
    }

    public static void main(String[] args) throws Exception {
        // Parse arguments - similar to Rust's Args::parse_with_defaults
        Args parsedArgs = Args.parseWithDefaults(args);
        logger.info("Basic consumer has started, connecting to {}:{}", parsedArgs.host, parsedArgs.port);

        // Create client using the builder pattern
        IggyBaseClient baseClient = createBaseClient(parsedArgs.host, parsedArgs.port);
        IggyClient client = new IggyClientBuilder()
                .withBaseClient(baseClient)
                .build();
        // Authenticate with credentials but here we will use the default
        client.getBaseClient().users().login("iggy", "iggy");

        // Test connection
        client.getBaseClient().system().ping();
        logger.info("Successfully connected to Iggy server");

        // Use parsed IDs
        StreamId streamId = StreamId.of(parsedArgs.streamId);
        TopicId topicId = TopicId.of(parsedArgs.topicId);
        Consumer consumer = Consumer.of(parsedArgs.consumerId);

        // initialize system resources
        initByConsumer(client, streamId, topicId, consumer);

        logger.info("Consuming from stream ID: {}, topic ID: {}, as consumer ID: {}",
                parsedArgs.streamId, parsedArgs.topicId, parsedArgs.consumerId);

        // Start consuming messages with adaptive polling
        consumeMessagesWithAdaptivePolling(client, streamId, topicId, consumer);
    }

    private static void handleMessage(Message message) {
        String payload = new String(message.payload(), StandardCharsets.UTF_8);
        logger.info("Handling message at offset: {}, payload: {}...",
                message.header().offset(), payload);
    }

    private static IggyBaseClient createBaseClient(String host, int port) {
        return new IggyTcpClient(host, port);
    }

    private static void initByConsumer(IggyClient client, StreamId streamId, TopicId topicId, Consumer consumer) throws Exception {
        try {
            // Always try to create stream regardless of whether we think it exists
            try {
                logger.info("Ensuring stream with ID {} exists...", streamId.getId());
                client.getBaseClient().streams().createStream(Optional.of(streamId.getId()), "example-stream");
                logger.info("Stream created or already exists");
            } catch (Exception e) {
                // Likely already exists, verify it
                logger.info("Create stream result (may be ok if already exists): {}", e.getMessage());
            }

            // Always try to create topic regardless of whether we think it exists
            try {
                logger.info("Ensuring topic with ID {} exists in stream {}...", topicId.getId(), streamId.getId());
                client.getBaseClient().topics().createTopic(
                        streamId,
                        Optional.of(topicId.getId()),
                        1L, // partitionsCount
                        CompressionAlgorithm.None, // Use appropriate compression algorithm
                        BigInteger.ZERO, // messageExpiry - no expiry
                        BigInteger.ZERO, // maxTopicSize - unlimited
                        Optional.empty(), // replicationFactor
                        "example-topic" // name
                );
                logger.info("Topic created or already exists");
            } catch (Exception e) {
                // Likely already exists, verify it
                logger.info("Create topic result (may be ok if already exists): {}", e.getMessage());
            }

            // For consumer ID, we're using client-assigned ID so we don't need to create it
            logger.info("Using consumer with ID {}", consumer.id());
        } catch (Exception e) {
            logger.error("Failed to initialize system resources", e);
            throw e;
        }
    }

    private static void consumeMessagesWithAdaptivePolling(
            IggyClient client,
            StreamId streamId,
            TopicId topicId,
            Consumer consumer) throws InterruptedException {

        int consecutiveEmptyPolls = 0;
        while (true) {
            try {
                PolledMessages messages = client.getBaseClient().messages().pollMessages(
                        streamId,
                        topicId,
                        Optional.empty(),
                        consumer,
                        PollingStrategy.next(),
                        100L,
                        true
                );

                int messageCount = messages.messages().size();

                // Process messages
                for (Message message : messages.messages()) {
                    handleMessage(message);
                }

                // Implement adaptive polling frequency
                if (messageCount == 0) {
                    // No messages found, gradually back off
                    consecutiveEmptyPolls++;
                    // Exponential backoff with a cap
                    int sleepTime = Math.min(100 * consecutiveEmptyPolls, 1000);
                    Thread.sleep(sleepTime);
                } else {
                    // Messages found, reset backoff and continue quickly
                    consecutiveEmptyPolls = 0;
                    // Small pause to prevent overwhelming the network
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                logger.error("Error polling messages: {}", e.getMessage());
                Thread.sleep(1000); // Back off on errors
            }
        }
    }

}
