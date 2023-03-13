package com.laodie.rpc.server;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.laodie.rpc.codec.RpcRequest;
import com.laodie.rpc.codec.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author laodie
 * @since 2020-08-03 10:57 下午
 **/
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private final int CPU_NUMBER = Runtime.getRuntime().availableProcessors();

    private final ExecutorService executor =
        new ThreadPoolExecutor(CPU_NUMBER, 2 * CPU_NUMBER, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536),
            new ThreadFactoryBuilder().setNamePrefix("rpc-server-executor").build());

    private final Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        RpcResponse response = new RpcResponse();
        executor.submit(() -> {
            response.setRequestId(rpcRequest.getRequestId());
            try {
                response.setResult(handle(rpcRequest));
                System.out.println(response.getResult());
            } catch (Throwable t) {
                response.setThrowable(t);
                t.printStackTrace();
                log.error("rpc service handle request error:" + t);
            }
            //解析request对象
            //从handlerMap中找到具体的接口（key）所绑定的实例（bean）
            //反射或者cglib调用 具体方法 传递相关执行参数进行执行
            //返回响应结果给调用方
            channelHandlerContext.writeAndFlush(response).addListener((ChannelFutureListener)future -> {
                if (future.isSuccess()) {
                    //这里可以定义后置处理器
                    log.info("rpc调用成功");
                }
            });
        });
    }

    /**
     * 解析request请求并且通过JDK反射或者Cglib获取具体本地服务实例后执行具体方法
     *
     * @param rpcRequest
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object handle(RpcRequest rpcRequest)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String className = rpcRequest.getClassName();
        Object serviceRef = handlerMap.get(className);
        Class<?> serviceClass = serviceRef.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();
        //JDK 反射
        //Method method = serviceClass.getMethod(methodName, parameterTypes);
        //return method.invoke(serviceRef, parameters);
        //Cglib
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod method = serviceFastClass.getMethod(methodName, parameterTypes);
        return method.invoke(serviceRef, parameters);
    }

    /**
     * 异常处理 关闭连接
     *
     * @param ctx
     * @param cause
     */
    public void exceptionCauht(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server caught Throwable: " + cause);
        ctx.close();
    }
}
