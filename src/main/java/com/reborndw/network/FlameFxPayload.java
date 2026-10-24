package com.reborndw.network;

import com.reborndw.RebornDW;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: play a flame effect. {@code kind} is one of {@link com.reborndw.fx.FlameFx}'s kinds; (x, y, z) is
 * where it happens and (a, b, c) a second point or a direction, depending on the kind; {@code extra} packs small
 * per-kind numbers (a slash's arc and roll).
 */
public record FlameFxPayload(int kind, int flame, float x, float y, float z, float a, float b, float c, float size, int extra)
        implements CustomPacketPayload {
    public static final Type<FlameFxPayload> TYPE = new Type<>(RebornDW.id("flame_fx"));
    public static final StreamCodec<ByteBuf, FlameFxPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeByte(p.kind);
        buf.writeByte(p.flame);
        buf.writeFloat(p.x); buf.writeFloat(p.y); buf.writeFloat(p.z);
        buf.writeFloat(p.a); buf.writeFloat(p.b); buf.writeFloat(p.c);
        buf.writeFloat(p.size);
        buf.writeInt(p.extra);
    }, buf -> new FlameFxPayload(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
