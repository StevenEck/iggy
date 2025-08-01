# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# This file is automatically generated by pyo3_stub_gen
# ruff: noqa: E501, F401

import asyncio
import builtins
import collections.abc
import datetime
import typing
from enum import Enum

class IggyClient:
    r"""
    A Python class representing the Iggy client.
    It wraps the RustIggyClient and provides asynchronous functionality
    through the contained runtime.
    """
    def __new__(cls, conn:typing.Optional[builtins.str]=None) -> IggyClient:
        r"""
        Constructs a new IggyClient.
        
        This initializes a new runtime for asynchronous operations.
        Future versions might utilize asyncio for more Pythonic async.
        """
    def ping(self) -> typing.Any:
        r"""
        Sends a ping request to the server to check connectivity.
        
        Returns `Ok(())` if the server responds successfully, or a `PyRuntimeError`
        if the connection fails.
        """
    def login_user(self, username:builtins.str, password:builtins.str) -> typing.Any:
        r"""
        Logs in the user with the given credentials.
        
        Returns `Ok(())` on success, or a PyRuntimeError on failure.
        """
    def connect(self) -> typing.Any:
        r"""
        Connects the IggyClient to its service.
        
        Returns Ok(()) on successful connection or a PyRuntimeError on failure.
        """
    def create_stream(self, name:builtins.str, stream_id:typing.Optional[builtins.int]=None) -> typing.Any:
        r"""
        Creates a new stream with the provided ID and name.
        
        Returns Ok(()) on successful stream creation or a PyRuntimeError on failure.
        """
    def get_stream(self, stream_id:builtins.str | builtins.int) -> typing.Any:
        r"""
        Gets stream by id.
        
        Returns Option of stream details or a PyRuntimeError on failure.
        """
    def create_topic(self, stream:builtins.str | builtins.int, name:builtins.str, partitions_count:builtins.int, compression_algorithm:typing.Optional[builtins.str]=None, topic_id:typing.Optional[builtins.int]=None, replication_factor:typing.Optional[builtins.int]=None) -> typing.Any:
        r"""
        Creates a new topic with the given parameters.
        
        Returns Ok(()) on successful topic creation or a PyRuntimeError on failure.
        """
    def get_topic(self, stream_id:builtins.str | builtins.int, topic_id:builtins.str | builtins.int) -> typing.Any:
        r"""
        Gets topic by stream and id.
        
        Returns Option of topic details or a PyRuntimeError on failure.
        """
    def send_messages(self, stream:builtins.str | builtins.int, topic:builtins.str | builtins.int, partitioning:builtins.int, messages:list) -> typing.Any:
        r"""
        Sends a list of messages to the specified topic.
        
        Returns Ok(()) on successful sending or a PyRuntimeError on failure.
        """
    def poll_messages(self, stream:builtins.str | builtins.int, topic:builtins.str | builtins.int, partition_id:builtins.int, polling_strategy:PollingStrategy, count:builtins.int, auto_commit:builtins.bool) -> typing.Any:
        r"""
        Polls for messages from the specified topic and partition.
        
        Returns a list of received messages or a PyRuntimeError on failure.
        """
    def consumer_group(self, name:builtins.str, stream:builtins.str, topic:builtins.str, partition_id:typing.Optional[builtins.int]=None, polling_strategy:typing.Optional[PollingStrategy]=None, batch_length:typing.Optional[builtins.int]=None, auto_commit:typing.Optional[AutoCommit]=None, create_consumer_group_if_not_exists:builtins.bool=True, auto_join_consumer_group:builtins.bool=True, poll_interval:typing.Optional[datetime.timedelta]=None, polling_retry_interval:typing.Optional[datetime.timedelta]=None, init_retries:typing.Optional[builtins.int]=None, init_retry_interval:typing.Optional[datetime.timedelta]=None, allow_replay:builtins.bool=False) -> IggyConsumer:
        r"""
        Creates a new consumer group consumer.
        
        Returns the consumer or a PyRuntimeError on failure.
        """

class IggyConsumer:
    r"""
    A Python class representing the Iggy consumer.
    It wraps the RustIggyConsumer and provides asynchronous functionality
    through the contained runtime.
    """
    def get_last_consumed_offset(self, partition_id:builtins.int) -> typing.Optional[builtins.int]:
        r"""
        Get the last consumed offset or `None` if no offset has been consumed yet.
        """
    def get_last_stored_offset(self, partition_id:builtins.int) -> typing.Optional[builtins.int]:
        r"""
        Get the last stored offset or `None` if no offset has been stored yet.
        """
    def name(self) -> builtins.str:
        r"""
        Gets the name of the consumer group.
        """
    def partition_id(self) -> builtins.int:
        r"""
        Gets the current partition id or `0` if no messages have been polled yet.
        """
    def stream(self) -> builtins.str | builtins.int:
        r"""
        Gets the name of the stream this consumer group is configured for.
        """
    def topic(self) -> builtins.str | builtins.int:
        r"""
        Gets the name of the topic this consumer group is configured for.
        """
    def store_offset(self, offset:builtins.int, partition_id:typing.Optional[builtins.int]) -> typing.Any:
        r"""
        Stores the provided offset for the provided partition id or if none is specified
        uses the current partition id for the consumer group.
        
        Returns `Ok(())` if the server responds successfully, or a `PyRuntimeError`
        if the operation fails.
        """
    def delete_offset(self, partition_id:typing.Optional[builtins.int]) -> typing.Any:
        r"""
        Deletes the offset for the provided partition id or if none is specified
        uses the current partition id for the consumer group.
        
        Returns `Ok(())` if the server responds successfully, or a `PyRuntimeError`
        if the operation fails.
        """
    def consume_messages(self, callback:collections.abc.Callable[[str]], shutdown_event:typing.Optional[asyncio.Event]) -> typing.Any:
        r"""
        Consumes messages continuously using a callback function and an optional `asyncio.Event` for signaling shutdown.
        
        Returns an awaitable that completes when shutdown is signaled or a PyRuntimeError on failure.
        """

class ReceiveMessage:
    r"""
    A Python class representing a received message.
    
    This class wraps a Rust message, allowing for access to its payload and offset from Python.
    """
    def payload(self) -> typing.Any:
        r"""
        Retrieves the payload of the received message.
        
        The payload is returned as a Python bytes object.
        """
    def offset(self) -> builtins.int:
        r"""
        Retrieves the offset of the received message.
        
        The offset represents the position of the message within its topic.
        """
    def timestamp(self) -> builtins.int:
        r"""
        Retrieves the timestamp of the received message.
        
        The timestamp represents the time of the message within its topic.
        """
    def id(self) -> builtins.int:
        r"""
        Retrieves the id of the received message.
        
        The id represents unique identifier of the message within its topic.
        """
    def checksum(self) -> builtins.int:
        r"""
        Retrieves the checksum of the received message.
        
        The checksum represents the integrity of the message within its topic.
        """
    def length(self) -> builtins.int:
        r"""
        Retrieves the length of the received message.
        
        The length represents the length of the payload.
        """

class SendMessage:
    r"""
    A Python class representing a message to be sent.
    
    This class wraps a Rust message meant for sending, facilitating
    the creation of such messages from Python and their subsequent use in Rust.
    """
    def __new__(cls, data:builtins.str) -> SendMessage:
        r"""
        Constructs a new `SendMessage` instance from a string.
        
        This method allows for the creation of a `SendMessage` instance
        directly from Python using the provided string data.
        """

class StreamDetails:
    id: builtins.int
    name: builtins.str
    messages_count: builtins.int
    topics_count: builtins.int

class TopicDetails:
    id: builtins.int
    name: builtins.str
    messages_count: builtins.int
    partitions_count: builtins.int

class AutoCommit(Enum):
    r"""
    The auto-commit configuration for storing the offset on the server.
    """
    Disabled = ...
    r"""
    The auto-commit is disabled and the offset must be stored manually by the consumer.
    """
    Interval = ...
    r"""
    The auto-commit is enabled and the offset is stored on the server after a certain interval.
    """
    IntervalOrWhen = ...
    r"""
    The auto-commit is enabled and the offset is stored on the server after a certain interval or depending on the mode when consuming the messages.
    """
    IntervalOrAfter = ...
    r"""
    The auto-commit is enabled and the offset is stored on the server after a certain interval or depending on the mode after consuming the messages.
    """
    When = ...
    r"""
    The auto-commit is enabled and the offset is stored on the server depending on the mode when consuming the messages.
    """
    After = ...
    r"""
    The auto-commit is enabled and the offset is stored on the server depending on the mode after consuming the messages.
    """

class AutoCommitAfter(Enum):
    r"""
    The auto-commit mode for storing the offset on the server **after** receiving the messages.
    """
    ConsumingAllMessages = ...
    r"""
    The offset is stored on the server after all the messages are consumed.
    """
    ConsumingEachMessage = ...
    r"""
    The offset is stored on the server after consuming each message.
    """
    ConsumingEveryNthMessage = ...
    r"""
    The offset is stored on the server after consuming every Nth message.
    """

class AutoCommitWhen(Enum):
    r"""
    The auto-commit mode for storing the offset on the server.
    """
    PollingMessages = ...
    r"""
    The offset is stored on the server when the messages are received.
    """
    ConsumingAllMessages = ...
    r"""
    The offset is stored on the server when all the messages are consumed.
    """
    ConsumingEachMessage = ...
    r"""
    The offset is stored on the server when consuming each message.
    """
    ConsumingEveryNthMessage = ...
    r"""
    The offset is stored on the server when consuming every Nth message.
    """

class PollingStrategy(Enum):
    Offset = ...
    Timestamp = ...
    First = ...
    Last = ...
    Next = ...

