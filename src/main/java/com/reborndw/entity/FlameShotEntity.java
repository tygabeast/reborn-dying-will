package com.reborndw.entity;

import com.reborndw.ability.FlameDamage;
import com.reborndw.ability.SkyAbilities;
import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A shot of pure flame: no gravity, a coloured trail, and flame damage (with its flame's property) on hit. */
public class FlameShotEntity extends Projectile {
    private static final EntityDataAccessor<Integer> FLAME = SynchedEntityData.defineId(FlameShotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(FlameShotEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private int lifeTicks = 20;
    private Rank rank = Rank.E;
    private int flags;

    public FlameShotEntity(EntityType<? extends FlameShotEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Fires a shot from the player's eyes along {@code dir}. Range is in blocks. */
    public static FlameShotEntity shoot(ServerPlayer owner, Flame flame, Vec3 dir, double speed, float damage, double range,
                                        Rank rank, int flags, float size) {
        FlameShotEntity shot = new FlameShotEntity(ModRegistry.FLAME_SHOT.get(), owner.level());
        shot.setOwner(owner);
        shot.entityData.set(FLAME, flame.ordinal());
        shot.entityData.set(SIZE, size);
        shot.damage = damage;
        shot.rank = rank;
        shot.flags = flags;
        shot.lifeTicks = (int) Math.ceil(range / speed);
        Vec3 d = dir.normalize();
        Vec3 start = owner.getEyePosition().add(0, -0.15, 0).add(d.scale(0.6));
        shot.setPos(start);
        shot.setDeltaMovement(d.scale(speed));
        shot.addTag(FlameDamage.TAG_NO_SPLIT); // Cloud Spread copies only weapon projectiles
        owner.level().addFreshEntity(shot);
        return shot;
    }

    public void setFlame(Flame flame) { entityData.set(FLAME, flame.ordinal()); }

    public Flame flame() {
        int i = entityData.get(FLAME);
        return Flame.values()[Math.floorMod(i, Flame.values().length)];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLAME, 0);
        builder.define(SIZE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (isRemoved()) return;
            }
            if (--lifeTicks <= 0) { discard(); return; }
        } else {
            com.reborndw.client.ShotTrail.tick(this);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || !(target instanceof LivingEntity living)) return false;
        return !(getOwner() instanceof ServerPlayer owner) || !SkyAbilities.isAlly(owner, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (getOwner() instanceof ServerPlayer owner && result.getEntity() instanceof LivingEntity target)
            FlameDamage.deal(owner, target, flame(), damage, rank, flags);
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!isRemoved()) discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // short-lived; never written to disk
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}
}
