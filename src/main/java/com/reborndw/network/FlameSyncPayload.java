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
        net.minecraft.nbt.CompoundTag tag = d.serializeNBT(player.registryAccess());
        // display extras for the HUD: rank by the server's thresholds, effective rank and whether a ring is worn
        tag.putInt("syncRank", d.active == null ? 0 : com.reborndw.flame.FlameLogic.rank(d, d.active).ordinal());
        tag.putInt("syncEffRank", d.cachedRank.ordinal());
        tag.putBoolean("syncHasRing", d.cachedHasRing);
        return new FlameSyncPayload(player.getId(), tag);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
