package com.exchange.client.handler;

import com.exchange.OrderAcknowledgedDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.agrona.concurrent.UnsafeBuffer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {

//    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]);

    private final OrderAcknowledgedDecoder orderAcknowledgedDecoder = new OrderAcknowledgedDecoder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
//        logger.info("Received from server: {}", byteBuf.toString(StandardCharsets.UTF_8));
        System.out.println("[Client] Received from server: " + byteBuf.toString(StandardCharsets.UTF_8));
        byteBuf.release();
//        ByteBuf byteBuf = (ByteBuf) msg;
//        try {
//            buffer.wrap(byteBuf.nioBuffer());
//            orderAcknowledgedDecoder.wrap(buffer, 0, OrderAcknowledgedDecoder.BLOCK_LENGTH, OrderAcknowledgedDecoder.SCHEMA_VERSION);
//
//            long orderId = orderAcknowledgedDecoder.orderId();
//            short status = orderAcknowledgedDecoder.status();
//
//            logger.info("[Client] Received OrderAcknowledged: orderId={}, status={}", orderId, status);
//        } finally {
//            byteBuf.release();
//        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ByteBuf message = Unpooled.copiedBuffer("Hello world", StandardCharsets.UTF_8);
        ctx.writeAndFlush(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        logger.error("[Client] Exception occurred: {}", cause.getMessage(), cause);
        System.out.println("[Client] Exception occurred: " + cause.getMessage());
        ctx.close();
    }
}
