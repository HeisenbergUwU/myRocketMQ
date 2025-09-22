package org.apache.rocketmq.remoting.rpc;

import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MockTest {

    @Mock
    private RemotingClient remotingClient;

    @Test
    public void test() throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        System.out.println(remotingClient.getNameServerAddressList());
        System.out.println(remotingClient.invokeSync("", null, 1));
    }

}
