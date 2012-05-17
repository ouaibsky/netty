package io.netty.handler.codec;

import static io.netty.handler.codec.MessageToMessageEncoder.*;
import io.netty.channel.ChannelBufferHolder;
import io.netty.channel.ChannelBufferHolders;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundHandlerContext;

import java.util.Queue;

public abstract class MessageToMessageDecoder<I, O> extends ChannelInboundHandlerAdapter<I> {

    @Override
    public ChannelBufferHolder<I> newInboundBuffer(
            ChannelInboundHandlerContext<I> ctx) throws Exception {
        return ChannelBufferHolders.messageBuffer();
    }

    @Override
    public void inboundBufferUpdated(ChannelInboundHandlerContext<I> ctx)
            throws Exception {
        Queue<I> in = ctx.in().messageBuffer();
        boolean decoded = false;
        for (;;) {
            try {
                I msg = in.poll();
                if (msg == null) {
                    break;
                }

                O emsg = decode(ctx, msg);
                if (emsg == null) {
                    throw new IllegalArgumentException(
                            "decode() returned null. unsupported message type? " +
                            msg.getClass().getName());
                }

                if (unfoldAndAdd(ctx, ctx.nextIn(), emsg)) {
                    decoded = true;
                }
            } catch (Throwable t) {
                ctx.fireExceptionCaught(t);
            }
        }
        if (decoded) {
            ctx.fireInboundBufferUpdated();
        }
    }

    public abstract O decode(ChannelInboundHandlerContext<I> ctx, I msg) throws Exception;
}