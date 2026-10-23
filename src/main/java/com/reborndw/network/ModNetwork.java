package com.reborndw.network;

import com.reborndw.ability.AbilityHandler;
import com.reborndw.client.ClientPayloadHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(FlameSyncPayload.TYPE, FlameSyncPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientPayloadHandler.handleSync(payload)));
        r.playToServer(ActionPayload.TYPE, ActionPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) AbilityHandler.handle(sp, payload.action(), payload.pressed());
                }));
        r.playToServer(SelectFlamePayload.TYPE, SelectFlamePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp && !sp.isSpectator())
                        com.reborndw.flame.FlameLogic.switchFlame(sp, payload.flame());
                }));
    }
}
