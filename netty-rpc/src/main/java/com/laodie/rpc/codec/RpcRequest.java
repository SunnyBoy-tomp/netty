package com.laodie.rpc.codec;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author laodie
 * @since 2020-08-03 8:37 下午
 **/
@Data
@Builder
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -6939371621159571899L;

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;

    /**
     * 参数数组
     */
    private Object[] parameters;
}
