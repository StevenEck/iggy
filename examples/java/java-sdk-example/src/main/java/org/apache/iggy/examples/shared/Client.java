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

package org.apache.iggy.examples.shared;

import org.apache.iggy.client.blocking.ConsumerGroupsClient;
import org.apache.iggy.client.blocking.ConsumerOffsetsClient;
import org.apache.iggy.client.blocking.IggyClient;
import org.apache.iggy.client.blocking.IggyBaseClient;
import org.apache.iggy.client.blocking.IggyClientBuilder;
import org.apache.iggy.client.blocking.MessagesClient;
import org.apache.iggy.client.blocking.PartitionsClient;
import org.apache.iggy.client.blocking.PersonalAccessTokensClient;
import org.apache.iggy.client.blocking.StreamsClient;
import org.apache.iggy.client.blocking.SystemClient;
import org.apache.iggy.client.blocking.TopicsClient;
import org.apache.iggy.client.blocking.UsersClient;
import org.apache.iggy.client.blocking.http.IggyHttpClient;
import org.apache.iggy.client.blocking.tcp.IggyTcpClient;

public class Client {

    /**
     * Builds an Iggy TCP client using the provided host and port.
     *
     * @param host The host address
     * @param port The port number
     * @return An IggyClient instance with TCP connection
     */
    public static IggyClient buildTcpClient(String host, int port) {
        IggyBaseClient baseClient = new IggyTcpClient(host, port);
        return new IggyClientBuilder()
                .withBaseClient(baseClient)
                .build();
    }

    /**
     * Builds an Iggy client using a custom base client.
     *
     * @param baseClient The base client implementation
     * @return An IggyClient instance
     */
    public static IggyClient buildClient(IggyBaseClient baseClient) {
        return new IggyClientBuilder()
                .withBaseClient(baseClient)
                .build();
    }

    /**
     * Builds a TCP client from any Args class that implements ClientArgs.
     *
     * @param args The arguments containing host and port
     * @return An IggyClient instance with TCP connection
     */
    public static IggyClient buildTcpClientFromArgs(ClientArgs args) {
        return buildTcpClient(args.getHost(), args.getPort());
    }

    /**
     * Builds an HTTP client from any Args class that implements ClientArgs.
     *
     * @param args The arguments containing host and port
     * @return An IggyClient instance with HTTP connection
     */

    public static IggyClient buildHttpClientFromArgs(ClientArgs args) {
        IggyBaseClient baseClient = new IggyHttpClient(args.getHost());
        return buildClient(baseClient);
    }

    /**
     * Gets a SystemClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A SystemClient instance
     */
    public static SystemClient systemClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().system();
    }

    /**
     * Gets a StreamsClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A StreamsClient instance
     */
    public static StreamsClient streamsClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().streams();
    }

    /**
     * Gets a UsersClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A UsersClient instance
     */
    public static UsersClient usersClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().users();
    }

    /**
     * Gets a TopicsClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A TopicsClient instance
     */
    public static TopicsClient topicsClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().topics();
    }

    /**
     * Gets a PartitionsClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A PartitionsClient instance
     */
    public static PartitionsClient partitionsClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().partitions();
    }

    /**
     * Gets a ConsumerGroupsClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A ConsumerGroupsClient instance
     */
    public static ConsumerGroupsClient consumerGroupsClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().consumerGroups();
    }

    /**
     * Gets a ConsumerOffsetsClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A ConsumerOffsetsClient instance
     */
    public static ConsumerOffsetsClient consumerOffsetsClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().consumerOffsets();
    }

    /**
     * Gets a MessagesClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A MessagesClient instance
     */
    public static MessagesClient messagesClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().messages();
    }

    /**
     * Gets a PersonalAccessTokensClient using TCP connection.
     *
     * @param host The host address
     * @param port The port number
     * @return A PersonalAccessTokensClient instance
     */
    public static PersonalAccessTokensClient personalAccessTokensClient(String host, int port) {
        return buildTcpClient(host, port).getBaseClient().personalAccessTokens();
    }

}