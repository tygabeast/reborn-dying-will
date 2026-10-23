package com.reborndw.network;

import com.reborndw.RebornDW;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a hotkey was pressed or (for ability keys, so hold abilities work) released. */
public record ActionPayload(Action action, boolean pressed) implements CustomPacketPayload {
    public enum Action { PRIMARY, SECONDARY, SIGNATURE, HYPER, RELEASE, INFUSE }

    public ActionPayload(Action action) { this(action, true); }

    public static final Type<ActionPayload> TYPE = new Type<>(RebornDW.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = ByteBufCodecs.VAR_INT.map(
            i -> new ActionPayload(Action.values()[Math.floorMod(i >> 1, Action.values().length)], (i & 1) == 1),
            p -> (p.action().ordinal() << 1) | (p.pressed() ? 1 : 0));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
