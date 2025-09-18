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

import org.apache.rocketmq.common.utils.NetworkUtil;
import org.apache.rocketmq.remoting.common.TlsMode;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingServer;
import org.apache.rocketmq.remoting.netty.TlsHelper;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.rocketmq.remoting.netty.TlsSystemConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class TlsTest {
    private RemotingServer remotingServer;
    private RemotingClient remotingClient;

    @Rule
    public TestName name = new TestName();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws InterruptedException {

    }

    @After
    public void tearDown() {
        remotingClient.shutdown();
        remotingServer.shutdown();
        tlsMode = TlsMode.PERMISSIVE;
    }

    @Test
    public void test() {
        getCertsPath("badClient.key");
    }


    private static String getCertsPath(String fileName) {
        ClassLoader loader = TlsTest.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream("certs/" + fileName);
        if (null == stream) {
            throw new RuntimeException("File: " + fileName + " is not found");
        }
        try {
            String[] segments = fileName.split("\\.");
            File f = File.createTempFile(UUID.randomUUID().toString(), segments[1]);
            f.deleteOnExit(); // 退出后自动删除
            try(BufferedInputStream bis = new BufferedInputStream(stream))
            {

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}
