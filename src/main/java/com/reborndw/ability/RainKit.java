package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Rank;
import com.reborndw.network.ActionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rain: Tranquility. Passive: Rain hits add Tranquility stacks (FlameDamage/Tranquility); with a Rain ring worn,
 * you can't stay on fire. Calm Field is held; Flame Parry is a short window (handled in KitEvents).
 */
public class RainKit implements FlameKit {
    public static final String TAG_SLOWED = "reborn_dying_will.calm_slowed";

    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Calm Field: hold to keep a field around you. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (d.heldAction == ActionPayload.Action.PRIMARY) return;
        if (!Kits.spend(player, KitConfig.CALM_FIELD_COST.get())) return;
        d.heldAction = ActionPayload.Action.PRIMARY;
        d.holdTicks = 0;
        player.level().playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @Override
    public void onRelease(ServerPlayer player, FlameData d, ActionPayload.Action action) {
        if (action == ActionPayload.Action.PRIMARY) d.heldAction = null;
    }

    /** Flame Parry: a half-second window that negates one melee hit. */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        if (d.parryTicks > 0) return;
        if (!Kits.spend(player, KitConfig.PARRY_COST.get())) return;
        d.parryTicks = KitConfig.PARRY_TICKS.get();
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        player.serverLevel().sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    public void tick(ServerPlayer player, FlameData d, Rank eff) {
        if (d.parryTicks > 0) d.parryTicks--;
        // Rain puts out fire while a Rain ring is worn
        if (player.isOnFire() && player.tickCount % 5 == 0 && RingHelper.hasRing(player)) player.clearFire();

        if (d.heldAction == ActionPayload.Action.PRIMARY && eff.atLeast(primaryRank())) {
            d.holdTicks++;
            if (d.holdTicks % 20 == 0 && !Kits.spend(player, KitConfig.CALM_FIELD_UPKEEP.get() * Kits.upkeepFactor(player))) {
                d.heldAction = null;
                return;
            }
            field(player, d, eff);
        } else if (d.heldAction == ActionPayload.Action.PRIMARY) {
            d.heldAction = null; // lost the ring or rank mid-hold
        }
    }

    private void field(ServerPlayer player, FlameData d, Rank eff) {
        ServerLevel level = player.serverLevel();
        double r = KitConfig.CALM_FIELD_RADIUS.get();
        Vec3 c = player.position().add(0, 1, 0);
        // enemy projectiles entering the field slow to half speed (once each)
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r),
                p -> p.getOwner() != player && !p.getTags().contains(TAG_SLOWED))) {
            p.setDeltaMovement(p.getDeltaMovement().scale(0.5));
            p.hurtMarked = true;
            p.addTag(TAG_SLOWED);
        }
        if (d.holdTicks % 20 == 0) {
            for (LivingEntity e : Kits.enemiesAround(player, c, r)) Tranquility.addStacks(e, eff, 1);
            // and it puts out fires it passes over
            BlockPos base = player.blockPosition();
            int ir = (int) Math.ceil(r);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-ir, -2, -ir), base.offset(ir, 2, ir))) {
                if (level.getBlockState(pos).is(BlockTags.FIRE) && pos.distSqr(base) <= r * r) level.removeBlock(pos, false);
            }
        }
        if (d.holdTicks % 4 == 0) {
            var color = SkyAbilities.rgb(Flame.RAIN.color);
            Kits.circle(level, player.position().add(0, 0.1, 0), r, new DustParticleOptions(color, 1.0F), 16);
            for (int i = 0; i < 3; i++) {
                double a = player.getRandom().nextDouble() * Math.PI * 2, dist = player.getRandom().nextDouble() * r;
                level.sendParticles(ParticleTypes.FALLING_WATER, c.x + Math.cos(a) * dist, c.y + 2.5, c.z + Math.sin(a) * dist, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void onDeactivate(ServerPlayer player, FlameData d) {
        d.parryTicks = 0;
    }
}
