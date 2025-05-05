package com.exchange.client;

import com.exchange.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.Scanner;

public class ExchangeClient {

    private final String host;
    private final int port;
    private Channel channel;

    private final ByteBuf encodeByteBuf = PooledByteBufAllocator.DEFAULT.buffer(128);
    private final UnsafeBuffer unsafeEncodeBuffer = new UnsafeBuffer(0,0);

    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final NewOrderEncoder newOrderEncoder = new NewOrderEncoder();
    private final CancelOrderEncoder cancelOrderEncoder = new CancelOrderEncoder();
    private final BulkCancelOrdersEncoder bulkCancelOrdersEncoder = new BulkCancelOrdersEncoder();

    public ExchangeClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws InterruptedException {
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ClientHandler());
                        }
                    });
            // Start the client.
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            // Start console input loop
            receiveUserInput();

            // Wait until the connection is closed.
            channel.closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    private void receiveUserInput(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Connected. Enter Orders in format: ");
        System.out.println("For new orders: NEW BUY/SELL <price> <qty>");
        System.out.println("For cancel order: CANCEL <orderId>");
        System.out.println("For bulk cancel order: BULKCANCEL <orderId>,<orderId>,...,<orderId>");
        System.out.println("");

        while (true) {
            System.out.println("> ");
            String line = scanner.nextLine();
            if (line == null || line.trim().equalsIgnoreCase("exit")) {
                break;
            }

            String[] parts = line.trim().split("\\s+");

            if (parts.length == 0) {
                System.out.println("Invalid input. Try again.");
                continue;
            }

            String msgType = parts[0].toUpperCase();

            switch (msgType) {
                case "NEW":
                    if (parts.length != 4) {
                        System.err.println("Invalid format for NEW order. Use: NEW BUY/SELL <price> <quantity>");
                        continue;
                    }
                    String sideStr = parts[1].toUpperCase();
                    SideEnum side = sideStr.equals("BUY") ? SideEnum.BUY :
                            sideStr.equals("SELL") ? SideEnum.SELL : null;
                    if (side == null) {
                        System.err.println("Invalid side. Use BUY or SELL.");
                        continue;
                    }
                    double price = Double.parseDouble(parts[2]);
                    long qty = Long.parseLong(parts[3]);

                    if ((price <= 0) || (qty <= 0)) {
                        System.err.println("Price and/or quantity must be greater than zero");
                        continue;
                    }

                    sendNewOrder(channel, encodeByteBuf, unsafeEncodeBuffer, side, price, qty);
                    break;
                case "CANCEL":
                    if (parts.length != 2) {
                        System.out.println("Invalid format for CANCEL order. Use: CANCEL <orderId>");
                        continue;
                    }
                    long orderId = Long.parseLong(parts[1]);

                    if (orderId <= 0) {
                        System.err.println("Order ID must be greater than 0");
                        continue;
                    }

                    sendCancelOrder(channel, encodeByteBuf, unsafeEncodeBuffer, orderId);
                    break;
                case "BULKCANCEL":
                    if (parts.length < 2) {
                        System.out.println("Invalid format for BULKCANCEL order. Use: BULKCANCEL <orderId>,...,<orderId>");
                        continue;
                    }
                    String[] orderIds = parts[1].split(",");
                    long[] orderIdList = new long[orderIds.length];

                    try {
                        for (int i = 0; i < orderIds.length; i++) {
                            orderIdList[i] = Long.parseLong(orderIds[i].trim());
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid order ID format - must be a number");
                        e.printStackTrace();
                        continue;
                    }

                    sendBulkCancelOrder(channel, encodeByteBuf, unsafeEncodeBuffer, orderIdList);
                    break;
                default:
                    System.out.println("Invalid number format.");
            }

        }
        scanner.close();
        channel.close();
    }

    private void sendNewOrder(Channel channel, ByteBuf byteBuf, UnsafeBuffer unsafeBuffer, SideEnum side, double price, long qty) {
        byteBuf.clear();
        unsafeBuffer.wrap(byteBuf.memoryAddress(), byteBuf.capacity());

        newOrderEncoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderEncoder)
                .side(side)
                .quantity(qty)
                .price(price)
                .orderType(OrderTypeEnum.LIMIT);

        int msgLen = MessageHeaderEncoder.ENCODED_LENGTH + newOrderEncoder.encodedLength();
        System.out.println("Encoded message length: " + msgLen);
        byteBuf.writerIndex(msgLen); // ensure correct length seen by Netty

        System.out.println("Sending NEW order to server...");

        channel.writeAndFlush(byteBuf.retainedDuplicate()).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Sent NEW order to server");
            } else {
                System.err.println("Failed to send order to server");
                future.cause().printStackTrace();
            }
        });
    }

    private void sendCancelOrder(Channel channel, ByteBuf byteBuf, UnsafeBuffer unsafeBuffer, long orderId) {
        byteBuf.clear();
        unsafeBuffer.wrap(byteBuf.memoryAddress(), byteBuf.capacity());

        cancelOrderEncoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderEncoder)
                .orderId(orderId);

        int msgLen = MessageHeaderEncoder.ENCODED_LENGTH + cancelOrderEncoder.encodedLength();
        System.out.println("Encoded message length: " + msgLen);
        byteBuf.writerIndex(msgLen);

        System.out.println("Sending CANCEL order to server...");

        channel.writeAndFlush(byteBuf.retainedDuplicate()).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Sent CANCEL order to server");
            } else {
                System.err.println("Failed to send order to server");
                future.cause().printStackTrace();
            }
        });
    }

    private void sendBulkCancelOrder(Channel channel, ByteBuf byteBuf, UnsafeBuffer unsafeBuffer, long[] orderIdList) {
        byteBuf.clear();
        unsafeBuffer.wrap(byteBuf.memoryAddress(), byteBuf.capacity());

        bulkCancelOrdersEncoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderEncoder);
        final BulkCancelOrdersEncoder.OrdersEncoder orders = bulkCancelOrdersEncoder.ordersCount(orderIdList.length);

        for (long orderId : orderIdList) {
            orders.next().orderId(orderId);
        }

        int msgLen = MessageHeaderEncoder.ENCODED_LENGTH + bulkCancelOrdersEncoder.encodedLength();
        System.out.println("MessageHeaderEncoder=" + MessageHeaderEncoder.ENCODED_LENGTH + ", bulkCancelOrdersEncoder=" + bulkCancelOrdersEncoder.encodedLength());
        System.out.println("Encoded message length: " + msgLen);
        byteBuf.writerIndex(msgLen);

        System.out.println("Sending BULKCANCEL order to server...");

        channel.writeAndFlush(byteBuf.retainedDuplicate()).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Sent BULKCANCEL order to server");
            } else {
                System.err.println("Failed to send order to server");
                future.cause().printStackTrace();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        new ExchangeClient("localhost",8080).start();
    }

}
