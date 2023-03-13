package com.laodie.rpc.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * @author laodie
 * @since 2020-08-03 8:37 下午
 **/
@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = -9194493992624088314L;
    /**
     * 请求id
     */
    private String requestId;

    /**
     * 返回结果
     */
    private Object result;

    /**
     * 异常
     */
    private Throwable throwable;
}
