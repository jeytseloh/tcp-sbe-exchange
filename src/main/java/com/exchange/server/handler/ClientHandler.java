package com.exchange.server.handler;

import com.exchange.NewOrderDecoder;
import com.exchange.OrderAcknowledgedEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.agrona.concurrent.UnsafeBuffer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ClientHandler extends ChannelInboundHandlerAdapter {

//    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[1024]); // reusable buffer

    private final NewOrderDecoder newOrderDecoder = new NewOrderDecoder();

    private final OrderAcknowledgedEncoder orderAcknowledgedEncoder = new OrderAcknowledgedEncoder();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        logger.info("[Server] Client connected: {}", ctx.channel().remoteAddress());
        System.out.println("[Server] Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg);
//        ByteBuf byteBuf = (ByteBuf) msg;
//        try {
//            buffer.wrap(byteBuf.nioBuffer());
//
//            // Decode NewOrder message
//            newOrderDecoder.wrap(buffer, 0, NewOrderDecoder.BLOCK_LENGTH, NewOrderDecoder.SCHEMA_VERSION);
//
//            // Extract fields from NewOrder message
//            long orderId = newOrderDecoder.orderId();
//            String symbol = newOrderDecoder.symbol();
//            short side = newOrderDecoder.side();
//            long quantity = newOrderDecoder.quantity();
//            long price = newOrderDecoder.price();
//
//            logger.info("[Server] Received NewOrder: orderId={}, symbol={}, side={}, quantity={}, price={}",
//                    orderId, symbol, side, quantity, price);
//
//            boolean orderAccepted = processOrder(newOrderDecoder);
//
//            // Encode and send OrderAcknowledged message
//            ByteBuf responseBuf = ctx.alloc().buffer();
//            UnsafeBuffer responseBuffer = new UnsafeBuffer(responseBuf.nioBuffer());
//            orderAcknowledgedEncoder.wrap(responseBuffer, 0)
//                    .orderId(orderId)
//                    .status(orderAccepted ? (short) 1 : (short) 0); // Accepted = 1, Rejected = 0
//
//            // Write the response back to the client
//            ctx.writeAndFlush(responseBuf);
//
//        } finally {
//            byteBuf.release(); // release to avoid memory leaks
//        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
//        logger.info("[Server] Client disconnected: {}", ctx.channel().remoteAddress());
        System.out.println("[Server] Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        logger.error("[Server] Exception occurred: {}", cause.getMessage(), cause);
        System.out.println("[Server] Exception occurred: " + cause.getMessage());
        ctx.close();
    }

    private boolean processOrder(NewOrderDecoder newOrderDecoder) {
        // add processing logic
        return true;
    }
}
