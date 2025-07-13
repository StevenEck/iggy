package org.apache.iggy.examples.basic.consumer;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        logger.info("Consuming from stream ID: {}, topic ID: {}, as consumer ID: {}",
                parsedArgs.streamId, parsedArgs.topicId, parsedArgs.consumerId);

        // Poll for messages continuously
        while (true) {
            PolledMessages messages = client.getBaseClient().messages().pollMessages(
                    streamId,
                    topicId,
                    Optional.empty(), // No specific partition
                    consumer,
                    PollingStrategy.next(), // Get next messages
                    100L,  // Fetch up to 100 messages
                    true   // Auto-commit offsets
            );

            // Process each message
            for (Message message : messages.messages()) {
                handleMessage(message);
            }

            // Don't hammer the server
            Thread.sleep(1000);
        }
    }

    private static void handleMessage(Message message) {
        String payload = new String(message.payload(), StandardCharsets.UTF_8);
        logger.info("Handling message at offset: {}, payload: {}...",
                message.header().offset(), payload);
    }

    private static IggyBaseClient createBaseClient(String host, int port) {
        return new IggyTcpClient(host, port);
    }

}
