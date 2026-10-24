package com.reborndw.network;

import com.reborndw.RebornDW;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a player starts a weapon move's animation ({@link com.reborndw.anim.MoveAnim} ordinal). */
public record MoveAnimPayload(int entityId, int anim, boolean offHand) implements CustomPacketPayload {
    public static final Type<MoveAnimPayload> TYPE = new Type<>(RebornDW.id("move_anim"));
    public static final StreamCodec<ByteBuf, MoveAnimPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoveAnimPayload::entityId,
            ByteBufCodecs.VAR_INT, MoveAnimPayload::anim,
            ByteBufCodecs.BOOL, MoveAnimPayload::offHand,
            MoveAnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
