package org.sahariardev.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String uri = copiedRequest.uri();
        Pattern pattern = Pattern.compile("^/([^/]+)(/.*)?");
        Matcher matcher = pattern.matcher(uri);

        String hostName = "";

        if (matcher.matches()) {
            hostName = matcher.group(1);
            uri = matcher.group(2);

            if (Objects.isNull(uri) || uri.length() == 0) {
                uri = "/";
            }
        }

        final String host = hostName;
        copiedRequest.setUri(uri);

        if (!host.equals("test")) {
            clientChannel
                    .writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.FORBIDDEN))
                    .addListener(ChannelFutureListener.CLOSE);
            return;
        }

        Bootstrap b = new Bootstrap();
        b.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(512 * 1024));
                        p.addLast(new ProxyBackendHandler(clientChannel, host));
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
