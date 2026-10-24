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

    public static void handleFx(com.reborndw.network.FlameFxPayload payload) {
        com.reborndw.client.fx.FxManager.add(payload);
    }

    public static void handleMoveAnim(com.reborndw.network.MoveAnimPayload payload) {
        var anims = com.reborndw.anim.MoveAnim.values();
        com.reborndw.client.anim.MoveAnims.play(payload.entityId(), anims[Math.floorMod(payload.anim(), anims.length)], payload.offHand());
    }
}
