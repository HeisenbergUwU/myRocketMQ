package org.apache.rocketmq.client.common;

public class NameserverAccessConfig {
    private String namesrvAddr;
    private String namesrvDomain;
    private String namesrvDomainSubgroup;

    public NameserverAccessConfig(String namesrvAddr, String namesrvDomain, String namesrvDomainSubgroup) {
        this.namesrvAddr = namesrvAddr;
        this.namesrvDomain = namesrvDomain;
        this.namesrvDomainSubgroup = namesrvDomainSubgroup;
    }

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public String getNamesrvDomain() {
        return namesrvDomain;
    }

    public String getNamesrvDomainSubgroup() {
        return namesrvDomainSubgroup;
    }
}
