package com.exchange.server;

import com.exchange.*;
import com.exchange.matching_engine.MatchingEngine;
import com.exchange.matching_engine.Order;
import com.exchange.matching_engine.OrderPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Object2IntHashMap;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.UUID;

public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Int2ObjectHashMap<Channel> clientIdToChannel = new Int2ObjectHashMap<>();
    private final Object2IntHashMap<Channel> channelToClientId = new Object2IntHashMap<>(-1);
    private int nextClientId = 1; // atomic or not?

    private final ByteBuf encodeByteBuf = PooledByteBufAllocator.DEFAULT.buffer(128);
    private final UnsafeBuffer unsafeEncodeBuffer = new UnsafeBuffer(0,0);
    private final UnsafeBuffer unsafeDecodeBuffer = new UnsafeBuffer(0,0); // wrap at read time

    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final NewOrderDecoder newOrderDecoder = new NewOrderDecoder();
    private final CancelOrderDecoder cancelOrderDecoder = new CancelOrderDecoder();
    private final BulkCancelOrdersDecoder bulkCancelOrdersDecoder = new BulkCancelOrdersDecoder();
    private final OrderAcknowledgedEncoder orderAcknowledgedEncoder = new OrderAcknowledgedEncoder();
    private final OrderRejectedEncoder orderRejectedEncoder = new OrderRejectedEncoder();

    private final OrderPool orderPool = new OrderPool();
    private final MatchingEngine matchingEngine = new MatchingEngine();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        int clientId = nextClientId++; // reuse ids with a pool?
        Channel channel = ctx.channel();
        System.out.println("Connected to channel " + channel);

        clientIdToChannel.put(clientId, channel);
        channelToClientId.put(channel, clientId);

        System.out.println("Size of clientIdToChannel: " + clientIdToChannel.size());
        System.out.println("Size of channelToClientId: " + channelToClientId.size());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        System.out.println("Server received message");
        System.out.println("Readable bytes: " + byteBuf.readableBytes());

        if (!byteBuf.isReadable()) {
            return; // no data to read, exit early
        }

        unsafeDecodeBuffer.wrap(byteBuf.memoryAddress(), byteBuf.readableBytes());
        System.out.println("unsafeBuffer capacity: " + unsafeDecodeBuffer.capacity());

        messageHeaderDecoder.wrap(unsafeDecodeBuffer, 0);

        // debugging start
        System.out.println("Schema ID: " + messageHeaderDecoder.schemaId());
        System.out.println("Template ID: " + messageHeaderDecoder.templateId());
        // debugging end

        int templateId = messageHeaderDecoder.templateId();

        switch (templateId) {
            case NewOrderDecoder.TEMPLATE_ID:
                receiveNewOrder(ctx, unsafeDecodeBuffer);
                break;
            case CancelOrderDecoder.TEMPLATE_ID:
                cancelOrder(ctx, unsafeDecodeBuffer);
                sendOrderAcknowledged(ctx, cancelOrderDecoder.orderId(), MessageTypeEnum.CancelOrder);
                break;
            case BulkCancelOrdersDecoder.TEMPLATE_ID:
                bulkCancelOrder(ctx, unsafeDecodeBuffer);
                break;
            default:
                // send order rejected
                System.err.println("Unknown template ID: " + templateId);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        int clientId = channelToClientId.remove(channel);
        clientIdToChannel.remove(clientId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void receiveNewOrder(ChannelHandlerContext ctx, UnsafeBuffer unsafeBuffer) {
        try {
            newOrderDecoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderDecoder);

            int clientId = channelToClientId.get(ctx.channel());
            System.out.println("Client ID: " + clientId);

            if (!isValidOrder(newOrderDecoder.price(), newOrderDecoder.quantity(), newOrderDecoder.side())) {
                System.err.println("Received invalid order");
                sendOrderRejected(ctx, OrderRejectEnum.InvalidOrder);
                return;
            }

            long orderId = Math.abs(UUID.randomUUID().getLeastSignificantBits());

            Order order = orderPool.acquire();
            order.set(orderId,
                    clientId,
                    newOrderDecoder.price(),
                    newOrderDecoder.quantity(),
                    newOrderDecoder.side());

            matchingEngine.addOrder(order);

            sendOrderAcknowledged(ctx, orderId, MessageTypeEnum.NewOrder);
        } catch (Exception e) {
            System.err.println("Exception while receiving new order: " + e.getMessage());
            e.printStackTrace();
            sendOrderRejected(ctx, OrderRejectEnum.Other);
        }
    }

    private void cancelOrder(ChannelHandlerContext ctx, UnsafeBuffer unsafeBuffer) {
        try {
            cancelOrderDecoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderDecoder);

            int clientId = channelToClientId.get(ctx.channel());
            System.out.println("Client ID: " + clientId);

            if (!matchingEngine.isOrderIdValid(cancelOrderDecoder.orderId())) {
                System.err.println("Invalid order ID: " + cancelOrderDecoder.orderId());
                sendOrderRejected(ctx, OrderRejectEnum.UnknownOrderID);
                return;
            }

            Order order = matchingEngine.cancelOrder(cancelOrderDecoder.orderId());
            orderPool.release(order);
        } catch (Exception e) {
            System.err.println("Exception while cancelling order: " + e.getMessage());
            e.printStackTrace();
            sendOrderRejected(ctx, OrderRejectEnum.Other);
        }
    }

    private void bulkCancelOrder(ChannelHandlerContext ctx, UnsafeBuffer unsafeBuffer) {
        try {
            bulkCancelOrdersDecoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderDecoder);

            int clientId = channelToClientId.get(ctx.channel());
            System.out.println("Client ID: " + clientId);

            for (BulkCancelOrdersDecoder.OrdersDecoder orders : bulkCancelOrdersDecoder.orders()) {
                if (!matchingEngine.isOrderIdValid(orders.orderId())) {
                    System.err.println("Invalid order ID: " + orders.orderId());
                    sendOrderRejected(ctx, OrderRejectEnum.UnknownOrderID);
                    continue;
                }
                Order order = matchingEngine.cancelOrder(orders.orderId());
                orderPool.release(order);

                sendOrderAcknowledged(ctx, orders.orderId(), MessageTypeEnum.BulkCancel);
            }
        } catch (Exception e) {
            System.err.println("Exception while bulk cancelling orders: " + e.getMessage());
            e.printStackTrace();
            sendOrderRejected(ctx, OrderRejectEnum.Other);
        }
    }

    private void sendOrderAcknowledged(ChannelHandlerContext ctx, long orderId, MessageTypeEnum msgType) {

        encodeByteBuf.clear();
        unsafeEncodeBuffer.wrap(encodeByteBuf.memoryAddress(), encodeByteBuf.capacity());

        orderAcknowledgedEncoder.wrapAndApplyHeader(unsafeEncodeBuffer, 0, messageHeaderEncoder)
                .orderId(orderId)
                .messageType(msgType);

        int msgLen = MessageHeaderEncoder.ENCODED_LENGTH + orderAcknowledgedEncoder.encodedLength();
        System.out.println("Encoded message length: " + msgLen);
        encodeByteBuf.writerIndex(msgLen);

        System.out.println("Sending order acknowledged to client for " + msgType);

        ctx.writeAndFlush(encodeByteBuf.retainedDuplicate()).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Sent order acknowledged to client");
            } else {
                System.err.println("Failed to send order acknowledged to client");
                future.cause().printStackTrace();
            }
        });
    }

    private void sendOrderRejected(ChannelHandlerContext ctx, OrderRejectEnum reason) {
        encodeByteBuf.clear();
        unsafeEncodeBuffer.wrap(encodeByteBuf.memoryAddress(), encodeByteBuf.capacity());

        orderRejectedEncoder.wrapAndApplyHeader(unsafeEncodeBuffer, 0, messageHeaderEncoder)
                .reason(reason);

        int msgLen = MessageHeaderEncoder.ENCODED_LENGTH + orderRejectedEncoder.encodedLength();
        System.out.println("Encoded message length: " + msgLen);
        encodeByteBuf.writerIndex(msgLen);

        System.out.println("Sending order rejected to client due to " + reason);

        ctx.writeAndFlush(encodeByteBuf.retainedDuplicate()).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Sent order rejected to client");
            } else {
                System.err.println("Failed to send order rejected to client");
                future.cause().printStackTrace();
            }
        });
    }

    private boolean isValidOrder(double price, long qty, SideEnum side) {
        if (qty <= 0) {
            return false;
        }

        if (price <= 0) {
            return false;
        }

        if (side != SideEnum.BUY && side != SideEnum.SELL) {
            return false;
        }

        return true;
    }

}
