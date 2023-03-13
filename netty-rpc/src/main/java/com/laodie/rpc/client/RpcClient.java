package com.laodie.rpc.client;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author laodie
 * @since 2020-08-03 8:29 下午
 **/
public class RpcClient {
    /**
     * 连接地址
     */
    private final String serverAddress;
    /**
     * 超时时间
     */
    private final long timeout;

    private final Map<Class<?>, Object> syncProxyInstanceMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> asyncProxyInstanceMap = new ConcurrentHashMap<>();

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    public void stop() {
        RpcConnectManager.getInstance().stop();
    }

    /**
     * 同步调用方法
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeSync(Class<T> interfaceClass) {
        if (syncProxyInstanceMap.containsKey(interfaceClass)) {
            return (T)syncProxyInstanceMap.get(interfaceClass);
        }
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader()
            , new Class<?>[] {interfaceClass}, new RpcProxyImpl<>(interfaceClass, timeout));
        syncProxyInstanceMap.put(interfaceClass, proxy);
        return (T)proxy;
    }

    /**
     * 异步调用方法
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
        if (asyncProxyInstanceMap.containsKey(interfaceClass)) {
            return (RpcAsyncProxy)asyncProxyInstanceMap.get(interfaceClass);
        }
        RpcProxyImpl<T> asyncProxyImpl = new RpcProxyImpl<>(interfaceClass, timeout);
        asyncProxyInstanceMap.put(interfaceClass, asyncProxyImpl);
        return asyncProxyImpl;
    }
}
