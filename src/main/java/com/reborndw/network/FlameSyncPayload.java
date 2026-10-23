package com.reborndw.network;

import com.reborndw.RebornDW;
import com.reborndw.flame.FlameData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** Server to client: a player's flame state, for the HUD and for visuals other players see. */
public record FlameSyncPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<FlameSyncPayload> TYPE = new Type<>(RebornDW.id("flame_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlameSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlameSyncPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, FlameSyncPayload::data,
            FlameSyncPayload::new);

    public static FlameSyncPayload of(Player player, FlameData d) {
        return new FlameSyncPayload(player.getId(), d.serializeNBT(player.registryAccess()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
