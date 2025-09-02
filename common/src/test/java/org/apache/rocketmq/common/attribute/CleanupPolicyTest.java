package org.apache.rocketmq.common.attribute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CleanupPolicyTest {

    @Test
    public void testCleanupPolicy_Delete() {
        CleanupPolicy cleanupPolicy = CleanupPolicy.DELETE;
        assertEquals("DELETE", cleanupPolicy.toString());
    }

    @Test
    public void testCleanupPolicy_Compaction() {
        CleanupPolicy cleanupPolicy = CleanupPolicy.COMPACTION;
        assertEquals("COMPACTION", cleanupPolicy.toString());
    }
}
