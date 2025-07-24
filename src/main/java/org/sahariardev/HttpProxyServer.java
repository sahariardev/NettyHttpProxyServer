package org.sahariardev;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.sahariardev.handler.FrontendHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Arrays;

public class HttpProxyServer {

    public void start(int port, String serverHost, int serverPort) throws InterruptedException, SSLException {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        String certRoot = "/home/sahariaralam/apps/aggregatorDocker/cert/";
        File cert = new File(certRoot + "star_therapdev_net_with_chain.crt");
        File key = new File(certRoot + "star_therapdev_net.key");

        SslContext sslCtx = SslContextBuilder.forServer(cert, key)
                .protocols("TLSv1", "TLSv1.1", "TLSv1.2")  // or just TLSv1.2, TLSv1.3 recommended
                .ciphers(Arrays.asList(
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
                ))
                .build();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .localAddress(port)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));

                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(512 * 1024));
                            pipeline.addLast(new FrontendHandler(serverHost, serverPort));
                        }
                    });

            ChannelFuture future = serverBootstrap.bind().sync();
            System.out.println("Server started on port " + port);
            future.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
        }
    }

}
