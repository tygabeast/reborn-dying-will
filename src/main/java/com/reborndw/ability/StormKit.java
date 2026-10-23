package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Rank;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Storm: Disintegration. Passive (in FlameDamage): Storm damage ignores 10% of armor plus 5% per rank,
 * and goes through other flames' shields at half strength.
 */
public class StormKit implements FlameKit {
    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Storm Bolt: a fast energy shot. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.STORM_BOLT_COST.get())) return;
        FlameShotEntity.shoot(player, Flame.STORM, player.getLookAngle(), 2.5,
                FlameDamage.scaled(KitConfig.STORM_BOLT_DAMAGE.get(), eff), KitConfig.STORM_BOLT_RANGE.get(), eff, 0, 1.1F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /** Storm Burst: destroys incoming projectiles around you and throws enemies back. */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.STORM_BURST_COST.get())) return;
        ServerLevel level = player.serverLevel();
        double r = KitConfig.STORM_BURST_RADIUS.get();
        Vec3 c = player.position().add(0, 1, 0);
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r),
                p -> p.getOwner() != player && !(p.getOwner() instanceof LivingEntity o && SkyAbilities.isAlly(player, o)))) {
            level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY(), p.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
            p.discard();
        }
        float damage = FlameDamage.scaled(KitConfig.STORM_BURST_DAMAGE.get(), eff);
        for (LivingEntity e : Kits.enemiesAround(player, c, r)) {
            FlameDamage.deal(player, e, Flame.STORM, damage, eff, 0);
            Vec3 push = e.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.2);
            e.push(push.x, 0.4, push.z);
            e.hurtMarked = true;
        }
        var color = SkyAbilities.rgb(Flame.STORM.color);
        for (int ring = 1; ring <= 3; ring++)
            Kits.circle(level, player.position().add(0, 0.3 * ring, 0), r * ring / 3.0, new DustParticleOptions(color, 1.0F), 10 * ring);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }
}
