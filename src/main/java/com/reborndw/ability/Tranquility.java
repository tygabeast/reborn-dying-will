package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import com.reborndw.util.DelayedTasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Rain's Tranquility stacks, kept as the Tranquility effect's level. Each stack slows movement and attack speed by 5%.
 * Stacks last 3 seconds after the last Rain hit, then fall off one per second. At full stacks the target is calmed.
 */
public final class Tranquility {
    public static final int HOLD_TICKS = 60, STEP_TICKS = 20, CALM_TICKS = 60;

    private Tranquility() {}

    public static int stacks(LivingEntity e) {
        MobEffectInstance inst = e.getEffect(ModRegistry.TRANQUILITY);
        return inst == null ? 0 : inst.getAmplifier() + 1;
    }

    public static int cap(Rank attackerRank) {
        return KitConfig.TRANQUILITY_BASE_CAP.get() + attackerRank.ordinal();
    }

    public static void addStacks(LivingEntity target, Rank attackerRank, int count) {
        int cap = cap(attackerRank);
        int next = Math.min(cap, Math.max(stacks(target), 0) + count);
        if (next <= 0) return;
        target.removeEffect(ModRegistry.TRANQUILITY);
        target.addEffect(new MobEffectInstance(ModRegistry.TRANQUILITY, HOLD_TICKS, next - 1, false, false, true));
        if (next >= cap) calm(target);
    }

    public static void fill(LivingEntity target, Rank attackerRank) {
        addStacks(target, attackerRank, cap(attackerRank));
    }

    public static void calm(LivingEntity target) {
        boolean fresh = !target.hasEffect(ModRegistry.CALMED);
        target.addEffect(new MobEffectInstance(ModRegistry.CALMED, CALM_TICKS, 0, false, false, true));
        if (fresh && target instanceof Mob mob) mob.setTarget(null); // loses its target for a moment
        if (fresh && target.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.FALLING_WATER, target.getX(), target.getEyeY() + 0.4, target.getZ(), 12, 0.3, 0.1, 0.3, 0);
    }

    /** Called when the effect runs out: step down one stack (after this tick, outside the effect loop). */
    public static void onExpired(LivingEntity e, int amplifier) {
        if (amplifier <= 0) return;
        DelayedTasks.schedule(0, () -> {
            if (!e.isAlive() || e.hasEffect(ModRegistry.TRANQUILITY)) return;
            e.addEffect(new MobEffectInstance(ModRegistry.TRANQUILITY, STEP_TICKS, amplifier - 1, false, false, true));
        });
    }
}
