package com.laodie.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author laodie
 * @since 2020-08-03 8:53 下午
 **/
public class RpcDecoder extends ByteToMessageDecoder {

    private final Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list)
        throws Exception {
        // 请求数据包不足4个字节直接返回
        /**
         * 消息头
         */
        int head = 4;
        if (byteBuf.readableBytes() < head) {
            return;
        }
        //记录一下当前位置
        byteBuf.markReaderIndex();
        //获取请求包数据长度
        int dataLength = byteBuf.readInt();
        if (byteBuf.readableBytes() < dataLength) {
            byteBuf.resetReaderIndex();
            return;
        }
        //真正读取需要长度的数据包内容
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);

        //解码操作 返回指定对象
        Object obj = Serialization.deserialize(data, genericClass);

        //填充到buffer中，传播给下游的handler执行实际处理
        list.add(obj);

    }
}
