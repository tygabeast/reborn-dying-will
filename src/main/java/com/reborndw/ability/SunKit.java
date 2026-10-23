package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.util.EffectTimers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Sun: Activation. Passive: extra regeneration, harmful effects wear off faster, and Sun abilities grow plants.
 */
public class SunKit implements FlameKit {
    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Activation Pulse: heal over 5 seconds and grow nearby crops. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.PULSE_COST.get())) return;
        double total = KitConfig.PULSE_HEAL.get() + KitConfig.PULSE_HEAL_PER_RANK.get() * Math.max(0, eff.ordinal() - Rank.D.ordinal());
        d.healTicks = 100;
        d.healPerTick = total / 100.0;
        growPlants(player.serverLevel(), player.blockPosition(), KitConfig.PULSE_GROW_RADIUS.get(), player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        burst(player.serverLevel(), player, 14);
    }

    /** Rally: heal allies around you and clear one harmful effect from each. */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.RALLY_COST.get())) return;
        float heal = (float) (KitConfig.RALLY_HEAL.get() + KitConfig.RALLY_HEAL_PER_RANK.get() * Math.max(0, eff.ordinal() - Rank.C.ordinal()));
        List<LivingEntity> targets = new ArrayList<>(Kits.alliesAround(player, KitConfig.RALLY_RADIUS.get()));
        targets.add(player);
        for (LivingEntity e : targets) {
            e.heal(heal);
            clearOneHarmful(e);
            burst(player.serverLevel(), e, 8);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** Clears the first harmful effect, except the weakness a Dying Will form leaves behind. */
    private static void clearOneHarmful(LivingEntity e) {
        for (MobEffectInstance inst : List.copyOf(e.getActiveEffects())) {
            if (inst.getEffect().value().isBeneficial() || inst.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.NEUTRAL) continue;
            if (inst.getEffect().value() == MobEffects.WEAKNESS.value() && e instanceof ServerPlayer sp && FlameLogic.get(sp).ownWeakness) continue;
            e.removeEffect(inst.getEffect());
            return;
        }
    }

    @Override
    public void tick(ServerPlayer player, FlameData d, Rank eff) {
        if (d.healTicks > 0) {
            d.healTicks--;
            if (player.getHealth() < player.getMaxHealth()) player.heal((float) d.healPerTick);
        }
        // Activation: extra regeneration (half a heart every 4 s at E, every 2 s at S)
        int interval = Math.max(10, KitConfig.SUN_REGEN_INTERVAL_E.get() - KitConfig.SUN_REGEN_INTERVAL_PER_RANK.get() * eff.ordinal());
        if (player.tickCount % interval == 0 && player.getHealth() < player.getMaxHealth() && !player.isDeadOrDying()) player.heal(1.0F);
        // harmful effects wear off faster: 25% at E plus 5% per rank, applied once a second
        if (player.tickCount % 20 == 0) {
            double faster = KitConfig.SUN_CLEANSE_BASE.get() + KitConfig.SUN_CLEANSE_PER_RANK.get() * eff.ordinal();
            EffectTimers.shorten(player, i -> i.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL,
                    (int) Math.round(20 * faster), true);
        }
    }

    @Override
    public void onDeactivate(ServerPlayer player, FlameData d) {
        d.healTicks = 0;
    }

    /** Ages crops and saplings around a spot, like a splash of bone meal. */
    public static void growPlants(ServerLevel level, BlockPos center, double radius, ServerPlayer player) {
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (pos.distSqr(center) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS))) continue;
            if (state.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(level, pos, state)) {
                BlockPos at = pos.immutable();
                if (b.isBonemealSuccess(level, level.random, at, state)) b.performBonemeal(level, level.random, at, state);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, at, 0);
            }
        }
    }

    private static void burst(ServerLevel level, LivingEntity e, int count) {
        level.sendParticles(new DustParticleOptions(SkyAbilities.rgb(Flame.SUN.color), 1.0F),
                e.getX(), e.getY() + 0.3, e.getZ(), count, 0.5, 0.2, 0.5, 0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 0.5, e.getZ(), count / 3, 0.4, 0.3, 0.4, 0);
    }
}
