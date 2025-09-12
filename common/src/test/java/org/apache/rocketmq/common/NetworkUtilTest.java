package org.apache.rocketmq.common;

import org.apache.rocketmq.common.utils.NetworkUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkUtilTest {
    @Test
    public void testGetLocalAddress() {
        String localAddress = NetworkUtil.getLocalAddress();
        System.out.println(localAddress);
        assertThat(localAddress).isNotNull();
        assertThat(localAddress.length()).isGreaterThan(0);
    }

    @Test
    public void testConvert2IpStringWithIp() {
        String result = NetworkUtil.convert2IpString("127.0.0.1:9876");
        assertThat(result).isEqualTo("127.0.0.1:9876");
    }

    @Test
    public void testConvert2IpStringWithHost() {
        String result = NetworkUtil.convert2IpString("localhost:9876");
        assertThat(result).isEqualTo("127.0.0.1:9876");
    }
}
