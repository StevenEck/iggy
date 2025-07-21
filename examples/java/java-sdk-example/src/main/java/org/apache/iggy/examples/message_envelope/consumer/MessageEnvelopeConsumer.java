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

package org.apache.iggy.examples.message_envelope.consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.iggy.client.blocking.IggyClient;
import org.apache.iggy.message.Message;
import org.apache.iggy.message.PolledMessages;
import org.apache.iggy.message.PollingStrategy;
import org.apache.iggy.identifier.StreamId;
import org.apache.iggy.identifier.TopicId;
import org.apache.iggy.identifier.ConsumerId;
import org.apache.iggy.consumergroup.Consumer;
import org.apache.iggy.topic.CompressionAlgorithm;
import org.apache.iggy.examples.shared.Client;
import org.apache.iggy.examples.shared.ClientArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class MessageEnvelopeConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageEnvelopeConsumer.class);
    private static final String ORDER_CREATED_TYPE = "order_created";
    private static final String ORDER_CONFIRMED_TYPE = "order_confirmed";
    private static final String ORDER_REJECTED_TYPE = "order_rejected";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    //todo match rust output logging
    //todo clean up some methods on client args, just set to what producer uses instead of stream topic checks
    public static void main(String[] args) {
        // Simple argument parsing - similar to BasicConsumer example
        Args parsedArgs = Args.parseWithDefaults(args);

        try {
            LOGGER.info("Message envelope consumer has started, connecting to {}:{}", parsedArgs.host, parsedArgs.port);

            // Create client using the Client utility class
            IggyClient client = Client.buildTcpClientFromArgs(parsedArgs);

            // Authenticate and test (using default)
            client.getBaseClient().users().login("iggy", "iggy");
            client.getBaseClient().system().ping();
            LOGGER.info("Connected to Iggy server");

            // Use parsed IDs
            StreamId streamId = StreamId.of(parsedArgs.streamId);
            TopicId topicId = TopicId.of(parsedArgs.topicId);

            // Initialize system resources
            initializeResources(client, parsedArgs.stream, parsedArgs.topic, streamId, topicId);

            // Set up consumer group
            String consumerGroupName = "example-message-envelope-consumer";
            // Generate a random consumer ID (numeric)
            long randomConsumerId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
            Consumer consumer = Consumer.of(randomConsumerId);
            ConsumerId groupId = ConsumerId.of(consumerGroupName);

            // Make sure consumer group exists
            try {
                client.getBaseClient().consumerGroups().getConsumerGroup(streamId, topicId, groupId);
                LOGGER.info("Consumer group {} exists", consumerGroupName);
            } catch (Exception e) {
                LOGGER.info("Creating consumer group: {}", consumerGroupName);
                client.getBaseClient().consumerGroups().createConsumerGroup(streamId, topicId, Optional.empty(), consumerGroupName);
            }

            // Start consuming messages
            LOGGER.info("Starting to consume messages as consumer ID {}...", randomConsumerId);
            while (true) {
                // Poll for messages
                PolledMessages messages = client.getBaseClient().messages().pollMessages(
                        streamId,
                        topicId,
                        Optional.empty(), // No consumer group
                        consumer,
                        PollingStrategy.next(),
                        100L,
                        true
                );

                for (Message message : messages.messages()) {
                    handleMessage(message);
                }

                if (messages.messages().isEmpty()) {
                    Thread.sleep(100);  // Small delay between empty polls
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error in message envelope consumer", e);
            System.exit(1);
        }
    }

    private static void handleMessage(Message message) {
        try {
            String json = new String(message.payload(), StandardCharsets.UTF_8);
            Envelope envelope = OBJECT_MAPPER.readValue(json, Envelope.class);
            String payloadJson = envelope.getPayload();

            LOGGER.info("Handling message type: {} at offset: {}...",
                    envelope.getMessageType(), message.header().offset());

            switch (envelope.getMessageType()) {
                case ORDER_CREATED_TYPE:
                    OrderCreated orderCreated = OBJECT_MAPPER.readValue(payloadJson, OrderCreated.class);
                    LOGGER.info("{}", orderCreated);
                    break;

                case ORDER_CONFIRMED_TYPE:
                    OrderConfirmed orderConfirmed = OBJECT_MAPPER.readValue(payloadJson, OrderConfirmed.class);
                    LOGGER.info("{}", orderConfirmed);
                    break;

                case ORDER_REJECTED_TYPE:
                    OrderRejected orderRejected = OBJECT_MAPPER.readValue(payloadJson, OrderRejected.class);
                    LOGGER.info("{}", orderRejected);
                    break;

                default:
                    LOGGER.warn("Received unknown message type: {}", envelope.getMessageType());
            }
        } catch (IOException e) {
            LOGGER.error("Error processing message", e);
        }
    }

    private static void initializeResources(IggyClient client, String streamName, String topicName,
            StreamId streamId, TopicId topicId) {

        // Try to create stream (ignoring "already exists" errors)
        try {
            LOGGER.info("Creating stream with ID {} and name {}...", streamId.getId(), streamName);
            client.getBaseClient().streams().createStream(Optional.of(streamId.getId()), streamName);
            LOGGER.info("Stream created successfully");
            Thread.sleep(200); // Allow server to process
        } catch (Exception e) {
            LOGGER.info("Stream creation result: {}", e.getMessage());
        }

        // Try to create topic (ignoring "already exists" errors)
        try {
            LOGGER.info("Creating topic with ID {} and name {}...", topicId.getId(), topicName);
            client.getBaseClient().topics().createTopic(
                    streamId,
                    Optional.of(topicId.getId()),
                    3L, // partitionsCount
                    CompressionAlgorithm.None,
                    BigInteger.ZERO,  // message expiry (no expiry)
                    BigInteger.ZERO,  // max topic size (unlimited)
                    Optional.empty(), // replication factor
                    topicName
            );
            LOGGER.info("Topic created successfully");
            Thread.sleep(20); // Allow server to process
        } catch (Exception e) {
            LOGGER.info("Topic creation result: {}", e.getMessage());
        }

        // Final verification before continuing
        try {
            client.getBaseClient().streams().getStream(streamId);
            client.getBaseClient().topics().getTopic(streamId, topicId);
            LOGGER.info("System initialized with stream '{}' and topic '{}'", streamName, topicName);
        } catch (Exception e) {
            LOGGER.error("Failed to verify resources: {}", e.getMessage());
            throw e;
        }
    }

    // Simple Args class implementing ClientArgs interface
    static class Args implements ClientArgs {
        String host = "localhost";
        int port = 8090;
        String stream = "examples-envelope";
        String topic = "envelope";
        long streamId = 2L;
        long topicId = 1L;

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        public static Args parseWithDefaults(String[] args) {
            Args parsedArgs = new Args();

            // Simple argument parsing
            for (int i = 0; i < args.length; i++) {
                if (i + 1 >= args.length) break;

                switch (args[i]) {
                    case "--host":
                    case "-h":
                        parsedArgs.host = args[++i];
                        break;
                    case "--port":
                    case "-p":
                        parsedArgs.port = Integer.parseInt(args[++i]);
                        break;
                    case "--stream":
                    case "-s":
                        parsedArgs.stream = args[++i];
                        break;
                    case "--topic":
                    case "-t":
                        parsedArgs.topic = args[++i];
                        break;
                    case "--stream-id":
                    case "-si":
                        parsedArgs.streamId = Long.parseLong(args[++i]);
                        break;
                    case "--topic-id":
                    case "-ti":
                        parsedArgs.topicId = Long.parseLong(args[++i]);
                        break;
                }
            }

            return parsedArgs;
        }
    }

    // Message envelope classes
    static class Envelope {
        private String messageType;
        private String payload;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

    static class OrderCreated implements SerializableMessage {
        private String orderId;
        private String userId;
        private Map<String, Integer> items = new HashMap<>();
        private double totalAmount;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Map<String, Integer> getItems() {
            return items;
        }

        public void setItems(Map<String, Integer> items) {
            this.items = items;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        @Override
        public String getMessageType() {
            return ORDER_CREATED_TYPE;
        }

    }

    static class OrderConfirmed implements SerializableMessage {
        private String orderId;
        private String confirmationCode;
        private long confirmedAt;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getConfirmationCode() {
            return confirmationCode;
        }

        public void setConfirmationCode(String confirmationCode) {
            this.confirmationCode = confirmationCode;
        }

        public long getConfirmedAt() {
            return confirmedAt;
        }

        public void setConfirmedAt(long confirmedAt) {
            this.confirmedAt = confirmedAt;
        }

        @Override
        public String getMessageType() {
            return ORDER_CONFIRMED_TYPE;
        }

    }

    static class OrderRejected implements SerializableMessage {
        private String orderId;
        private String reason;
        private long rejectedAt;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public long getRejectedAt() {
            return rejectedAt;
        }

        public void setRejectedAt(long rejectedAt) {
            this.rejectedAt = rejectedAt;
        }

        @Override
        public String getMessageType() {
            return ORDER_REJECTED_TYPE;
        }

    }

    interface SerializableMessage {
        @JsonIgnore // don't use message type in our json serialization
        String getMessageType();
    }
}
