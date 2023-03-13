package com.laodie.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author laodie
 * @since 2020-08-03 8:53 下午
 **/
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 1.把对应的java对象进行编码
     * 2.把编码后的的内容填充到buff中
     * 3.写出到server端
     *
     * @param channelHandlerContext
     * @param msg
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf byteBuf) throws Exception {
        if (genericClass.isInstance(msg)) {
            byte[] data = Serialization.serialize(msg);
            //消息分为 1.包头:数据包长度 2.包体：数据包内容
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
        }
    }
}
