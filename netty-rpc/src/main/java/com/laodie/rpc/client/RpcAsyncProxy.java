package com.laodie.rpc.client;

/**
 * @author laodie
 * @since 2020-08-09 6:04 下午
 **/
public interface RpcAsyncProxy {

    /**
     * 异步代理的接口
     *
     * @param funcName
     * @param args
     * @return
     */
    RpcFuture call(String funcName, Object... args);

}
