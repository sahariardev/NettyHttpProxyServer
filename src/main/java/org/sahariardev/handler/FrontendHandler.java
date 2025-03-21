package org.sahariardev.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

public class FrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String serverHost;

    private final int serverPort;

    public FrontendHandler(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest requestFromBrowser) throws Exception {
        final Channel clientChannel = ctx.channel();

        FullHttpRequest copiedRequest = requestFromBrowser.retainedDuplicate();
        copiedRequest.setUri(copiedRequest.uri().replace("/client", ""));

        Bootstrap b = new Bootstrap();
        b.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                        p.addLast(new ProxyBackendHandler(clientChannel));
                    }
                });
        b.connect(serverHost, serverPort).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel targetChannel = future.channel();
                targetChannel.writeAndFlush(copiedRequest);
            } else {
                clientChannel
                        .writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.BAD_GATEWAY))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
