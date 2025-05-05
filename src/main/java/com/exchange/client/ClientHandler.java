package com.exchange.client;

import com.exchange.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.agrona.concurrent.UnsafeBuffer;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ByteBuf encodeByteBuf = PooledByteBufAllocator.DEFAULT.buffer(128);
    private final UnsafeBuffer unsafeEncodeBuffer = new UnsafeBuffer(encodeByteBuf.memoryAddress(), encodeByteBuf.capacity());
    private final UnsafeBuffer unsafeDecodeBuffer = new UnsafeBuffer(0,0);

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final OrderAcknowledgedDecoder orderAcknowledgedDecoder = new OrderAcknowledgedDecoder();
    private final OrderRejectedDecoder orderRejectedDecoder = new OrderRejectedDecoder();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        System.out.println("Client received message");
        System.out.println("Readable bytes: " + byteBuf.readableBytes());

        if (!byteBuf.isReadable()) {
            return; // no data to read, exit early
        }
        unsafeDecodeBuffer.wrap(byteBuf.memoryAddress(), byteBuf.readableBytes());
        System.out.println("unsafeBuffer capacity: " + unsafeDecodeBuffer.capacity());

        messageHeaderDecoder.wrap(unsafeDecodeBuffer, 0);

        int templateId = messageHeaderDecoder.templateId();

        switch(templateId) {
            case OrderAcknowledgedDecoder.TEMPLATE_ID:
                receiveOrderAcknowledged(ctx, unsafeDecodeBuffer);
                break;
            case OrderRejectedDecoder.TEMPLATE_ID:
                receiveOrderRejected(ctx, unsafeDecodeBuffer);
                break;
            default:
                System.err.println("Unknown template ID: " + templateId);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void receiveOrderAcknowledged(ChannelHandlerContext ctx, UnsafeBuffer unsafeBuffer) {
        orderAcknowledgedDecoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderDecoder);
        final StringBuilder sb = new StringBuilder();
        sb.append("Order acknowledged for orderId=").append(orderAcknowledgedDecoder.orderId());
        sb.append("\nMessage type acknowledged: ").append(orderAcknowledgedDecoder.messageType());
        System.out.println(sb);
    }

    private void receiveOrderRejected(ChannelHandlerContext ctx, UnsafeBuffer unsafeBuffer) {
        orderRejectedDecoder.wrapAndApplyHeader(unsafeBuffer, 0, messageHeaderDecoder);
        final StringBuilder sb = new StringBuilder();
        sb.append("Order rejected with reason: ").append(orderRejectedDecoder.reason());
        System.out.println(sb);
    }

}
