package com.reborndw.ability;

import com.reborndw.RebornDW;
import com.reborndw.config.KitConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import com.reborndw.util.DelayedTasks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * All flame ability damage goes through here, so each flame's property applies to its hits:
 * Storm ignores armor, Rain adds Tranquility, Lightning chains, Cloud echoes, Mist confuses.
 */
public final class FlameDamage {
    public static final ResourceKey<DamageType> FLAME = ResourceKey.create(Registries.DAMAGE_TYPE, RebornDW.id("flame"));
    public static final ResourceKey<DamageType> FLAME_PIERCE = ResourceKey.create(Registries.DAMAGE_TYPE, RebornDW.id("flame_pierce"));

    public static final int NO_ECHO = 1, NO_CHAIN = 2;
    /** Entity tag: Cloud Spread doesn't copy this projectile. */
    public static final String TAG_NO_SPLIT = RebornDW.MODID + ".no_split";

    private static Flame current;

    private FlameDamage() {}

    /** The flame of the damage being dealt right now, or null (read by damage events, e.g. Storm vs shields). */
    @Nullable
    public static Flame current() { return current; }

    public static float scaled(double base, Rank eff) {
        return (float) (base * (1 + KitConfig.RANK_DAMAGE_SCALING.get() * eff.ordinal()));
    }

    public static boolean deal(ServerPlayer attacker, LivingEntity target, Flame flame, float amount, Rank eff, int flags) {
        if (amount <= 0 || !target.isAlive()) return false;
        ServerLevel level = attacker.serverLevel();
        var types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        DamageSource src;
        float dmg = amount;
        if (flame == Flame.STORM) {
            // Disintegration: work out the damage against reduced armor, then deal it past armor
            src = new DamageSource(types.getHolderOrThrow(FLAME_PIERCE), attacker);
            double pierce = Math.min(1.0, KitConfig.STORM_PIERCE_BASE.get() + KitConfig.STORM_PIERCE_PER_RANK.get() * eff.ordinal());
            dmg = CombatRules.getDamageAfterAbsorb(target, amount, src, (float) (target.getArmorValue() * (1 - pierce)),
                    (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        } else {
            src = new DamageSource(types.getHolderOrThrow(FLAME), attacker);
        }
        Flame prev = current;
        current = flame;
        boolean hit;
        try {
            hit = target.hurt(src, dmg);
        } finally {
            current = prev;
        }
        if (!hit) return false;
        switch (flame) {
            case RAIN -> Tranquility.addStacks(target, eff, 1);
            case LIGHTNING -> { if ((flags & NO_CHAIN) == 0) chain(attacker, target, amount, eff); }
            case CLOUD -> { if ((flags & NO_ECHO) == 0) maybeEcho(attacker, target, amount, eff); }
            case MIST -> maybeConfuse(attacker, target, eff);
            default -> {}
        }
        return true;
    }

    /** Lightning: the hit jumps to nearby enemies, one at E, two from C, three from A. */
    private static void chain(ServerPlayer attacker, LivingEntity first, float amount, Rank eff) {
        int jumps = 1 + (eff.atLeast(Rank.C) ? 1 : 0) + (eff.atLeast(Rank.A) ? 1 : 0);
        float chainDamage = (float) (amount * KitConfig.CHAIN_FRACTION.get());
        double range = KitConfig.CHAIN_RANGE.get();
        Set<LivingEntity> hit = new HashSet<>();
        hit.add(first);
        LivingEntity from = first;
        for (int i = 0; i < jumps; i++) {
            final LivingEntity src = from;
            LivingEntity next = attacker.serverLevel().getEntitiesOfClass(LivingEntity.class, src.getBoundingBox().inflate(range),
                            e -> e.isAlive() && e != attacker && !hit.contains(e) && !SkyAbilities.isAlly(attacker, e)
                                    && e.distanceTo(src) <= range)
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(src))).orElse(null);
            if (next == null) break;
            arc(attacker.serverLevel(), src.position().add(0, src.getBbHeight() / 2, 0), next.position().add(0, next.getBbHeight() / 2, 0));
            next.invulnerableTime = 0;
            deal(attacker, next, Flame.LIGHTNING, chainDamage, eff, NO_CHAIN);
            hit.add(next);
            from = next;
        }
    }

    private static void arc(ServerLevel level, Vec3 a, Vec3 b) {
        int steps = (int) Math.max(3, a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, b.x, b.y, b.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.15F, 2.0F);
    }

    /** Cloud: a chance for the hit to repeat a moment later at half damage. Echoes don't echo. */
    private static void maybeEcho(ServerPlayer attacker, LivingEntity target, float amount, Rank eff) {
        double chance = KitConfig.ECHO_BASE.get() + KitConfig.ECHO_PER_RANK.get() * eff.ordinal();
        if (attacker.getRandom().nextDouble() >= chance) return;
        DelayedTasks.schedule(5, () -> {
            if (!target.isAlive() || attacker.isRemoved()) return;
            target.invulnerableTime = 0;
            if (deal(attacker, target, Flame.CLOUD, amount * 0.5F, eff, NO_ECHO) && target.level() instanceof ServerLevel sl)
                sl.sendParticles(new DustParticleOptions(SkyAbilities.rgb(Flame.CLOUD.color), 1.2F),
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 8, 0.3, 0.4, 0.3, 0);
        });
    }

    /** Mist: a chance to confuse. A mob turns on the nearest other creature; a player gets Nausea. */
    private static void maybeConfuse(ServerPlayer attacker, LivingEntity target, Rank eff) {
        double chance = KitConfig.CONFUSE_BASE.get() + KitConfig.CONFUSE_PER_RANK.get() * eff.ordinal();
        if (attacker.getRandom().nextDouble() >= chance) return;
        confuse(attacker, target);
    }

    public static void confuse(ServerPlayer attacker, LivingEntity target) {
        if (target instanceof Player p) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        } else if (target instanceof Mob mob) {
            LivingEntity other = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(8),
                            e -> e.isAlive() && e != mob && e != attacker && !(e instanceof Player))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(mob))).orElse(null);
            if (other != null) {
                mob.setTarget(other);
                mob.addEffect(new MobEffectInstance(ModRegistry.CONFUSED, 60, 0, false, true, true));
            }
        }
        if (target.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY() + 0.3, target.getZ(), 6, 0.3, 0.2, 0.3, 0);
    }
}
