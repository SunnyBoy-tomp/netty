package com.laodie.rpc.consumer;

import com.laodie.rpc.client.RpcAsyncProxy;
import com.laodie.rpc.client.RpcClient;
import com.laodie.rpc.client.RpcFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;



/**
 * @author laodie
 * @since 2020-08-09 10:46 下午
 **/
@Slf4j
public class ConsumerStarter {

    private static void sync() {
        RpcClient client = new RpcClient("127.0.0.1:8766", 3000);
        HelloService helloService = client.invokeSync(HelloService.class);
        String hello = helloService.hello("张三");
        log.info(hello);
    }

    private static void async() throws ExecutionException, InterruptedException {
        RpcClient client = new RpcClient("127.0.0.1:8766", 3000);
        RpcAsyncProxy rpcAsyncProxy = client.invokeAsync(HelloService.class);
        User user = new User();
        user.setId(100L);
        user.setName("李四");
        RpcFuture hello = rpcAsyncProxy.call("hello", user);
        log.info(hello.get().toString());
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        long l = System.currentTimeMillis();
        sync();
        long l1 = System.currentTimeMillis();
        System.out.println(l1-l);
        long ll = System.currentTimeMillis();
        async();
        long ll1 = System.currentTimeMillis();
        System.out.println(ll1-ll);
    }

}
