package io.github.heisenberguwu.myrocketmq.common.namesrv;

import io.github.heisenberguwu.myrocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultTopAddressing implements TopAddressing {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    //
    private String nsAddr;
    private String wsAddr;
    private String unitName;
    private Map<String, String> para;
    private List<TopAddressing> topAddressingList;

    public DefaultTopAddressing(final String wsAddr) {
        this(wsAddr, null);
    }


    public DefaultTopAddressing(final String wsAddr, final String unitName) {
        this.wsAddr = wsAddr;
        this.unitName = unitName;
        this.topAddressingList = loadCustomTopAddressing();
    }

    public DefaultTopAddressing(final String unitName, final Map<String, String> para, final String wsAddr) {
        this.wsAddr = wsAddr;
        this.unitName = unitName;
        this.para = para;
        this.topAddressingList = loadCustomTopAddressing();
    }

    private static String clearNewLine(final String str) {
        String newString = str.trim();
        int index = newString.indexOf("\r");
        if (index != -1) {
            return newString.substring(0, index);
        }

        index = newString.indexOf("\n");
        if (index != -1) {
            return newString.substring(0, index);
        }

        return newString;
    }


    private List<TopAddressing> loadCustomTopAddressing() {
        ServiceLoader<TopAddressing> serviceLoader = ServiceLoader.load(TopAddressing.class);
        Iterator<TopAddressing> iterator = serviceLoader.iterator();
        List<TopAddressing> topAddressingList = new ArrayList<>();
        if (iterator.hasNext()) {
            topAddressingList.add(iterator.next());
        }
        return topAddressingList;
    }


    @Override
    public String fetchNSAddr() {
        return "";
    }

    @Override
    public void registerChangeCallBack(NameServerUpdateCallback changeCallBack) {

    }

    public String getNsAddr() {
        return nsAddr;
    }

    public void setNsAddr(String nsAddr) {
        this.nsAddr = nsAddr;
    }
}