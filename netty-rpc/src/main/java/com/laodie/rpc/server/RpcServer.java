package com.laodie.rpc.server;

import com.laodie.rpc.codec.RpcDecoder;
import com.laodie.rpc.codec.RpcEncoder;
import com.laodie.rpc.codec.RpcRequest;
import com.laodie.rpc.codec.RpcResponse;
import com.laodie.rpc.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author laodie
 * @since 2020-08-03 10:50 下午
 **/
@Slf4j
public class RpcServer {

    private final String serverAddress;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workGroup = new NioEventLoopGroup();

    /**
     * key is interface name
     * value 具体实现类
     */
    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        this.start();
    }

    private void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            //tcp = sync + accept = backlog
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline()
                        //编解码
                        .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4))
                        .addLast(new RpcDecoder(RpcRequest.class))
                        .addLast(new RpcEncoder(RpcResponse.class))
                        //实际处理器
                        .addLast(new RpcServerHandler(handlerMap));
                }
            });
        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);
        ChannelFuture channelFuture = bootstrap.bind(host, port).sync();
        channelFuture.addListener((ChannelFutureListener)future -> {
            if (future.isSuccess()) {
                log.info("server success bind to {}", serverAddress);
            } else {
                log.info("server failure bind to {}", serverAddress);
                throw new Exception("server start fail", future.cause());
            }
        });
    }

    /**
     * 程序注册器
     *
     * @param config
     */
    public void registerProcessor(ProviderConfig config) {
        //key : providerConfig.interface (比如：userService接口全命名)
        //value ： providerConfig.ref （userService接口下具体实现类userServiceImpl实例对象）
        handlerMap.put(config.getInterfaceClass(), config.getRef());
    }

    /**
     * 关闭连接
     */
    public void close() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }
}
