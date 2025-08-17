package org.sahariardev.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class ProxyBackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Channel clientChannel;
    private final String hostName;

    public ProxyBackendHandler(Channel clientChannel, String hostName) {
        this.clientChannel = clientChannel;
        this.hostName = hostName;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msgFromServer) throws Exception {
        FullHttpResponse response = msgFromServer.retainedDuplicate();
        ByteBuf buf = response.content().retain();
        String content = buf.toString(CharsetUtil.UTF_8);

        String contentType = msgFromServer.headers().get(HttpHeaderNames.CONTENT_TYPE);

        String modifiedContentStr = getContent(content, contentType);

        ByteBuf modifiedBuffer = Unpooled.copiedBuffer(modifiedContentStr, CharsetUtil.UTF_8);

        HttpHeaders headers = response.headers();

        if (headers.contains(HttpHeaderNames.SET_COOKIE)) {
            List<String> cookies = headers.getAll(HttpHeaderNames.SET_COOKIE);
            headers.remove(HttpHeaderNames.SET_COOKIE);

            for (String cookieStr : cookies) {
                headers.add(HttpHeaderNames.SET_COOKIE, cookieStr.replaceAll("(?<=Path=)[^;]+", "/"));
            }
        }

        if (headers.contains(HttpHeaderNames.LOCATION)) {
            String updatedLocation = getRelativeUrl(headers.get(HttpHeaderNames.LOCATION));

            headers.set(HttpHeaderNames.LOCATION, updatedLocation);
        }

        if (headers.get(HttpHeaderNames.LOCATION) == null) {
            for (Map.Entry<String, String> header : msgFromServer.headers()) {
                System.out.println(header.getKey() + ": " + header.getValue());
            }
        }

        FullHttpResponse modifiedResponse = new DefaultFullHttpResponse(response.protocolVersion(), response.status(),
                contentType != null && contentType.startsWith("text/html") ? modifiedBuffer : response.content());

        modifiedResponse.headers().clear();

        for (Map.Entry<String, String> header : headers.entries()) {
            modifiedResponse.headers().add(header.getKey(), header.getValue());
        }

        modifiedResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, modifiedBuffer.readableBytes());

        clientChannel.writeAndFlush(modifiedResponse.retainedDuplicate());
        ctx.close();
    }

    private String getContent(String content, String contentType) {
        String modifiedContentStr = content;
        if (contentType != null && contentType.startsWith("text/html")) {
            String ajaxPrefilter = "<script>\n" +
                    "            $.ajaxPrefilter(function (options, originalOptions, jqXHR) {\n" +
                    "                const prefix = '/" + hostName + "';\n" +
                    "                if (!/^https?:\\/\\//i.test(options.url)) {\n" +
                    "                    options.url = prefix + options.url;\n" +
                    "                }\n" +
                    "            });\n" +
                    "        </script>\n";

            modifiedContentStr = content.replaceAll(
                    "(?i)(href|src|action|window.location.href|loginUrl)\\s*=\\s*(['\"])(/.*?)\\2",
                    "$1=$2/" + hostName + "$3$2"
            );

            modifiedContentStr = modifiedContentStr.replace("</body>", ajaxPrefilter + "</body>");

        }
        return modifiedContentStr;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        clientChannel.close();
    }

    private String getRelativeUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);

        String stripped = uri.getRawPath();

        if (uri.getRawQuery() != null) {
            stripped += "?" + uri.getRawQuery();
        }

        if (uri.getRawFragment() != null) {
            stripped += "#" + uri.getRawFragment();
        }

        return "/" + hostName + stripped;
    }
}
