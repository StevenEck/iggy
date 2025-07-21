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

package org.apache.iggy.examples.message_envelope.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.iggy.client.blocking.IggyClient;
import org.apache.iggy.examples.shared.Client;
import org.apache.iggy.examples.shared.ClientArgs;
import org.apache.iggy.identifier.StreamId;
import org.apache.iggy.identifier.TopicId;
import org.apache.iggy.message.Message;
import org.apache.iggy.message.Partitioning;
import org.apache.iggy.topic.CompressionAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

//todo match rust output logging
public class MessageEnvelopeProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageEnvelopeProducer.class);
    private static final String ORDER_CREATED_TYPE = "order_created";
    private static final String ORDER_CONFIRMED_TYPE = "order_confirmed";
    private static final String ORDER_REJECTED_TYPE = "order_rejected";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AtomicInteger ORDER_ID_COUNTER = new AtomicInteger(1);
    private static final String[] PRODUCT_IDS = {"prod-1", "prod-2", "prod-3", "prod-4", "prod-5"};
    private static final String[] USER_IDS = {"user-1", "user-2", "user-3", "user-4", "user-5"};

    public static void main(String[] args) {
        // Simple argument parsing
        Args parsedArgs = Args.parseWithDefaults(args);

        try {
            LOGGER.info("Message envelope producer has started, connecting to {}:{}", parsedArgs.host, parsedArgs.port);

            // Create client using the Client utility class
            IggyClient client = Client.buildTcpClientFromArgs(parsedArgs);

            // Authenticate and test
            client.getBaseClient().users().login("iggy", "iggy");
            client.getBaseClient().system().ping();
            LOGGER.info("Connected to Iggy server");

            // Use parsed IDs
            StreamId streamId = StreamId.of(parsedArgs.streamId);
            TopicId topicId = TopicId.of(parsedArgs.topicId);

            // Initialize system resources
            initializeResources(client, parsedArgs.stream, parsedArgs.topic, streamId, topicId, parsedArgs);

            // Produce messages
            produceMessages(client, streamId, topicId, parsedArgs);

        } catch (Exception e) {
            LOGGER.error("Error in message envelope producer", e);
            System.exit(1);
        }
    }

    private static void produceMessages(IggyClient client, StreamId streamId, TopicId topicId, Args args) throws Exception {
        LOGGER.info("Messages will be sent to stream: {}, topic: {}, partition: {} with {} batches of {} messages.",
                streamId.getId(), topicId.getId(), args.partitionId, args.messageBatchesLimit, args.messagesPerBatch);

        int sentBatches = 0;
        MessageGenerator messageGenerator = new MessageGenerator();
        Partitioning partitioning = Partitioning.partitionId(args.partitionId);

        while (true) {
            if (args.messageBatchesLimit > 0 && sentBatches == args.messageBatchesLimit) {
                LOGGER.info("Sent {} batches of messages, exiting.", sentBatches);
                return;
            }

            List<Message> messages = new ArrayList<>();
            List<SerializableMessage> serializableMessages = new ArrayList<>();

            for (int i = 0; i < args.messagesPerBatch; i++) {
                SerializableMessage serializableMessage = messageGenerator.generate();
                String jsonEnvelope = serializableMessage.toJsonEnvelope();
                Message message = Message.of(jsonEnvelope);
                messages.add(message);
                serializableMessages.add(serializableMessage);
            }

            LOGGER.info("Sending messages count: {}", messages.size());

            // Send all messages at once, like in the Rust code
            client.getBaseClient().messages().sendMessages(
                    streamId,
                    topicId,
                    partitioning,
                    messages
            );

            sentBatches++;
            LOGGER.info("Sent messages: {}", serializableMessages);

            if (args.interval > 0) {
                Thread.sleep(args.interval);
            }
        }
    }

    private static void initializeResources(IggyClient client, String streamName, String topicName,
            StreamId streamId, TopicId topicId, Args args) {

        // 1. Check if stream exists, create if needed
        boolean streamExists = false;
        try {
            var stream = client.getBaseClient().streams().getStream(streamId);
            if (stream.isPresent()) {
                streamExists = true;
                LOGGER.info("Stream {} exists", streamName);
            }
        } catch (Exception e) {
            LOGGER.info("Stream doesn't exist: {}", e.getMessage());
        }

        if (!streamExists) {
            try {
                LOGGER.info("Creating stream with ID {} and name {}...", streamId.getId(), streamName);
                client.getBaseClient().streams().createStream(Optional.of(streamId.getId()), streamName);
                LOGGER.info("Stream created successfully");
                Thread.sleep(200);
            } catch (Exception e) {
                LOGGER.info("Stream creation result: {}", e.getMessage());
            }
        }

        // 2. Check if topic exists, create if needed
        boolean topicExists = false;
        try {
            var topic = client.getBaseClient().topics().getTopic(streamId, topicId);
            if (topic.isPresent()) {
                topicExists = true;
                LOGGER.info("Topic {} exists with {} partitions",
                        topicName, topic.get().partitionsCount());
            }
        } catch (Exception e) {
            LOGGER.info("Topic doesn't exist: {}", e.getMessage());
        }

        if (!topicExists) {
            try {
                LOGGER.info("Creating topic with ID {} and name {}...", topicId.getId(), topicName);
                client.getBaseClient().topics().createTopic(
                        streamId,
                        Optional.of(topicId.getId()),
                        3L, // partitionsCount
                        CompressionAlgorithm.None,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        Optional.empty(),
                        topicName
                );
                LOGGER.info("Topic created successfully");
                Thread.sleep(200);
            } catch (Exception e) {
                LOGGER.info("Topic creation result: {}", e.getMessage());
            }
        }

        // 3. Check partitions and ensure we use the correct partition ID
        try {
            var topic = client.getBaseClient().topics().getTopic(streamId, topicId);
            if (topic.isPresent()) {
                int partitionsCount = topic.get().partitionsCount().intValue();
                LOGGER.info("Topic has {} partitions", partitionsCount);

                if (partitionsCount <= 0) {
                    throw new RuntimeException("Topic has no partitions");
                }

                // Always use partition ID 1 instead of 0
                args.partitionId = 1;
                LOGGER.info("Setting partition ID to 1 as partition indexing in Iggy starts from 1");

                LOGGER.info("System initialized with stream '{}', topic '{}', using partition ID {}",
                        streamName, topicName, args.partitionId);
            } else {
                throw new RuntimeException("Topic not found after creation attempts");
            }
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
        long partitionId = 0L;
        int messagesPerBatch = 5;
        int messageBatchesLimit = 2;
        long interval = 1000; // milliseconds

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
                    case "--partition-id":
                    case "-pi":
                        parsedArgs.partitionId = Integer.parseInt(args[++i]);
                        break;
                    case "--messages-per-batch":
                    case "-mpb":
                        parsedArgs.messagesPerBatch = Integer.parseInt(args[++i]);
                        break;
                    case "--message-batches-limit":
                    case "-mbl":
                        parsedArgs.messageBatchesLimit = Integer.parseInt(args[++i]);
                        break;
                    case "--interval":
                    case "-i":
                        parsedArgs.interval = Long.parseLong(args[++i]);
                        break;
                }
            }

            return parsedArgs;
        }
    }

    // Message generator class to create different types of messages
    static class MessageGenerator {
        private final Random random = new Random();

        public SerializableMessage generate() {
            int type = random.nextInt(3);
            switch (type) {
                case 0:
                    return createOrderCreated();
                case 1:
                    return createOrderConfirmed();
                case 2:
                default:
                    return createOrderRejected();
            }
        }

        private OrderCreated createOrderCreated() {
            OrderCreated order = new OrderCreated();
            order.setOrderId("order-" + ORDER_ID_COUNTER.getAndIncrement());
            order.setUserId(USER_IDS[random.nextInt(USER_IDS.length)]);

            int itemCount = random.nextInt(3) + 1;
            Map<String, Integer> items = new HashMap<>();
            for (int i = 0; i < itemCount; i++) {
                String productId = PRODUCT_IDS[random.nextInt(PRODUCT_IDS.length)];
                int quantity = random.nextInt(5) + 1;
                items.put(productId, quantity);
            }
            order.setItems(items);

            double totalAmount = random.nextDouble() * 500.0;
            order.setTotalAmount(Math.round(totalAmount * 100.0) / 100.0);

            return order;
        }

        private OrderConfirmed createOrderConfirmed() {
            OrderConfirmed confirmation = new OrderConfirmed();
            confirmation.setOrderId("order-" + (ORDER_ID_COUNTER.get() - random.nextInt(Math.min(5, ORDER_ID_COUNTER.get()))));
            confirmation.setConfirmationCode(UUID.randomUUID().toString().substring(0, 8));
            confirmation.setConfirmedAt(Instant.now().getEpochSecond());
            return confirmation;
        }

        private OrderRejected createOrderRejected() {
            OrderRejected rejection = new OrderRejected();
            rejection.setOrderId("order-" + (ORDER_ID_COUNTER.get() - random.nextInt(Math.min(5, ORDER_ID_COUNTER.get()))));

            String[] reasons = {
                    "Payment failed",
                    "Items out of stock",
                    "Invalid shipping address",
                    "Order canceled by user"
            };
            rejection.setReason(reasons[random.nextInt(reasons.length)]);
            rejection.setRejectedAt(Instant.now().getEpochSecond());
            return rejection;
        }
    }

    // Interface for all serializable messages
    interface SerializableMessage {
        String getMessageType();
        String toJsonEnvelope() throws Exception;
    }

    // Message envelope classes
    static class Envelope<T> {
        private String messageType;
        private T payload;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public T getPayload() {
            return payload;
        }

        public void setPayload(T payload) {
            this.payload = payload;
        }
    }

    static class OrderCreated implements SerializableMessage {
        private String orderId;
        private String userId;
        private Map<String, Integer> items = new HashMap<>();
        private double totalAmount;

        @Override
        public String getMessageType() {
            return ORDER_CREATED_TYPE;
        }

        @Override
        public String toJsonEnvelope() throws Exception {
            Envelope envelope = new Envelope();  // Use non-generic Envelope
            envelope.setMessageType(getMessageType());
            envelope.setPayload(OBJECT_MAPPER.writeValueAsString(this));  // Serialize to string
            return OBJECT_MAPPER.writeValueAsString(envelope);
        }

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
        public String toString() {
            return "OrderCreated{" +
                    "orderId='" + orderId + '\'' +
                    ", userId='" + userId + '\'' +
                    ", items=" + items +
                    ", totalAmount=" + totalAmount +
                    '}';
        }
    }

    static class OrderConfirmed implements SerializableMessage {
        private String orderId;
        private String confirmationCode;
        private long confirmedAt;

        @Override
        public String getMessageType() {
            return ORDER_CONFIRMED_TYPE;
        }

        @Override
        public String toJsonEnvelope() throws Exception {
            Envelope envelope = new Envelope();
            envelope.setMessageType(getMessageType());
            envelope.setPayload(OBJECT_MAPPER.writeValueAsString(this));
            return OBJECT_MAPPER.writeValueAsString(envelope);
        }

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
        public String toString() {
            return "OrderConfirmed{" +
                    "orderId='" + orderId + '\'' +
                    ", confirmationCode='" + confirmationCode + '\'' +
                    ", confirmedAt=" + confirmedAt +
                    '}';
        }
    }

    static class OrderRejected implements SerializableMessage {
        private String orderId;
        private String reason;
        private long rejectedAt;

        @Override
        public String getMessageType() {
            return ORDER_REJECTED_TYPE;
        }

        @Override
        public String toJsonEnvelope() throws Exception {
            Envelope envelope = new Envelope();
            envelope.setMessageType(getMessageType());
            envelope.setPayload(OBJECT_MAPPER.writeValueAsString(this));
            return OBJECT_MAPPER.writeValueAsString(envelope);
        }

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
        public String toString() {
            return "OrderRejected{" +
                    "orderId='" + orderId + '\'' +
                    ", reason='" + reason + '\'' +
                    ", rejectedAt=" + rejectedAt +
                    '}';
        }
    }
}
