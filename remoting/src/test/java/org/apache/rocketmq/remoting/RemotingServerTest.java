/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.remoting;

import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.remoting.annotation.CFNullable;
import org.apache.rocketmq.remoting.exception.*;
import org.apache.rocketmq.remoting.netty.*;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class RemotingServerTest {
    private static RemotingServer remotingServer;
    private static RemotingClient remotingClient;

    public static RemotingServer createRemotingServer() throws InterruptedException {
        NettyServerConfig config = new NettyServerConfig();
        NettyRemotingServer remotingServer = new NettyRemotingServer(config);
        remotingServer.registerProcessor(0, new NettyRequestProcessor() {
                    @Override
                    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                        request.setRemark("Hi" + ctx.channel().remoteAddress());
                        return request;
                    }

                    @Override
                    public boolean rejectRequest() {
                        return false;
                    }
                },
                Executors.newCachedThreadPool());
        remotingServer.start();

        return remotingServer;
    }

    public static RemotingClient createRemotingClient() {
        return createRemotingClient(new NettyClientConfig());
    }

    public static RemotingClient createRemotingClient(NettyClientConfig nettyClientConfig) {
        RemotingClient client = new NettyRemotingClient(nettyClientConfig);
        client.start();
        return client;
    }

}