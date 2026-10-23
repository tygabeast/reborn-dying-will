package com.reborndw.entity;

import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameLogic;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A Dying Will Bullet. It does no damage: a player it hits enters the Dying Will state; a mob gets a short frenzy. */
public class DyingWillBulletEntity extends ThrowableItemProjectile {
    public DyingWillBulletEntity(EntityType<? extends DyingWillBulletEntity> type, Level level) { super(type, level); }

    public DyingWillBulletEntity(Level level, LivingEntity shooter) {
        super(ModRegistry.DYING_WILL_BULLET_ENTITY.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModRegistry.DYING_WILL_BULLET.get(); }

    @Override
    protected double getDefaultGravity() { return 0.005; }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 60) discard();
        if (level() instanceof ServerLevel sl && tickCount % 2 == 0)
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        if (hit.getEntity() instanceof ServerPlayer target) {
            boolean pvp = getOwner() instanceof ServerPlayer shooter && shooter != target;
            if (pvp && !ServerConfig.BULLET_PVP.get()) return;
            String fail = FlameLogic.tryDyingWillEntry(target, false);
            if (fail != null && getOwner() instanceof ServerPlayer shooter)
                shooter.displayClientMessage(Component.translatable(fail), true);
        } else if (hit.getEntity() instanceof LivingEntity mob) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 20, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) discard();
    }

    @SuppressWarnings("unused")
    private static final Flame VISUAL = Flame.SKY;
}
