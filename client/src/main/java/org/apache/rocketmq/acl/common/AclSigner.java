package org.apache.rocketmq.acl.common;

import org.apache.commons.codec.binary.Base64;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 一段式概述：开头即简要说明类职责 —— 用于 HMAC 签名 + Base64 编码，在 RocketMQ 的 ACL 中起到关键作用。
 * <p>
 * 功能细节：明确列出默认配置、支持的 API 方法、底层实现流程。
 * <p>
 * 容错策略：指出出错时如何处理（日志 + 抛出自定义异常）。
 * <p>
 * 上下文说明：强调它在 RocketMQ ACL 认证流程中的位置，为读者提供背景信息。
 * <p>
 * 引用文档：引用 RocketMQ 官方 ACL 文档，说明签名机制在权限校验中的作用
 */
public class AclSigner {
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final SigningAlgorithm DEFAULT_ALGORITHM = SigningAlgorithm.HmacSHA1;
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_AUTHORIZE_LOGGER_NAME);
    private static final int CAL_SIGNATURE_FAILED = 10015;
    private static final String CAL_SIGNATURE_FAILED_MSG = "[%s:signature-failed] unable to calculate a request signature. error=%s";


    public static String calSignature(String data, String key) throws AclException {
        return calSignature(data, key, DEFAULT_ALGORITHM, DEFAULT_CHARSET);
    }

    public static String calSignature(String data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        return signAndBase64Encode(data, key, algorithm, charset);
    }

    public static String calSignature(byte[] data, String key) throws AclException {
        return calSignature(data, key, DEFAULT_ALGORITHM, DEFAULT_CHARSET);
    }


    public static String calSignature(byte[] data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        return signAndBase64Encode(data, key, algorithm, charset);
    }

    private static String signAndBase64Encode(byte[] data, String key, SigningAlgorithm algorithm, Charset charset) throws AclException {
        try {
            byte[] signature = sign(data, key.getBytes(charset), algorithm);
            return new String(Base64.encodeBase64(signature), DEFAULT_CHARSET);
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, CAL_SIGNATURE_FAILED, e.getMessage());
            log.error(message, e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }

    private static String signAndBase64Encode(String data, String key, SigningAlgorithm algorithm, Charset charset)
            throws AclException {
        try {
            byte[] signature = sign(data.getBytes(charset), key.getBytes(charset), algorithm);
            return new String(Base64.encodeBase64(signature), DEFAULT_CHARSET);
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, CAL_SIGNATURE_FAILED, e.getMessage());
            log.error(message, e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }

    private static byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) throws NoSuchAlgorithmException {
        // import java.security.MessageDigest; 这个包不能带 秘钥认证
        // MessageDigest.getInstance("MD5")
        // "Message Authentication Code" (MAC)
        try {
            Mac mac = Mac.getInstance(algorithm.toString()); // 根据 算法名称 生成 MAC 实体对象
            mac.init(new SecretKeySpec(key, algorithm.toString())); // 进一步初始化 ，使用key进行关联
            return mac.doFinal(data); // 计算并且返回
        } catch (Exception e) {
            String message = String.format(CAL_SIGNATURE_FAILED_MSG, CAL_SIGNATURE_FAILED, e.getMessage());
            log.error(message, e);
            throw new AclException("CAL_SIGNATURE_FAILED", CAL_SIGNATURE_FAILED, message, e);
        }
    }
}