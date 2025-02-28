package com.exchange.client;

import com.exchange.NewOrderEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.charset.StandardCharsets;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ExchangeClient {

//    private static final Logger logger = LoggerFactory.getLogger(ExchangeClient.class);

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
//                            pipeline.addLast(new LengthFieldPrepender(4));
//                            pipeline.addLast(new ClientHandler());
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            // Connect to server
            ChannelFuture future = bootstrap.connect("localhost", 8080).sync();
            future.channel().closeFuture().sync();
//            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
//            logger.info("Connected to the exchange server.");

//            sendNewOrder(channel);
//
//            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void sendNewOrder(Channel channel) {
        // Create NewOrder message
        NewOrderEncoder newOrderEncoder = new NewOrderEncoder();
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);
        newOrderEncoder.wrap(buffer, 0)
                .orderId(12345)
                .symbol("AAPL")
                .side((short) 0) // buy
                .quantity(100)
                .price(15000); // price in cents

        // Send message to server
        ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(buffer.byteArray(), 0, newOrderEncoder.encodedLength());
        channel.writeAndFlush(byteBuf);
//        logger.info("[Client] Sent NewOrder message to the server.");
        System.out.println("[Client] Sent NewOrder message to the server.");
    }

//    static class ClientHandler extends ChannelInboundHandlerAdapter {
//        @Override
//        public void channelActive(ChannelHandlerContext ctx) {
//            ByteBuf message = Unpooled.copiedBuffer("Hello, Netty!", StandardCharsets.UTF_8);
//            ctx.writeAndFlush(message);
//        }
//
//        @Override
//        public void channelRead(ChannelHandlerContext ctx, Object msg) {
//            ByteBuf byteBuf = (ByteBuf) msg;
//            System.out.println("Received from server: " + byteBuf.toString(StandardCharsets.UTF_8));
//            byteBuf.release();
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//            cause.printStackTrace();
//            ctx.close();
//        }
//    }
}
