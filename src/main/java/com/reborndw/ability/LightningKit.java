package com.reborndw.ability;

import com.reborndw.RebornDW;
import com.reborndw.config.KitConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning: Hardening. Passive: extra armor toughness and knockback resistance, armor and shields wear 25% slower
 * (KitEvents), Lightning hits chain (FlameDamage), and the pool refills twice as fast in thunderstorms (FlameLogic).
 */
public class LightningKit implements FlameKit {
    public static final ResourceLocation HARDENING_ID = RebornDW.id("lightning_hardening");

    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Harden: take less damage and ignore knockback for a few seconds. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.HARDEN_COST.get())) return;
        player.addEffect(new MobEffectInstance(ModRegistry.HARDENED, KitConfig.HARDEN_SECONDS.get() * 20, 0, false, false, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5F, 1.5F);
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.7, 0.4, 0.05);
    }

    /** Static Discharge: a shock around you that damages and stuns. */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.DISCHARGE_COST.get())) return;
        ServerLevel level = player.serverLevel();
        double r = KitConfig.DISCHARGE_RADIUS.get();
        Vec3 c = player.position().add(0, 1, 0);
        float damage = FlameDamage.scaled(KitConfig.DISCHARGE_DAMAGE.get(), eff);
        for (LivingEntity e : Kits.enemiesAround(player, c, r)) {
            // no chaining from the discharge itself: it already hits everything in range
            FlameDamage.deal(player, e, Flame.LIGHTNING, damage, eff, FlameDamage.NO_CHAIN);
            e.addEffect(new MobEffectInstance(ModRegistry.STUNNED, KitConfig.STUN_TICKS.get(), 0, false, true, true));
        }
        for (int i = 0; i < 40; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2, dist = player.getRandom().nextDouble() * r;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * dist, c.y + (player.getRandom().nextDouble() - 0.5), c.z + Math.sin(a) * dist, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    @Override
    public void tick(ServerPlayer player, FlameData d, Rank eff) {
        if (player.tickCount % 10 != 0) return;
        double toughness = KitConfig.LIGHTNING_TOUGHNESS_BASE.get() + KitConfig.LIGHTNING_TOUGHNESS_PER_RANK.get() * eff.ordinal();
        double kb = KitConfig.LIGHTNING_KB_BASE.get() + KitConfig.LIGHTNING_KB_PER_RANK.get() * eff.ordinal();
        FlameLogic.setModifier(player, Attributes.ARMOR_TOUGHNESS, HARDENING_ID, toughness, AttributeModifier.Operation.ADD_VALUE);
        FlameLogic.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, HARDENING_ID, kb, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onDeactivate(ServerPlayer player, FlameData d) {
        FlameLogic.setModifier(player, Attributes.ARMOR_TOUGHNESS, HARDENING_ID, 0, AttributeModifier.Operation.ADD_VALUE);
        FlameLogic.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, HARDENING_ID, 0, AttributeModifier.Operation.ADD_VALUE);
    }
}
