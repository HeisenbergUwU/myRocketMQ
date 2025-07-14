package org.apache.rocketmq.remoting.rpc;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import org.apache.rocketmq.remoting.CommandCustomHeader;

public abstract class RpcRequestHeader implements CommandCustomHeader {
    //the namespace name
    protected String ns;
    //if the data has been namespaced
    protected Boolean nsd;
    //the abstract remote addr name, usually the physical broker name
    protected String bname; // broker 逻辑地址
    //oneway
    protected Boolean oway; // 表示客户端值发出请求不进行等待。异步调用，有助于提升吞吐、降低延迟；但也意味着无法获取 result 或失败信息，适用于幂等或边缘计算场景。

    @Deprecated
    public String getBname() {
        return bname;
    }

    @Deprecated
    public void setBname(String brokerName) {
        this.bname = brokerName;
    }

    public String getBrokerName() {
        return bname;
    }

    public void setBrokerName(String brokerName) {
        this.bname = brokerName;
    }

    public String getNamespace() {
        return ns;
    }

    public void setNamespace(String namespace) {
        this.ns = namespace;
    }

    public Boolean getNamespaced() {
        return nsd;
    }

    public void setNamespaced(Boolean namespaced) {
        this.nsd = namespaced;
    }

    public Boolean getOneway() {
        return oway;
    }

    public void setOneway(Boolean oneway) {
        this.oway = oneway;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcRequestHeader header = (RpcRequestHeader) o;
        return Objects.equals(ns, header.ns) && Objects.equals(nsd, header.nsd) && Objects.equals(bname, header.bname) && Objects.equals(oway, header.oway);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ns, nsd, bname, oway);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("namespace", ns)
                .add("namespaced", nsd)
                .add("brokerName", bname)
                .add("oneway", oway)
                .toString();
    }
}
