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

package org.apache.iggy.examples.basic.producer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.iggy.client.blocking.IggyBaseClient;
import org.apache.iggy.client.blocking.IggyClient;
import org.apache.iggy.client.blocking.IggyClientBuilder;
import org.apache.iggy.client.blocking.tcp.IggyTcpClient;
import org.apache.iggy.identifier.StreamId;
import org.apache.iggy.identifier.TopicId;
import org.apache.iggy.message.Message;
import org.apache.iggy.message.Partitioning;
import org.apache.iggy.topic.CompressionAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasicProducer example for the Iggy Java SDK.
 *
 * PREREQUISITES:
 * Before running this example, you need to start the Iggy server:
 *
 *    cargo run --bin iggy-server
 *
 * This will start a local Iggy server instance on the default port (8090)
 * which this example connects to.
 */

public class BasicProducer {
    private static final Logger logger = LoggerFactory.getLogger(BasicProducer.class);

    static class Args {
        private String host = "localhost";
        private int port = 8090;
        private long streamId = 1L;
        private long topicId = 1L;
        private long partitionId = 0L;
        private int messagesPerBatch = 10;
        private int messageBatchesLimit = 0; // 0 = unlimited

        public static Args parseWithDefaults(String[] args) {
            Args parsedArgs = new Args();
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
                    case "--partition-id":
                        parsedArgs.partitionId = Long.parseLong(args[++i]);
                        break;
                    case "--messages-per-batch":
                        parsedArgs.messagesPerBatch = Integer.parseInt(args[++i]);
                        break;
                    case "--message-batches-limit":
                        parsedArgs.messageBatchesLimit = Integer.parseInt(args[++i]);
                        break;
                }
            }
            return parsedArgs;
        }
    }

    public static void main(String[] args) throws Exception {
        Args parsedArgs = Args.parseWithDefaults(args);
        logger.info("Basic producer has started, connecting to {}:{}", parsedArgs.host, parsedArgs.port);

        IggyBaseClient baseClient = new IggyTcpClient(parsedArgs.host, parsedArgs.port);
        IggyClient client = new IggyClientBuilder().withBaseClient(baseClient).build();
        client.getBaseClient().users().login("iggy", "iggy");
        client.getBaseClient().system().ping();
        logger.info("Successfully connected to Iggy server");

        StreamId streamId = StreamId.of(parsedArgs.streamId);
        TopicId topicId = TopicId.of(parsedArgs.topicId);

        initByProducer(client, streamId, topicId);

        logger.info("Producing to stream ID: {}, topic ID: {}, partition ID: {}",
                parsedArgs.streamId, parsedArgs.topicId, parsedArgs.partitionId);

        produceMessages(parsedArgs, client, streamId, topicId);
    }

    private static void initByProducer(IggyClient client, StreamId streamId, TopicId topicId) {
        try {
            // Always try to create stream regardless of whether we think it exists
            try {
                logger.info("Ensuring stream with ID {} exists...", streamId.getId());
                client.getBaseClient().streams().createStream(Optional.of(streamId.getId()), "example-stream");
                logger.info("Stream created successfully");
            } catch (Exception e) {
                // Likely already exists
                logger.info("Create stream result (may be ok if already exists): {}", e.getMessage());
            }

            // Always try to create topic regardless of whether we think it exists
            try {
                logger.info("Ensuring topic with ID {} exists in stream {}...", topicId.getId(), streamId.getId());
                client.getBaseClient().topics().createTopic(
                        streamId,
                        Optional.of(topicId.getId()),
                        1L, // partitionsCount
                        CompressionAlgorithm.None,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        Optional.empty(),
                        "example-topic"
                );
                logger.info("Topic created successfully");
            } catch (Exception e) {
                // Likely already exists
                logger.info("Create topic result (may be ok if already exists): {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize system resources", e);
            throw e;
        }
    }

    private static void produceMessages(Args args, IggyClient client, StreamId streamId, TopicId topicId) throws Exception {
        long currentId = 0;
        int sentBatches = 0;
        // Use partitionId = 1 (not 0)
        long partitionId = 1L;
        while (args.messageBatchesLimit == 0 || sentBatches < args.messageBatchesLimit) {
            List<Message> messages = new ArrayList<>();
            List<String> sentPayloads = new ArrayList<>();
            for (int i = 0; i < args.messagesPerBatch; i++) {
                currentId++;
                String payload = "message-" + currentId;
                messages.add(Message.of(payload));
                sentPayloads.add(payload);
            }
            client.getBaseClient().messages().sendMessages(
                    streamId.getId(),
                    topicId.getId(),
                    Partitioning.partitionId(partitionId), // <-- use 1 here
                    messages
            );
            sentBatches++;
            logger.info("Sent messages: {}", sentPayloads);
            Thread.sleep(1000); // throttle sending
        }
        logger.info("Sent {} batches of messages, exiting.", sentBatches);
    }
}
