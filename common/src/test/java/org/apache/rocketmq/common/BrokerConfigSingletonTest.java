package org.apache.rocketmq.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

public class BrokerConfigSingletonTest {

    private static final Log log = LogFactory.getLog(BrokerConfigSingletonTest.class);

    /**
     * Tests the behavior of getting the broker configuration when it has not been initialized.
     * Expects an IllegalArgumentException to be thrown, ensuring that the configuration cannot be obtained without initialization.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getBrokerConfig_NullConfiguration_ThrowsException() {
        BrokerConfigSingleton.getBrokerConfig();
    }

    /**
     * Tests the behavior of setting the broker configuration after it has already been initialized.
     * Expects an IllegalArgumentException to be thrown, ensuring that the configuration cannot be reset once set.
     * Also asserts that the returned brokerConfig instance is the same as the one set, confirming the singleton property.
     */
    @Test(expected = IllegalArgumentException.class)
    public void setBrokerConfig_AlreadyInitialized_ThrowsException() {
        log.info("hihihi");
        BrokerConfig config = new BrokerConfig();
        BrokerConfigSingleton.setBrokerConfig(config);
        Assert.assertSame("Expected brokerConfig instance is not returned", config, BrokerConfigSingleton.getBrokerConfig());
        BrokerConfigSingleton.setBrokerConfig(config);
    }

}
