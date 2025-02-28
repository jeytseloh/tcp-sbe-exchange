package com.exchange.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.CharsetUtil;

public class ExchangeServer {

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
//                            pipeline.addLast(new ClientHandler());
                            pipeline.addLast(new ServerHandler());
                        }
                    });
            // Start the server
//            bootstrap.bind(8080).sync().channel().closeFuture().sync();
            ChannelFuture future = bootstrap.bind(8080).sync();
            future.channel().closeFuture().sync();
            System.out.println("Server started at port: 8080");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

//    static class ServerHandler extends ChannelInboundHandlerAdapter {
//        @Override
//        public void channelRead(ChannelHandlerContext ctx, Object msg) {
//            ByteBuf byteBuf = (ByteBuf) msg;
//            String received = byteBuf.toString(CharsetUtil.UTF_8);
//            System.out.println("Server received: " + received);
//            ctx.write(msg);
//        }
//
//        @Override
//        public void channelReadComplete(ChannelHandlerContext ctx) {
//            ctx.flush();
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//            cause.printStackTrace();
//            ctx.close();
//        }
//    }
}
