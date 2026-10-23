package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.util.DelayedTasks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Cloud: Propagation. Passive (in FlameDamage): Cloud hits can echo for half damage. */
public class CloudKit implements FlameKit {
    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Cloud Spread: your next projectile, from any weapon, splits into three. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (d.cloudSpreadTicks > 0) return;
        if (!Kits.spend(player, KitConfig.SPREAD_COST.get())) return;
        d.cloudSpreadTicks = KitConfig.SPREAD_ARMED_SECONDS.get() * 20;
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.cloud_spread_ready"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** Needle Burst: flame needles fly in every direction, each able to echo. */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.NEEDLE_COST.get())) return;
        int n = KitConfig.NEEDLE_COUNT.get();
        float damage = FlameDamage.scaled(KitConfig.NEEDLE_DAMAGE.get(), eff);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double pitch = (i % 3 - 1) * 0.25; // three bands: slightly down, level, slightly up
            Vec3 dir = new Vec3(Math.cos(a), pitch, Math.sin(a));
            FlameShotEntity.shoot(player, Flame.CLOUD, dir, 1.6, damage, 10, eff, 0, 0.7F);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.8F);
    }

    @Override
    public void tick(ServerPlayer player, FlameData d, Rank eff) {
        if (d.cloudSpreadTicks > 0) d.cloudSpreadTicks--;
    }

    @Override
    public void onDeactivate(ServerPlayer player, FlameData d) {
        d.cloudSpreadTicks = 0;
    }

    /** Called when a projectile enters the world: if its owner armed Cloud Spread, add two copies 10 degrees out. */
    public static void onProjectileSpawn(Projectile projectile) {
        if (!(projectile.getOwner() instanceof ServerPlayer owner) || projectile.getTags().contains(FlameDamage.TAG_NO_SPLIT)) return;
        FlameData d = FlameLogic.get(owner);
        if (d.cloudSpreadTicks <= 0 || d.active != Flame.CLOUD) return;
        d.cloudSpreadTicks = 0;
        projectile.addTag(FlameDamage.TAG_NO_SPLIT);
        CompoundTag saved = projectile.saveWithoutId(new CompoundTag());
        Vec3 pos = projectile.position();
        Vec3 motion = projectile.getDeltaMovement();
        ServerLevel level = (ServerLevel) projectile.level();
        // spawn after this tick: adding entities from inside the join event isn't safe
        DelayedTasks.schedule(0, () -> {
            for (int side : new int[]{-1, 1}) {
                Entity copy = projectile.getType().create(level);
                if (copy == null) continue;
                CompoundTag tag = saved.copy();
                tag.remove("UUID");
                copy.load(tag);
                copy.setUUID(java.util.UUID.randomUUID());
                copy.setPos(pos);
                copy.setDeltaMovement(motion.yRot((float) Math.toRadians(10 * side)));
                copy.addTag(FlameDamage.TAG_NO_SPLIT);
                if (copy instanceof Projectile p) p.setOwner(owner);
                if (copy instanceof AbstractArrow arrow) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY; // no free arrows
                level.addFreshEntity(copy);
            }
            level.sendParticles(new DustParticleOptions(SkyAbilities.rgb(Flame.CLOUD.color), 1.0F), pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2, 0);
        });
    }
}
