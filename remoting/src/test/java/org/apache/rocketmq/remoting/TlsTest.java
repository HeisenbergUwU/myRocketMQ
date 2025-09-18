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
/**
 * JUnit 4 provides several built-in rules, including:
 *
 * 1. __@Rule__ - The main annotation for applying rules to test methods
 * 2. __@ClassRule__ - Applies a rule to the entire test class (executed once per class)
 *
 * ## Common JUnit 4 Rules:
 *
 * 1. __TemporaryFolder__ - Creates a temporary directory for the test
 * 2. __TestName__ - Provides access to the current test method name
 * 3. __ExpectedException__ - Allows you to specify an expected exception
 * 4. __Timeout__ - Sets a timeout for test execution
 * 5. __Ignore__ - Skips a test method or class
 * 6. __Repeat__ - Repeats a test multiple times
 * 7. __Verifier__ - Allows you to verify conditions at the end of a test
 * 8. __TestWatcher__ - Watches test execution and provides callbacks for test events
 *
 */
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * The import static statement in Java is a feature that allows you to import static members (fields and methods)
 * from a class directly into your current namespace, so you can use them without qualifying them with the class name.
 */
import static org.apache.rocketmq.remoting.netty.TlsSystemConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class) // 通知junit 使用 MockitoJUnitRunner 执行器运行， 可以自动初始化 Mock InjectMocks Spy 注解
public class TlsTest {
    private RemotingServer remotingServer;
    private RemotingClient remotingClient;

    @Rule
    public TestName name = new TestName(); //

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws InterruptedException {
        tlsMode = TlsMode.ENFORCING; // Any connection attempt that doesn't use SSL/TLS will be rejected immediately.
        tlsTestModeEnable = false;
        tlsServerNeedClientAuth = "require";
        tlsServerKeyPath = getCertsPath("server.key");
        tlsServerCertPath = getCertsPath("server.pem");
        tlsServerAuthClient = true;
        tlsServerTrustCertPath = getCertsPath("ca.pem");
        tlsClientKeyPath = getCertsPath("client.key");
        tlsClientCertPath = getCertsPath("client.pem");
        tlsClientAuthServer = true;
        tlsClientTrustCertPath = getCertsPath("ca.pem");
        tlsClientKeyPassword = "1234";
        tlsServerKeyPassword = "";

        NettyClientConfig clientConfig = new NettyClientConfig();
        clientConfig.setUseTLS(true);

        if ("serverRejectsUntrustedClientCert".equals(name.getMethodName())) {
            // Create a client. Its credentials come from a CA that the server does not trust. The client
            // trusts both test CAs to ensure the handshake failure is due to the server rejecting the client's cert.
            tlsClientKeyPath = getCertsPath("badClient.key");
            tlsClientCertPath = getCertsPath("badClient.pem");
        } else if ("serverAcceptsUntrustedClientCert".equals(name.getMethodName())) {
            tlsClientKeyPath = getCertsPath("badClient.key");
            tlsClientCertPath = getCertsPath("badClient.pem");
            tlsServerAuthClient = false;
        } else if ("noClientAuthFailure".equals(name.getMethodName())) {
            //Clear the client cert config to ensure produce the handshake error
            tlsClientKeyPath = "";
            tlsClientCertPath = "";
        } else if ("clientRejectsUntrustedServerCert".equals(name.getMethodName())) {
            tlsServerKeyPath = getCertsPath("badServer.key");
            tlsServerCertPath = getCertsPath("badServer.pem");
        } else if ("clientAcceptsUntrustedServerCert".equals(name.getMethodName())) {
            tlsServerKeyPath = getCertsPath("badServer.key");
            tlsServerCertPath = getCertsPath("badServer.pem");
            tlsClientAuthServer = false;
        } else if ("serverNotNeedClientAuth".equals(name.getMethodName())) {
            tlsServerNeedClientAuth = "none";
            tlsClientKeyPath = "";
            tlsClientCertPath = "";
        } else if ("serverWantClientAuth".equals(name.getMethodName())) {
            tlsServerNeedClientAuth = "optional";
        } else if ("serverWantClientAuth_ButClientNoCert".equals(name.getMethodName())) {
            tlsServerNeedClientAuth = "optional";
            tlsClientKeyPath = "";
            tlsClientCertPath = "";
        } else if ("serverAcceptsUnAuthClient".equals(name.getMethodName())) {
            tlsMode = TlsMode.PERMISSIVE;
            tlsClientKeyPath = "";
            tlsClientCertPath = "";
            clientConfig.setUseTLS(false);
        } else if ("disabledServerRejectsSSLClient".equals(name.getMethodName())) {
            tlsMode = TlsMode.DISABLED;
        } else if ("disabledServerAcceptUnAuthClient".equals(name.getMethodName())) {
            tlsMode = TlsMode.DISABLED;
            tlsClientKeyPath = "";
            tlsClientCertPath = "";
            clientConfig.setUseTLS(false);
        } else if ("reloadSslContextForServer".equals(name.getMethodName())) {
            tlsClientAuthServer = false;
            tlsServerNeedClientAuth = "none";
        }

    }

    @After
    public void tearDown() {
        remotingClient.shutdown();
        remotingServer.shutdown();
        tlsMode = TlsMode.PERMISSIVE;
    }

    @Test
    public void test() {
        String certsPath = getCertsPath("badClient.key");

        System.out.println(certsPath);
    }

    @Test
    public void test123() {
        String certsPath = getCertsPath("badClient.key");

        System.out.println(certsPath);
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
            try (BufferedInputStream bis = new BufferedInputStream(stream);
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return f.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getServerAddress() {
        return "localhost:" + remotingServer.localListenPort();
    }
}

