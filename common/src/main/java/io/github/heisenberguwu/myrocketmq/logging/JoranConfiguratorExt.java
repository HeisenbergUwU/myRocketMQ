package io.github.heisenberguwu.myrocketmq.logging;

import com.google.common.io.CharStreams;
import org.apache.rocketmq.logging.ch.qos.logback.classic.joran.JoranConfigurator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
}
