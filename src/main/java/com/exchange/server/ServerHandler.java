package com.exchange.server;

import com.exchange.*;
import com.exchange.order_matching_engine.Order;
import com.exchange.order_matching_engine.OrderBook;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final NewOrderDecoder orderDecoder = new NewOrderDecoder();
    private final OrderAcknowledgedEncoder acknowledgedEncoder = new OrderAcknowledgedEncoder();
    private final OrderFilledEncoder filledEncoder = new OrderFilledEncoder();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final OrderBook orderBook = new OrderBook();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        ByteBuf byteBuf = (ByteBuf) msg;
//        String received = byteBuf.toString(CharsetUtil.UTF_8);
//        System.out.println("Server received: " + received);
//        ctx.write(msg);

//        NewOrderDecoder newOrder = (NewOrderDecoder) msg;
//        OrderAcknowledgedEncoder orderAck = new OrderAcknowledgedEncoder();
//
//        ctx.write(orderAck);
//
//        // Schedule OrderFilled message after 2 seconds
//        ctx.channel().eventLoop().schedule(() -> {
//            OrderFilledEncoder orderFilled = new OrderFilledEncoder();
//            ctx.writeAndFlush(orderFilled);
//        }, 2, TimeUnit.SECONDS);
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        decodeNewOrder(ctx, bytes);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void decodeNewOrder(ChannelHandlerContext ctx, byte[] data) {
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.wrap(data));

        int offset = 0;
        headerDecoder.wrap(buffer, offset);
        offset += headerDecoder.encodedLength();
        orderDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

        long orderId = orderDecoder.orderId();
        short side = orderDecoder.side();
        long quantity = orderDecoder.quantity();
        long price = orderDecoder.price();
        byte[] symbolBytes = new byte[4];
        orderDecoder.getSymbol(symbolBytes,0);
        String symbol = new String(symbolBytes).trim();

        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Received Order: orderId=" + orderId + ", symbol=" + symbol + ", side=" + side +
                ", quantity=" + quantity + ", price=" + price);

        boolean isFilled = orderBook.addOrder(new Order(orderId, symbol, side, quantity, price));
        // might need to separate add order and match order to be independent of each other so that acknowledge and filled can be sent correctly
//        orderBook.printOrderBook();

        // Send acknowledgement
        byte [] acknowledgedMsg = encodeOrderAcknowledged(orderId, (byte) 1);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(acknowledgedMsg));

        // Send order filled
        if (isFilled) {

        }

//        // Schedule order fulfilled after 5 seconds (temporary)
//        scheduler.schedule(() -> {
//            byte[] filledMsg = encodeOrderFilled(orderId, quantity, price);
//            ctx.writeAndFlush(Unpooled.wrappedBuffer(filledMsg));
//            System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Order Filled: orderId=" + orderId + ", filledQuantity=" + quantity + ", filledPrice=" + price);
//        }, 5, TimeUnit.SECONDS);
    }

    private byte [] encodeOrderAcknowledged(long orderId, byte status) {
        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(128);
        int offset = 0;
        headerEncoder.wrap(buffer, offset)
                .blockLength(acknowledgedEncoder.sbeBlockLength())
                .templateId(acknowledgedEncoder.sbeTemplateId())
                .schemaId(acknowledgedEncoder.sbeSchemaId())
                .version(acknowledgedEncoder.sbeSchemaVersion());
        offset += headerEncoder.encodedLength();
        acknowledgedEncoder.wrap(buffer, offset)
                .orderId(orderId)
                .status(status);

        byte[] encodedMsg = new byte[offset + acknowledgedEncoder.encodedLength()];
        buffer.getBytes(0, encodedMsg);
        return encodedMsg;
    }

    private byte[] encodeOrderFilled(long orderId, long filledQty, long filledPrice) {
        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(128);
        int offset = 0;
        headerEncoder.wrap(buffer, offset)
                .blockLength(filledEncoder.sbeBlockLength())
                .templateId(filledEncoder.sbeTemplateId())
                .schemaId(filledEncoder.sbeSchemaId())
                .version(filledEncoder.sbeSchemaVersion());
        offset += headerEncoder.encodedLength();
        filledEncoder.wrap(buffer, offset)
                .orderId(orderId)
                .filledQuantity(filledQty)
                .filledPrice(filledPrice);

        byte[] encodedMsg = new byte[offset + filledEncoder.encodedLength()];
        buffer.getBytes(0, encodedMsg);
        return encodedMsg;
    }
}
