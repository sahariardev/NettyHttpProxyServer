package org.sahariardev.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class ProxyBackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Channel clienChannel;

    public ProxyBackendHandler(Channel clienChannel) {
        this.clienChannel = clienChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse fullHttpResponse) throws Exception {
        FullHttpResponse response = fullHttpResponse.retainedDuplicate();

        ByteBuf buf = response.content().retain();
        String content = buf.toString(CharsetUtil.UTF_8);
        String modifiedContentStr = content.replaceAll("action\\s*=\\s*\"([^\"]*)\"", "action=\"/client$1\"");
        ByteBuf modifiedBuffer = Unpooled.copiedBuffer(modifiedContentStr, CharsetUtil.UTF_8);

        FullHttpResponse modifiedResponse = new DefaultFullHttpResponse(response.protocolVersion(), response.status(), modifiedBuffer);
        modifiedResponse.headers().setAll(response.headers());
        modifiedResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, modifiedBuffer.readableBytes());

        clienChannel.writeAndFlush(modifiedResponse);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        clienChannel.close();
    }
}
