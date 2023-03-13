package com.laodie.rpc.client;

import com.laodie.rpc.codec.RpcRequest;
import com.laodie.rpc.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author laodie
 * @since 2020-07-29 10:46 下午
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Channel channel;

    private SocketAddress remotePeer;

    /**
     * key: requestId
     * value: RpcFuture
     */
    private Map<String, RpcFuture> pendingRpcTable = new ConcurrentHashMap<>();

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = channel.remoteAddress();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        RpcFuture rpcFuture = pendingRpcTable.get(requestId);
        if (rpcFuture != null) {
            pendingRpcTable.remove(requestId);
            rpcFuture.done(response);
        }
    }

    /**
     * Netty提供发送空buf添加监听关闭连接
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 异步发送请求方法
     *
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture future = new RpcFuture(request);
        pendingRpcTable.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future;
    }
}
