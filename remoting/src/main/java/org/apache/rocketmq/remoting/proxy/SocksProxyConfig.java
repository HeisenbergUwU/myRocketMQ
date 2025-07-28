package org.apache.rocketmq.remoting.proxy;

public class SocksProxyConfig {
    private String addr;
    private String username;
    private String password;

    public SocksProxyConfig() {
    }

    public SocksProxyConfig(String addr) {
        this.addr = addr;
    }

    public SocksProxyConfig(String addr, String username, String password) {
        this.addr = addr;
        this.username = username;
        this.password = password;
    }


    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return String.format("SocksProxy address: %s, username: %s, password: %s", addr, username, password);
    }
}