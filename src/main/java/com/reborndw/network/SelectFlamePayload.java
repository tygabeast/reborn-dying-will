package com.reborndw.network;

import com.reborndw.RebornDW;
import com.reborndw.flame.Flame;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player picked a flame in the flame selection screen. */
public record SelectFlamePayload(Flame flame) implements CustomPacketPayload {
    public static final Type<SelectFlamePayload> TYPE = new Type<>(RebornDW.id("select_flame"));
    public static final StreamCodec<ByteBuf, SelectFlamePayload> CODEC =
            Flame.STREAM_CODEC.map(SelectFlamePayload::new, SelectFlamePayload::flame);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
