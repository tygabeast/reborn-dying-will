package com.reborndw.client;

import com.reborndw.flame.FlameData;
import com.reborndw.network.FlameSyncPayload;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleSync(FlameSyncPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        FlameData d = new FlameData();
        d.deserializeNBT(level.registryAccess(), payload.data());
        var ranks = com.reborndw.flame.Rank.values();
        d.syncedRank = ranks[Math.floorMod(payload.data().getInt("syncRank"), ranks.length)];
        d.syncedEffRank = ranks[Math.floorMod(payload.data().getInt("syncEffRank"), ranks.length)];
        d.syncedHasRing = payload.data().getBoolean("syncHasRing");
        ClientFlameCache.put(payload.entityId(), d);
    }
}
