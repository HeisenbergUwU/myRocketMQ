package org.apache.rocketmq.common.namesrv;

import com.google.common.base.Strings;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.help.FAQUrl;
import org.apache.rocketmq.common.utils.HttpTinyClient;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class DefaultTopAddressing implements TopAddressing {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);

    private String nsAddr; // NameServer 地址
    private String wsAddr; // WebServer 地址
    private String unitName; // 单位名称
    private Map<String, String> para; // URL构建时候的参数
    private List<TopAddressing> topAddressingList; // 使用不同的策略实现的TopAddressing

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
        /**
         * str.trim()：去掉输入字符串 str 两端的空白字符（包括空格、Tab、回车 \r 和换行 \n）。
         * 查找第一个回车符 \r 的位置：
         * 如果存在，返回从开头到该回车符前的子串（不包括回车和它后面的内容）。
         * 查找第一个换行符 \n 的位置：
         * 如果存在（并且没有回车），返回它之前的内容。
         * 如果既没有 \r 也没有 \n，返回已 trim() 处理的字符串。
         */
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
        if (!this.topAddressingList.isEmpty()) {
            for (TopAddressing topAddressing : topAddressingList) {
                String nsAddress = topAddressing.fetchNSAddr();
                if (!Strings.isNullOrEmpty(nsAddress)) {
                    return nsAddress;
                }
            }
        }
        // Return result of default implementation
        return fetchNSAddr(true, 3000);
    }

    /**
     * 这里有一个默认实现 - 吓老子一跳
     * @param verbose
     * @param timeoutMills
     * @return
     */
    public final String fetchNSAddr(boolean verbose, long timeoutMills) {
        StringBuilder url = new StringBuilder(this.wsAddr);
        try {
            if (null != para && para.size() > 0) {
                if (!UtilAll.isBlank(this.unitName)) {
                    url.append("-").append(this.unitName).append("?nofix=1&");
                } else {
                    url.append("?");
                }
                for (Map.Entry<String, String> entry : this.para.entrySet()) {
                    url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
                url = new StringBuilder(url.substring(0, url.length() - 1));
            } else {
                if (!UtilAll.isBlank(this.unitName)) {
                    url.append("-").append(this.unitName).append("?nofix=1");
                }
            }

            HttpTinyClient.HttpResult result = HttpTinyClient.httpGet(url.toString(), null, null, "UTF-8", timeoutMills);
            if (200 == result.code) {
                String responseStr = result.content;
                if (responseStr != null) {
                    return clearNewLine(responseStr);
                } else {
                    LOGGER.error("fetch nameserver address is null");
                }
            } else {
                LOGGER.error("fetch nameserver address failed. statusCode=" + result.code);
            }
        } catch (IOException e) {
            if (verbose) {
                LOGGER.error("fetch name server address exception", e);
            }
        }

        if (verbose) {
            String errorMsg =
                    "connect to " + url + " failed, maybe the domain name " + MixAll.getWSAddr() + " not bind in /etc/hosts";
            errorMsg += FAQUrl.suggestTodo(FAQUrl.NAME_SERVER_ADDR_NOT_EXIST_URL);

            LOGGER.warn(errorMsg);
        }
        return null;
    }

    @Override
    public void registerChangeCallBack(NameServerUpdateCallback changeCallBack) {
        if (!topAddressingList.isEmpty()) {
            for (TopAddressing topAddressing : topAddressingList) {
                topAddressing.registerChangeCallBack(changeCallBack);
            }
        }
    }

    public String getNsAddr() {
        return nsAddr;
    }

    public void setNsAddr(String nsAddr) {
        this.nsAddr = nsAddr;
    }
}