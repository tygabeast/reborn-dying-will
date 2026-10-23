package com.reborndw.util;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/** Speeds status effects up by taking extra time off their timers (needs the access transformer on duration). */
public final class EffectTimers {
    private EffectTimers() {}

    /** Takes {@code ticks} off every matching, non-infinite effect. Never ends an effect directly: the game does on its next tick. */
    public static void shorten(LivingEntity entity, Predicate<MobEffectInstance> which, int ticks, boolean sync) {
        if (ticks <= 0) return;
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            if (inst.isInfiniteDuration() || !which.test(inst)) continue;
            inst.duration = Math.max(1, inst.duration - ticks);
            if (sync && entity instanceof ServerPlayer sp && sp.connection != null && !sp.isFakePlayer())
                sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), inst, false));
        }
    }
}
