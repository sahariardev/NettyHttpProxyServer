package org.sahariardev.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.sahariardev.Deployment;
import org.sahariardev.Store;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String serverHost;

    public FrontendHandler(String serverHost) {
        this.serverHost = serverHost;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest requestFromBrowser) throws Exception {
        final Channel clientChannel = ctx.channel();

        String uri = requestFromBrowser.uri();
        Pattern pattern = Pattern.compile("^/([^/]+)(/.*)?");
        Matcher matcher = pattern.matcher(uri);

        String hostName = "";

        if (matcher.matches()) {
            hostName = matcher.group(1);
            uri = matcher.group(2);

            if (Objects.isNull(uri) || uri.isEmpty()) {
                uri = "/";
            }
        }

        final String host = hostName;

        List<Deployment> deployments = Store.getDeployments();

        Deployment deployment = deployments.stream()
                .filter(deployment1 -> deployment1.getName().equals(host))
                .findFirst().orElse(null);

        if (Objects.isNull(deployment)) {
            clientChannel
                    .writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                            HttpResponseStatus.FORBIDDEN))
                    .addListener(ChannelFutureListener.CLOSE);
            return;
        }

        HttpHeaders headers = requestFromBrowser.headers();

        if (headers.contains(HttpHeaderNames.HOST)) {
            String updatedHostName = "localhost:" + deployment.getPort();

            headers.set(HttpHeaderNames.HOST, updatedHostName);
        }

        FullHttpRequest modifiedRequest = new DefaultFullHttpRequest(requestFromBrowser.protocolVersion(), requestFromBrowser.method(), uri);
        modifiedRequest.headers().setAll(headers);
        modifiedRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, requestFromBrowser.content().readableBytes());

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

        b.connect(serverHost, deployment.port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel targetChannel = future.channel();
                targetChannel.writeAndFlush(modifiedRequest);
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
