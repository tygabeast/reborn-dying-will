package com.reborndw.network;

import com.reborndw.RebornDW;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a hotkey was pressed. */
public record ActionPayload(Action action) implements CustomPacketPayload {
    public enum Action { PRIMARY, SECONDARY, SIGNATURE, HYPER, SWITCH_FLAME }

    public static final Type<ActionPayload> TYPE = new Type<>(RebornDW.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(i -> new ActionPayload(Action.values()[Math.floorMod(i, Action.values().length)]),
                    p -> p.action().ordinal());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
