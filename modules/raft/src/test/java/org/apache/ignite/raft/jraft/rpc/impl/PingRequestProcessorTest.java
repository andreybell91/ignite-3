/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite.raft.jraft.rpc.impl;

import org.apache.ignite.raft.jraft.RaftMessagesFactory;
import org.apache.ignite.raft.jraft.rpc.RpcRequests.ErrorResponse;
import org.apache.ignite.raft.jraft.test.MockAsyncContext;
import org.apache.ignite.raft.jraft.test.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingRequestProcessorTest {
    @Test
    public void testHandlePing() throws Exception {
        PingRequestProcessor processor = new PingRequestProcessor(null, new RaftMessagesFactory());
        MockAsyncContext ctx = new MockAsyncContext();
        processor.handleRequest(ctx, TestUtils.createPingRequest());
        ErrorResponse response = (ErrorResponse) ctx.getResponseObject();
        assertEquals(0, response.errorCode());
    }
}
