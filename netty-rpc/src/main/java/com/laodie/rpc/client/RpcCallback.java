package com.laodie.rpc.client;

/**
 * @author laodie
 * @since 2020-08-08 8:54 下午
 **/
public interface RpcCallback {

    void success(Object result);

    void failure(Throwable cause);
}
