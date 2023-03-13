package com.laodie.rpc.client;

import com.laodie.rpc.codec.RpcDecoder;
import com.laodie.rpc.codec.RpcEncoder;
import com.laodie.rpc.codec.RpcRequest;
import com.laodie.rpc.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author laodie
 * @since 2020-07-29 11:02 下午
 **/
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
            //编解码
            .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4))
            .addLast(new RpcEncoder(RpcRequest.class))
            .addLast(new RpcDecoder(RpcResponse.class))
            //实际处理器
            .addLast(new RpcClientHandler());
    }
}
