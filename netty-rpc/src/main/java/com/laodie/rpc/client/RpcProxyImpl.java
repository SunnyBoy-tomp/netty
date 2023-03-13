package com.laodie.rpc.client;


import com.laodie.rpc.codec.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author laodie
 * @since 2020-08-08 9:55 下午
 **/
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

    private final Class<T> clazz;

    private final long timeout;

    public RpcProxyImpl(Class<T> clazz, long timeout) {
        this.clazz = clazz;
        this.timeout = timeout;
    }

    /**
     * 代理接口调用
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1.设置请求对象
        RpcRequest request = RpcRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .className(method.getDeclaringClass().getName())
            .methodName(method.getName())
            .parameterTypes(method.getParameterTypes())
            .parameters(args).build();
        //2.选择一个合适的client任务处理器
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        //3.发送真正客户端请求 返回请求
        RpcFuture future = handler.sendRequest(request);
        return future.get(timeout, TimeUnit.SECONDS);
    }

    /**
     * 异步代理的接口实现
     * 抛出rpcFuture给业务方做回调处理
     *
     * @param funcName
     * @param args
     * @return
     */
    @Override
    public RpcFuture call(String funcName, Object... args) {
        Class<?>[] parameterTypes = null;
        Class<?>[] argsTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argsTypes[i] = args[i].getClass();
        }
        for (Method method : this.clazz.getDeclaredMethods()) {
            if (method.getName().equals(funcName)) {
                Class<?>[] methodParameterTypes = method.getParameterTypes();
                //比较两个数据一致
                if (Arrays.equals(methodParameterTypes,argsTypes)) {
                    parameterTypes = method.getParameterTypes();
                    break;
                }
            }
        }
        if (parameterTypes == null) {
            throw new RuntimeException("no method find,funcName = " + funcName);
        }
        //1.设置请求对象
        RpcRequest request = RpcRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .className(this.clazz.getName())
            .methodName(funcName)
            .parameterTypes(parameterTypes)
            .parameters(args).build();
        //2.选择一个合适的client任务处理器
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        //3.返回future对象
        return handler.sendRequest(request);
    }
}
