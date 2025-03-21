package org.sahariardev.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

public class ProxyBackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Channel clienChannel;

    public ProxyBackendHandler(Channel clienChannel) {
        this.clienChannel = clienChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse fullHttpResponse) throws Exception {
        clienChannel.writeAndFlush(fullHttpResponse.retainedDuplicate());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        clienChannel.close();
    }
}
