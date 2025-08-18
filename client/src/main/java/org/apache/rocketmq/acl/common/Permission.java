package org.apache.rocketmq.acl.common;

public class Permission {

    public static final byte DENY = 1;
    public static final byte ANY = 1 << 1;
    public static final byte PUB = 1 << 2;
    public static final byte SUB = 1 << 3;

    public static byte parsePermFromString(String permString) {
        if (permString == null) {
            return Permission.DENY;
        }
        switch (permString.trim()) {
            case AclConstants.PUB:
                return Permission.PUB;
            case AclConstants.SUB:
                return Permission.SUB;
            case AclConstants.PUB_SUB:
            case AclConstants.SUB_PUB:
                return Permission.PUB | Permission.SUB;
            case AclConstants.DENY:
                return Permission.DENY;
            default:
                return Permission.DENY;
        }
    }
}
