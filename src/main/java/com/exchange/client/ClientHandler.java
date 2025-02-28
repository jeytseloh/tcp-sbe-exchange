package com.exchange.client;

import com.exchange.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final Scanner scanner = new Scanner(System.in);
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final NewOrderEncoder orderEncoder = new NewOrderEncoder();
    private final OrderAcknowledgedDecoder acknowledgedDecoder = new OrderAcknowledgedDecoder();
    private final OrderFilledDecoder filledDecoder = new OrderFilledDecoder();
    private ChannelHandlerContext ctx;


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        // Send string message
//        ByteBuf message = Unpooled.copiedBuffer("Hello, Netty!", StandardCharsets.UTF_8);
//        ctx.writeAndFlush(message);

//        // Allocate memory and an unsafe buffer to act as destination for data
//        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
//        MutableDirectBuffer directBuffer = new UnsafeBuffer(byteBuffer);
//
//        int messageOffset = headerEncoder.encodedLength();
//
//        headerEncoder.wrap(directBuffer, 0)
//                .blockLength(orderEncoder.sbeBlockLength())
//                .templateId(orderEncoder.sbeTemplateId())
//                .schemaId(orderEncoder.sbeSchemaId())
//                .version(orderEncoder.sbeSchemaVersion());
//
//        orderEncoder.wrap(directBuffer, messageOffset)
//                .orderId(1)
//                .price(100)
//                .quantity(5);

//        ByteBuf buffer = Unpooled.wrappedBuffer(byteBuffer, 0, messageOffset + orderEncoder.encodedLength());
//        System.out.println("Client sending: new order [ID=1, Price=100, Quantity=5]");

//        byte[] message = encodeNewOrder(12345L, "AAPL", (byte) 0, 100, 15000);
//        ctx.writeAndFlush(Unpooled.wrappedBuffer(message));
//
//        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Sent Order: orderId=12345, symbol=AAPL, side=0, quantity=100, price=15000");
////        ctx.writeAndFlush(buffer);
        this.ctx = ctx;
        new Thread(this::readOrders).start(); // run input reading in separate thread
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] bytes = new byte[byteBuf.readableBytes()];
//        System.out.println("Received from server: " + byteBuf.toString(StandardCharsets.UTF_8));
//        byteBuf.release();
        byteBuf.readBytes(bytes);
        decodeMessage(bytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public byte[] encodeNewOrder(long orderId, String symbol, byte side, long quantity, long price) {
        ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(128);
        int offset = 0;
        headerEncoder.wrap(buffer, offset)
                .blockLength(orderEncoder.sbeBlockLength())
                .templateId(orderEncoder.sbeTemplateId())
                .schemaId(orderEncoder.sbeSchemaId())
                .version(orderEncoder.sbeSchemaVersion());
        offset += headerEncoder.encodedLength();
        orderEncoder.wrap(buffer, offset)
                .orderId(orderId)
                .symbol(symbol)
                .side(side)
                .quantity((int) quantity)
                .price(price);
        // Encoding symbol into fixed-length ASCII string
        orderEncoder.putSymbol(symbol.getBytes(), 0);

        byte[] encodedMessage = new byte[offset + orderEncoder.encodedLength()];
        buffer.getBytes(0, encodedMessage);
        return encodedMessage;
    }

    private void decodeMessage(byte[] data) {
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.wrap(data));
        int offset = 0;
        headerDecoder.wrap(buffer, offset);
        int templateId = headerDecoder.templateId();
        offset += headerDecoder.encodedLength();

        switch (templateId) {
            case OrderAcknowledgedDecoder.TEMPLATE_ID:
                decodeOrderAcknowledged(buffer, offset);
                break;
            case OrderFilledDecoder.TEMPLATE_ID:
                decodeOrderFilled(buffer, offset);
                break;
            default:
                System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Unknown message received.");
        }
    }

    private void decodeOrderAcknowledged(UnsafeBuffer buffer, int offset) {
        acknowledgedDecoder.wrap(buffer, offset, acknowledgedDecoder.sbeBlockLength(), acknowledgedDecoder.sbeSchemaVersion());
        long orderId = acknowledgedDecoder.orderId();
        short status = acknowledgedDecoder.status();
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Order Acknowledged: orderId=" + orderId + ", status=" + (status == 1 ? "Accepted" : "Rejected"));
    }

    private void decodeOrderFilled(UnsafeBuffer buffer, int offset) {
        filledDecoder.wrap(buffer, offset, filledDecoder.sbeBlockLength(), filledDecoder.sbeSchemaVersion());
        long orderId = filledDecoder.orderId();
        long filledQty = filledDecoder.filledQuantity();
        long filledPrice = filledDecoder.filledPrice();
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Order Filled: orderId=" + orderId + ", filledQuantity=" + filledQty + ", filledPrice=" + filledPrice);
    }

    private void readOrders() {
        while (true) {
            System.out.println("Enter order (orderId,symbol,side,quantity,price): ");
            String input = scanner.nextLine();
            String[] parts = input.split(",");
            if (parts.length != 5) {
                System.out.println("Invalid format. Try again.");
                continue;
            }
            try {
                long orderId = Long.parseLong(parts[0].trim());
                String symbol = parts[1].trim();
                byte side = Byte.parseByte(parts[2].trim());
                long qty = Long.parseLong(parts[3].trim());
                long price = Long.parseLong(parts[4].trim());
                sendNewOrder(orderId, symbol, side, qty, price);
            } catch (Exception e) {
                System.out.println("Invalid input.");
                e.printStackTrace();
            }
        }
    }

    private void sendNewOrder(long orderId, String symbol, byte side, long qty, long price) {
        byte[] message = encodeNewOrder(orderId, symbol, side, qty, price);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(message));
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + "Sent Order: orderId=" + orderId +
                ", symbol=" + symbol +", side=" + side + ", qty=" + qty + ", price=" + price);
    }
}
