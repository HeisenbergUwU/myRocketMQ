package org.apache.rocketmq.common.logging;

import com.google.common.io.CharStreams;
import org.apache.rocketmq.logging.ch.qos.logback.classic.joran.JoranConfigurator;
import org.apache.rocketmq.logging.ch.qos.logback.core.joran.spi.JoranException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class JoranConfiguratorExt extends JoranConfigurator {
    private InputStream transformXml(InputStream in) throws IOException {
        try {
            String str = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
            str = str.replace("\"ch.qos.logback", "\"org.apache.rocketmq.logging.ch.qos.logback");
            return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        } finally {
            if (null != in) {
                in.close();
            }
        }
    }

    public final void doConfigure0(URL url) throws JoranException {
        /**
         * 加载一个 XML 配置文件（通过 URL）
         *
         * 对其中的内容做一次自定义转换（如类名替换）
         *
         * 然后交给 logback 的 doConfigure() 方法执行配置
         *
         * 对整个流程做异常处理和日志记录
         */

        InputStream in = null; // 输入流对象，用于读取配置文件内容
        try {
            // 通知上下文，这次配置使用的是这个 URL，对 logback 上下文有用
            informContextOfURLUsedForConfiguration(getContext(), url);

            // 打开 URL 的连接（可能是本地文件、远程资源等）
            URLConnection urlConnection = url.openConnection();

            // 禁用缓存，防止读取的是旧数据（logback 的配置读取缓存可能出问题）
            // 参考 logback 的两个已知问题（LBCORE-105 和 LBCORE-127）
            urlConnection.setUseCaches(false);

            // 获取原始输入流，准备读取 XML 配置文件
            InputStream temp = urlConnection.getInputStream();

            // 调用自定义方法，将 XML 中的类名做替换（例如替换 logback 的包路径）
            in = transformXml(temp);

            // 执行真正的配置解析工作，传入转换后的流和路径
            doConfigure(in, url.toExternalForm());
        } catch (IOException ioe) {
            // 如果打开 URL 或读取失败，构造错误信息
            String errMsg = "Could not open URL [" + url + "].";

            // 记录错误日志（logback 的错误处理方式）
            addError(errMsg, ioe);

            // 抛出 JoranException 终止配置流程
            throw new JoranException(errMsg, ioe);
        } finally {
            // 无论成功或失败，都确保关闭输入流，避免内存泄漏
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // 如果关闭失败，也记录错误并抛出异常
                    String errMsg = "Could not close input stream";
                    addError(errMsg, ioe);
                    throw new JoranException(errMsg, ioe);
                }
            }
        }
    }


}
