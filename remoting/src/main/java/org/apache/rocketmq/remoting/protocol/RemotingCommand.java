/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.remoting.protocol;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Stopwatch;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.BoundaryType;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.remoting.CommandCallback;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

/**
 * GPT4o 说这个很重要，是通信模块核心载体
 * <p>
 * 统一封装请求与响应
 * <p>
 * 它是客户端与服务端所有远程调用（同步/异步/单向）的消息承载结构，包含通用字段如 code（请求/响应类型），opaque（唯一请求标识），flag、header、body 等。
 * <p>
 * 定义协议格式
 * <p>
 * 无论是发送消息、拉取消息、心跳还是 Broker、NameServer、Proxy、Controller 之间同步状态，都通过 RemotingCommand 进行编码、传输和解码。
 * <p>
 * 可扩展性强
 * <p>
 * 使用 extFields 和自定义 header，可以灵活扩展，支持如 V2 版本、RPC 版本迭代，以及 Proxy、Controller 新节点的扩展。
 */
public class RemotingCommand {
    public static final String SERIALIZE_TYPE_PROPERTY = "rocketmq.serialize.type";
    public static final String SERIALIZE_TYPE_ENV = "ROCKETMQ_SERIALIZE_TYPE";
    public static final String REMOTING_VERSION_KEY = "rocketmq.remoting.version";
    static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_REMOTING_NAME);

    private static final int RPC_TYPE = 0; // 0, REQUEST_COMMAND
    private static final int RPC_ONEWAY = 1; // 0, RPC

    // Class<? extends CommandCustomHeader> 未知类型通配符，但是属于CommandCustomHeader的子类
    // 这个集合属于 一个类型 对应 多个 字段
    private static final Map<Class<? extends CommandCustomHeader>, Field[]> CLASS_HASH_MAP = new HashMap<>();
    // 这个类叫啥名字 - 集合
    private static final Map<Class, String> CANONICAL_NAME_CACHE = new HashMap<>();
    // 1, Oneway
    // 1, RESPONSE_COMMAND
    private static final Map<Field, Boolean> NULLABLE_FIELD_CACHE = new HashMap<>();
    /**
     * | 类类型    | `getName()`            | `getCanonicalName()`  | `getSimpleName()` |
     * | ------ | ---------------------- | --------------------- | ----------------- |
     * | 普通类    | `java.lang.Integer`    | `java.lang.Integer`   | `Integer`         |
     * | 成员内部类  | `java.util.Map$Entry`  | `java.util.Map.Entry` | `Entry`           |
     * | 匿名内部类  | `pkg.Outer$1`          | `null`                | `""` (空字符串)       |
     * | 基本类型数组 | `[I`                   | `int[]`               | `int[]`           |
     * | 对象类型数组 | `[Ljava.lang.Integer;` | `java.lang.Integer[]` | `Integer[]`       |
     */
    private static final String STRING_CANONICAL_NAME = String.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_1 = Double.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_2 = double.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_1 = Integer.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_2 = int.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_1 = Long.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_2 = long.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_1 = Boolean.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_2 = boolean.class.getCanonicalName();
    private static final String BOUNDARY_TYPE_CANONICAL_NAME = BoundaryType.class.getCanonicalName();

    private static volatile int configVersion = -1;
    private static AtomicInteger requestId = new AtomicInteger(0);
    // 序列化类型 - 这里是 JSON
    private static SerializeType serializeTypeConfigInThisServer = SerializeType.JSON;

    static {
        // 初始化时候查看 JVM -D 参数、环境变量
        final String protocol = System.getProperty(SERIALIZE_TYPE_PROPERTY, System.getenv(SERIALIZE_TYPE_ENV));
        if (!StringUtils.isBlank(protocol)) {
            try {
                serializeTypeConfigInThisServer = SerializeType.valueOf(protocol);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("parser specified protocol error. protocol=" + protocol, e);
            }
        }
    }

    private int code;
    // 那种语言
    private LanguageCode language = LanguageCode.JAVA;
    private int version = 0;
    private int opaque = requestId.getAndIncrement(); // 原子操作
    private int flag = 0;
    private String remark;
    private HashMap<String, String> extFields;
    // 不进行序列化
    private transient CommandCustomHeader customHeader;
    private transient CommandCustomHeader cachedHeader;
    // RPC 方法
    private SerializeType serializeTypeCurrentRPC = serializeTypeConfigInThisServer;

    private transient byte[] body;
    private boolean suspended;
    private transient Stopwatch processTimer;
    private transient List<CommandCallback> callbackList;

    protected RemotingCommand() {
    }

    public static RemotingCommand createRequestCommand(int code, CommandCustomHeader customHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.setCode(code);
        cmd.customHeader = customHeader;
        setCmdVersion(cmd);
        return cmd;
    }

    public static RemotingCommand createResponseCommandWithHeader(int code, CommandCustomHeader customHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.setCode(code);
        cmd.markResponseType();
        cmd.customHeader = customHeader;
        setCmdVersion(cmd);
        return cmd;
    }

    /**
     * 设定RPC指令版本号
     *
     * @param cmd
     */
    protected static void setCmdVersion(RemotingCommand cmd) {
        if (configVersion >= 0) {
            cmd.setVersion(configVersion);
        } else {
            String v = System.getProperty(REMOTING_VERSION_KEY);
            if (v != null) {
                int value = Integer.parseInt(v);
                cmd.setVersion(value);
                configVersion = value;
            }
        }
    }

    public static RemotingCommand createResponseCommand(Class<? extends CommandCustomHeader> classHeader) {
        return createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR, "not set any response code", classHeader);
    }

    public static RemotingCommand buildErrorResponse(int code, String remark,
                                                     Class<? extends CommandCustomHeader> classHeader) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(classHeader);
        response.setCode(code);
        response.setRemark(remark);
        return response;
    }

    public static RemotingCommand buildErrorResponse(int code, String remark) {
        return buildErrorResponse(code, remark, null);
    }

    public static RemotingCommand createResponseCommand(int code, String remark,
                                                        Class<? extends CommandCustomHeader> classHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.markResponseType();
        cmd.setCode(code);
        cmd.setRemark(remark);
        setCmdVersion(cmd);

        if (classHeader != null) {
            try {
                // 通过反射创建类型
                CommandCustomHeader objectHeader = classHeader.getDeclaredConstructor().newInstance();
                cmd.customHeader = objectHeader;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            } catch (InvocationTargetException e) {
                return null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        return cmd;
    }

    public static RemotingCommand createResponseCommand(int code, String remark) {
        return createResponseCommand(code, remark, null);
    }

    public static RemotingCommand decode(final byte[] array) throws RemotingCommandException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        return decode(byteBuffer);
    }

    public static RemotingCommand decode(final ByteBuffer byteBuffer) throws RemotingCommandException {
        return decode(Unpooled.wrappedBuffer(byteBuffer)); // 非池化的 byteBuf ，无需release， 无需直接内存
    }

    /**
     * 这里进行解码，应该使用 unpooled ByteBuf 适合临时解码操作，不需要release 也不会自动托管到 direct Mem
     *
     * @param byteBuffer
     * @return
     * @throws RemotingCommandException
     */
    public static RemotingCommand decode(final ByteBuf byteBuffer) throws RemotingCommandException {
        // --- 这里进来的数据已经是 Netty 解码好的了

        // 1. 读取一下字节总长度
        int length = byteBuffer.readableBytes();
        // 2. 第一个 Int 是Header长度
        int oriHeaderLen = byteBuffer.readInt();
        // 3. 截取 Header 数据 -- 最长 16777215，负数处理
        int headerLength = getHeaderLength(oriHeaderLen);
        // 报文头长度大于ByteBuf的总长度 ， 这明显是有问题的。
        if (headerLength > length - 4) {
            throw new RemotingCommandException("decode error, bad header length: " + headerLength);
        }
        // 序列化-报文头部
        RemotingCommand cmd = headerDecode(byteBuffer, headerLength, getProtocolType(oriHeaderLen));
        // 报文长度
        int bodyLength = length - 4 - headerLength;
        byte[] bodyData = null;
        // 如果有报文体长度的话就进行读取
        if (bodyLength > 0) {
            bodyData = new byte[bodyLength];
            byteBuffer.readBytes(bodyData);
        }
        cmd.body = bodyData;
        // 这么说来还有不带有 cmdBody 的报文。
        return cmd;
    }

    public static int getHeaderLength(int length) {
        /**
         * | length（十进制） | 二进制（32 位）         | & `0xFFFFFF` 后二进制                 | 结果（十进制）  |
         * | ----------- | ----------------- | --------------------------------- | -------- |
         * | -1          | `1111…11111111`   | `0000 11111111 11111111 11111111` | 16777215 |
         * | -2          | `1111…11111110`   | `0000 11111111 11111111 11111110` | 16777214 |
         * | 5           | `0000…00000101`   | 相同                                | 5        |
         * | 0x01000000  | `0000 0001 …0000` | `0000 0000 …0000`                 | 0        |
         */
        return length & 0xFFFFFF;
    }

    private static RemotingCommand headerDecode(ByteBuf byteBuffer, int len,
                                                SerializeType type) throws RemotingCommandException {
        // 解析RPC头
        switch (type) {
            case JSON: // JSON 头
                byte[] headerData = new byte[len];
                byteBuffer.readBytes(headerData); // 读取 len 个。
                RemotingCommand resultJson = RemotingSerializable.decode(headerData, RemotingCommand.class); // JSON.parseObject(data, classOfT); fastjson
                resultJson.setSerializeTypeCurrentRPC(type);
                return resultJson;
            case ROCKETMQ:
                RemotingCommand resultRMQ = RocketMQSerializable.rocketMQProtocolDecode(byteBuffer, len);
                resultRMQ.setSerializeTypeCurrentRPC(type);
                return resultRMQ;
            default:
                break;
        }
        return null;
    }

    public static SerializeType getProtocolType(int source) {
        return SerializeType.valueOf((byte) ((source >> 24) & 0xFF));
    }

    public static int createNewRequestId() {
        return requestId.getAndIncrement();
    }

    public static SerializeType getSerializeTypeConfigInThisServer() {
        return serializeTypeConfigInThisServer;
    }

    public static int markProtocolType(int source, SerializeType type) {
        return (type.getCode() << 24) | (source & 0x00FFFFFF);
    }

    public void markResponseType() {
        int bits = 1 << RPC_TYPE;
        this.flag |= bits;
    }

    public CommandCustomHeader readCustomHeader() {
        return customHeader;
    }

    public void writeCustomHeader(CommandCustomHeader customHeader) {
        this.customHeader = customHeader;
    }

    public <T extends CommandCustomHeader> T decodeCommandCustomHeader(
            Class<T> classHeader) throws RemotingCommandException {
        return decodeCommandCustomHeader(classHeader, false);
    }

    public <T extends CommandCustomHeader> T decodeCommandCustomHeader(
            Class<T> classHeader, boolean isCached) throws RemotingCommandException {
        if (isCached && cachedHeader != null) {
            return classHeader.cast(cachedHeader);
        }
        cachedHeader = decodeCommandCustomHeaderDirectly(classHeader, true);
        if (cachedHeader == null) {
            return null;
        }
        return classHeader.cast(cachedHeader);
    }

    public <T extends CommandCustomHeader> T decodeCommandCustomHeaderDirectly(Class<T> classHeader,
                                                                               boolean useFastEncode) throws RemotingCommandException {
        T objectHeader;
        try {
            objectHeader = classHeader.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }

        if (this.extFields != null) {
            if (objectHeader instanceof FastCodesHeader && useFastEncode) {
                ((FastCodesHeader) objectHeader).decode(this.extFields);
                objectHeader.checkFields();
                return objectHeader;
            }

            Field[] fields = getClazzFields(classHeader);
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    String fieldName = field.getName();
                    if (!fieldName.startsWith("this")) {
                        try {
                            String value = this.extFields.get(fieldName);
                            if (null == value) {
                                if (!isFieldNullable(field)) {
                                    throw new RemotingCommandException("the custom field <" + fieldName + "> is null");
                                }
                                continue;
                            }

                            field.setAccessible(true);
                            String type = getCanonicalName(field.getType());
                            Object valueParsed;

                            if (type.equals(STRING_CANONICAL_NAME)) {
                                valueParsed = value;
                            } else if (type.equals(INTEGER_CANONICAL_NAME_1) || type.equals(INTEGER_CANONICAL_NAME_2)) {
                                valueParsed = Integer.parseInt(value);
                            } else if (type.equals(LONG_CANONICAL_NAME_1) || type.equals(LONG_CANONICAL_NAME_2)) {
                                valueParsed = Long.parseLong(value);
                            } else if (type.equals(BOOLEAN_CANONICAL_NAME_1) || type.equals(BOOLEAN_CANONICAL_NAME_2)) {
                                valueParsed = Boolean.parseBoolean(value);
                            } else if (type.equals(DOUBLE_CANONICAL_NAME_1) || type.equals(DOUBLE_CANONICAL_NAME_2)) {
                                valueParsed = Double.parseDouble(value);
                            } else if (type.equals(BOUNDARY_TYPE_CANONICAL_NAME)) {
                                valueParsed = BoundaryType.getType(value);
                            } else {
                                throw new RemotingCommandException("the custom field <" + fieldName + "> type is not supported");
                            }

                            field.set(objectHeader, valueParsed);

                        } catch (Throwable e) {
                            log.error("Failed field [{}] decoding", fieldName, e);
                        }
                    }
                }
            }

            objectHeader.checkFields();
        }

        return objectHeader;
    }

    //make it able to test
    Field[] getClazzFields(Class<? extends CommandCustomHeader> classHeader) {
        Field[] field = CLASS_HASH_MAP.get(classHeader);

        if (field == null) {
            Set<Field> fieldList = new HashSet<>();
            for (Class className = classHeader; className != Object.class; className = className.getSuperclass()) {
                Field[] fields = className.getDeclaredFields();
                fieldList.addAll(Arrays.asList(fields));
            }
            field = fieldList.toArray(new Field[0]);
            synchronized (CLASS_HASH_MAP) {
                CLASS_HASH_MAP.put(classHeader, field);
            }
        }
        return field;
    }

    private boolean isFieldNullable(Field field) {
        if (!NULLABLE_FIELD_CACHE.containsKey(field)) {
            Annotation annotation = field.getAnnotation(CFNotNull.class);
            synchronized (NULLABLE_FIELD_CACHE) {
                NULLABLE_FIELD_CACHE.put(field, annotation == null);
            }
        }
        return NULLABLE_FIELD_CACHE.get(field);
    }

    private String getCanonicalName(Class clazz) {
        String name = CANONICAL_NAME_CACHE.get(clazz);

        if (name == null) {
            name = clazz.getCanonicalName();
            synchronized (CANONICAL_NAME_CACHE) {
                CANONICAL_NAME_CACHE.put(clazz, name);
            }
        }
        return name;
    }

    public ByteBuffer encode() {
        // 1> header length size
        int length = 4;

        // 2> header data length
        byte[] headerData = this.headerEncode();
        length += headerData.length;

        // 3> body data length
        if (this.body != null) {
            length += body.length;
        }

        ByteBuffer result = ByteBuffer.allocate(4 + length);

        // length
        result.putInt(length);

        // header length
        result.putInt(markProtocolType(headerData.length, serializeTypeCurrentRPC));

        // header data
        result.put(headerData);

        // body data;
        if (this.body != null) {
            result.put(this.body);
        }

        result.flip();

        return result;
    }

    private byte[] headerEncode() {
        this.makeCustomHeaderToNet();
        if (SerializeType.ROCKETMQ == serializeTypeCurrentRPC) {
            return RocketMQSerializable.rocketMQProtocolEncode(this);
        } else {
            return RemotingSerializable.encode(this);
        }
    }

    public void makeCustomHeaderToNet() {
        if (this.customHeader != null) {
            Field[] fields = getClazzFields(customHeader.getClass());
            if (null == this.extFields) {
                this.extFields = new HashMap<>();
            }

            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    String name = field.getName();
                    if (!name.startsWith("this")) {
                        Object value = null;
                        try {
                            field.setAccessible(true);
                            value = field.get(this.customHeader);
                        } catch (Exception e) {
                            log.error("Failed to access field [{}]", name, e);
                        }

                        if (value != null) {
                            this.extFields.put(name, value.toString());
                        }
                    }
                }
            }
        }
    }

    public void fastEncodeHeader(ByteBuf out) {
        int bodySize = this.body != null ? this.body.length : 0;
        int beginIndex = out.writerIndex();
        // skip 8 bytes
        out.writeLong(0);
        int headerSize;
        if (SerializeType.ROCKETMQ == serializeTypeCurrentRPC) {
            if (customHeader != null && !(customHeader instanceof FastCodesHeader)) {
                this.makeCustomHeaderToNet();
            }
            headerSize = RocketMQSerializable.rocketMQProtocolEncode(this, out);
        } else {
            this.makeCustomHeaderToNet();
            byte[] header = RemotingSerializable.encode(this);
            headerSize = header.length;
            out.writeBytes(header);
        }
        out.setInt(beginIndex, 4 + headerSize + bodySize);
        out.setInt(beginIndex + 4, markProtocolType(headerSize, serializeTypeCurrentRPC));
    }

    public ByteBuffer encodeHeader() {
        return encodeHeader(this.body != null ? this.body.length : 0);
    }

    public ByteBuffer encodeHeader(final int bodyLength) {
        // 1> header length size
        int length = 4;

        // 2> header data length
        byte[] headerData;
        headerData = this.headerEncode();

        length += headerData.length;

        // 3> body data length
        length += bodyLength;

        ByteBuffer result = ByteBuffer.allocate(4 + length - bodyLength);

        // length
        result.putInt(length);

        // header length
        result.putInt(markProtocolType(headerData.length, serializeTypeCurrentRPC));

        // header data
        result.put(headerData);

        ((Buffer) result).flip();

        return result;
    }

    public void markOnewayRPC() {
        int bits = 1 << RPC_ONEWAY;
        this.flag |= bits;
    }

    @JSONField(serialize = false)
    public boolean isOnewayRPC() {
        int bits = 1 << RPC_ONEWAY;
        return (this.flag & bits) == bits;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @JSONField(serialize = false)
    public RemotingCommandType getType() {
        if (this.isResponseType()) {
            return RemotingCommandType.RESPONSE_COMMAND;
        }

        return RemotingCommandType.REQUEST_COMMAND;
    }

    @JSONField(serialize = false)
    public boolean isResponseType() {
        int bits = 1 << RPC_TYPE;
        return (this.flag & bits) == bits;
    }

    public LanguageCode getLanguage() {
        return language;
    }

    public void setLanguage(LanguageCode language) {
        this.language = language;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getOpaque() {
        return opaque;
    }

    public void setOpaque(int opaque) {
        this.opaque = opaque;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @JSONField(serialize = false)
    public boolean isSuspended() {
        return suspended;
    }

    @JSONField(serialize = false)
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public HashMap<String, String> getExtFields() {
        return extFields;
    }

    public void setExtFields(HashMap<String, String> extFields) {
        this.extFields = extFields;
    }

    public void addExtField(String key, String value) {
        if (null == extFields) {
            extFields = new HashMap<>(256);
        }
        extFields.put(key, value);
    }

    public void addExtFieldIfNotExist(String key, String value) {
        extFields.putIfAbsent(key, value);
    }

    @Override
    public String toString() {
        return "RemotingCommand [code=" + code + ", language=" + language + ", version=" + version + ", opaque=" + opaque + ", flag(B)="
                + Integer.toBinaryString(flag) + ", remark=" + remark + ", extFields=" + extFields + ", serializeTypeCurrentRPC="
                + serializeTypeCurrentRPC + "]";
    }

    public SerializeType getSerializeTypeCurrentRPC() {
        return serializeTypeCurrentRPC;
    }

    public void setSerializeTypeCurrentRPC(SerializeType serializeTypeCurrentRPC) {
        this.serializeTypeCurrentRPC = serializeTypeCurrentRPC;
    }

    public Stopwatch getProcessTimer() {
        return processTimer;
    }

    public void setProcessTimer(Stopwatch processTimer) {
        this.processTimer = processTimer;
    }

    public List<CommandCallback> getCallbackList() {
        return callbackList;
    }

    public void setCallbackList(List<CommandCallback> callbackList) {
        this.callbackList = callbackList;
    }
}
