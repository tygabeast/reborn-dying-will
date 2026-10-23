package com.reborndw.ability;

import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.network.ActionPayload;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Sky's innate ring kit: Harmony (passive), Flame Burst and Sky Shield. */
public final class SkyAbilities implements FlameKit {
    public static final SkyAbilities KIT = new SkyAbilities();

    private SkyAbilities() {}

    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    @Override
    public boolean beforeGate(ServerPlayer player, FlameData d, ActionPayload.Action action) {
        if (action == ActionPayload.Action.SECONDARY && d.shieldActive) { // switching the shield off needs no ring
            d.shieldActive = false; d.shieldHp = 0; d.markDirty();
            return true;
        }
        return false;
    }

    @Override public void primary(ServerPlayer player, FlameData d, Rank eff) { flameBurst(player, eff); }
    @Override public void secondary(ServerPlayer player, FlameData d, Rank eff) { skyShield(player, eff); }

    @Override
    public void onDeactivate(ServerPlayer player, FlameData d) {
        if (d.shieldActive) { d.shieldActive = false; d.shieldHp = 0; d.markDirty(); }
    }

    private static void noFlame(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.empty_pool"), true);
    }

    private static void flameBurst(ServerPlayer player, Rank eff) {
        if (!FlameLogic.spendFraction(player, ServerConfig.FLAME_BURST_COST.get())) { noFlame(player); return; }
        ServerLevel level = player.serverLevel();
        double radius = ServerConfig.FLAME_BURST_RADIUS.get();
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(radius));
        float damage = FlameDamage.scaled(ServerConfig.FLAME_BURST_DAMAGE.get(), eff);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(center) <= radius * radius && !isAlly(player, e))) {
            FlameDamage.deal(player, e, Flame.SKY, damage, eff, 0);
            Vec3 push = e.position().subtract(player.position()).normalize().scale(0.9);
            e.push(push.x, 0.35, push.z);
            e.hurtMarked = true;
        }
        Vector3f c = rgb(Flame.SKY.color);
        // the trail starts ahead of the caster and below eye level so it doesn't block their view
        Vec3 origin = player.getEyePosition().add(0, -0.35, 0);
        double start = 1.5, end = radius * 2;
        for (int i = 0; i <= 20; i++) {
            Vec3 p = origin.add(look.scale(start + (end - start) * i / 20.0));
            level.sendParticles(new DustParticleOptions(c, 1.0F), p.x, p.y, p.z, 1, 0.12, 0.12, 0.12, 0.0);
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 12, radius / 3, radius / 3, radius / 3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static void skyShield(ServerPlayer player, Rank eff) {
        if (!FlameLogic.spendFraction(player, ServerConfig.SKY_SHIELD_COST.get())) { noFlame(player); return; }
        FlameData d = FlameLogic.get(player);
        int ranksAboveC = eff.ordinal() - Rank.C.ordinal();
        d.shieldActive = true;
        d.shieldHp = 2.0 * ServerConfig.SKY_SHIELD_HEARTS.get() * (1 + ranksAboveC); // hearts to health points
        d.markDirty();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** Every tick while Sky is active. */
    @Override
    public void tick(ServerPlayer player, FlameData d, Rank eff) {
        if (d.shieldActive) {
            if (player.tickCount % 20 == 0 && !FlameLogic.spendFraction(player, ServerConfig.SKY_SHIELD_UPKEEP.get() * Kits.upkeepFactor(player))) {
                d.shieldActive = false; d.shieldHp = 0; d.markDirty();
            } else if (player.tickCount % 6 == 0) {
                // a few motes on a ring around the body, kept below the eyes
                Vector3f c = rgb(Flame.SKY.color);
                for (int i = 0; i < 3; i++) {
                    double a = player.getRandom().nextDouble() * Math.PI * 2;
                    player.serverLevel().sendParticles(new DustParticleOptions(c, 0.8F),
                            player.getX() + Math.cos(a) * 0.9, player.getY() + 0.2 + player.getRandom().nextDouble() * 0.9,
                            player.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
                }
            }
        }
        // Harmony: allies share part of your boost
        if (player.tickCount % 20 == 0 && d.form.boosted()) {
            double share = (FlameLogic.boostMultiplier(d) - 1) * ServerConfig.HARMONY_SHARE.get();
            int amp = (int) Math.round(share * 100) - 1;
            if (amp < 0) return;
            double r = ServerConfig.HARMONY_RADIUS.get();
            for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r),
                    e -> e != player && e.isAlive() && isAlly(player, e))) {
                e.addEffect(new MobEffectInstance(ModRegistry.HARMONIZED, 50, Math.min(amp, 254), true, false, true));
            }
        }
    }

    /** Allies: teammates, your tamed animals, and (by default) teamless players when you have no team. */
    public static boolean isAlly(Player player, LivingEntity other) {
        if (other instanceof OwnableEntity own && player.getUUID().equals(own.getOwnerUUID())) return true;
        if (other instanceof Player op) {
            if (player.getTeam() != null) return player.isAlliedTo(op);
            return ServerConfig.HARMONY_TEAMLESS_ALLIES.get() && op.getTeam() == null;
        }
        return player.getTeam() != null && player.isAlliedTo(other);
    }

    public static Vector3f rgb(int color) {
        return new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
    }
}
