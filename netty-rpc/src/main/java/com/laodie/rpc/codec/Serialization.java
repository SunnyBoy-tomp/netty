package com.laodie.rpc.codec;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.experimental.UtilityClass;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author laodie
 * @since 2020-08-03 9:01 下午
 **/
@UtilityClass
public class Serialization {

    private final Map<Class<?>, Schema<?>> CACHED_SCHEMA = new ConcurrentHashMap<>();

    private final Objenesis OBJENESIS = new ObjenesisStd(true);

    private <T> Schema<T> getSchema(Class<T> cls) {
        @SuppressWarnings("unchecked")
        Schema<T> schema = (Schema<T>)CACHED_SCHEMA.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                CACHED_SCHEMA.put(cls, schema);
            }
        }
        return schema;
    }

    /**
     * 序列化：对象->字节数组
     */
    public <T> byte[] serialize(T obj) {
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>)obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组->对象）
     */
    public <T> T deserialize(byte[] data, Class<T> cls) {
        try {
            T message = OBJENESIS.newInstance(cls);
            Schema<T> schema = getSchema(cls);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
