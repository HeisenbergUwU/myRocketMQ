package io.github.heisenberguwu.myrocketmq.common.utils;

import io.github.heisenberguwu.myrocketmq.common.MQVersion;
import io.github.heisenberguwu.myrocketmq.common.MixAll;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

/**
 * http 轻量级请求连接封
 * <p>
 * GET 报文：
 * <p>
 * GET /index.html HTTP/1.1
 * Host: www.example.com
 * User-Agent: MyClient/1.0
 * Accept: text/html
 * <p>
 * <p>
 * POST报文：
 * <p>
 * POST /submit-form HTTP/1.1
 * Host: www.example.com
 * Content-Type: application/x-www-form-urlencoded
 * Content-Length: 27
 * <p>
 * field1=value1&field2=value2
 * <p>
 * 200 OK
 * <p>
 * HTTP/1.1 200 OK
 * Date: Tue, 01 Jul 2025 12:00:00 GMT
 * Server: Apache/2.4.46
 * Content-Type: text/html; charset=UTF-8
 * Content-Length: 44
 *
 * <html><body><h1>Hello, world!</h1></body></html>
 * <p>
 * 404 NOT FOUND
 * <p>
 * HTTP/1.1 404 Not Found
 * Date: Tue, 01 Jul 2025 12:05:00 GMT
 * Server: nginx/1.18.0
 * Content-Type: text/html; charset=UTF-8
 * Content-Length: 76
 *
 * <html><body><h1>404 Not Found</h1><p>Page not found.</p></body></html>
 */
public class HttpTinyClient {
    public static HttpResult httpGet(String url, List<String> headers, List<String> paramValues,
                                     String encoding, long readTimeoutMs) throws IOException {
        String encodedContent = encodingParams(paramValues, encoding);
        url += (null == encodedContent) ? "" : ("?" + encodedContent);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout((int) readTimeoutMs);
            conn.setReadTimeout((int) readTimeoutMs);
            setHeaders(conn, headers, encoding);

            conn.connect(); // call
            int respCode = conn.getResponseCode();
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
            }
            return new HttpResult(respCode, resp);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static private String encodingParams(List<String> paramValues, String encoding) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == paramValues) {
            return null;
        }

        for (Iterator<String> iter = paramValues.iterator(); iter.hasNext(); ) {
            sb.append(iter.next()).append("=");
            /*
            将字母和数字保持不变，允许 . - * _ 保留原样；

            空格被转换成加号 (+)；

            其他不安全字符（比如 ü、@、#、非 ASCII 等）先转成字节（按指定 encoding，如 UTF‑8），然后每个字节转换成 % 后跟两位十六进制表示；例如 ü（在 UTF‑8 中是 C3 BC）变成 %C3%BC，@ 变成 %40
             */
            sb.append(URLEncoder.encode(iter.next(), encoding));
            if (iter.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * @return the http response of given http post request
     */
    static public HttpResult httpPost(String url, List<String> headers, List<String> paramValues,
                                      String encoding, long readTimeoutMs) throws IOException {
        String encodedContent = encodingParams(paramValues, encoding);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout((int) readTimeoutMs);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            setHeaders(conn, headers, encoding);

            conn.getOutputStream().write(encodedContent.getBytes(MixAll.DEFAULT_CHARSET));

            int respCode = conn.getResponseCode();
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
            }
            return new HttpResult(respCode, resp);
        } finally {
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    static private void setHeaders(HttpURLConnection conn, List<String> headers, String encoding) {
        /**
         * | Header 名称                | 用途说明                      |
         * | ------------------------ | ------------------------- |
         * | `Client-Version`         | 标明客户端版本，帮助后端验证兼容性、收集使用统计  |
         * | `Content-Type`           | 指定请求体格式和字符集，保障正确解析表单数据    |
         * | `Metaq-Client-RequestTS` | 提供请求发起时间，有助于防重放、日志追踪或性能统计 |
         */
        if (null != headers) {
            for (Iterator<String> iter = headers.iterator(); iter.hasNext(); ) {
                conn.addRequestProperty(iter.next(), iter.next());
            }
        }
        conn.addRequestProperty("Client-Version", MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + encoding); // url 传参

        String ts = String.valueOf(System.currentTimeMillis());
        conn.addRequestProperty("Metaq-Client-RequestTS", ts);
    }

    static public class HttpResult {
        final public int code;
        final public String content;

        public HttpResult(int code, String content) {
            this.code = code;
            this.content = content;
        }
    }
}