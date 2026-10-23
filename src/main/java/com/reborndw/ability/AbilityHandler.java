package com.reborndw.ability;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.network.ActionPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Routes hotkeys. Each key fires from the first source with an ability: held weapon, gloves, then the ring's innate kit. */
public final class AbilityHandler {
    private AbilityHandler() {}

    public static void handle(ServerPlayer player, ActionPayload.Action action) {
        FlameData d = FlameLogic.get(player);
        if (!d.awakened || d.active == null || player.isSpectator()) return;
        switch (action) {
            case HYPER -> FlameLogic.toggleHyper(player);
            case RELEASE -> FlameLogic.release(player);
            case PRIMARY, SECONDARY, SIGNATURE -> {
                if (d.active == Flame.SKY) SkyAbilities.use(player, action);
                else player.displayClientMessage(Component.translatable("message.reborn_dying_will.kit_not_ready",
                        Component.translatable(d.active.translationKey()).withColor(d.active.color)), true);
            }
        }
    }
}
