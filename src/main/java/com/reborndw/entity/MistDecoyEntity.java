package com.reborndw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Mist's Decoy: an illusory copy of its caster that mobs go after. It stands still and fades after a few seconds. */
public class MistDecoyEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MistDecoyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int life = 160;

    public MistDecoyEntity(EntityType<? extends MistDecoyEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** Places a copy of the player where they stand, wearing what they wear. */
    public void copyOf(ServerPlayer owner, int lifeTicks) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        life = lifeTicks;
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYHeadRot(owner.getYHeadRot());
        setYBodyRot(owner.yBodyRot);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            setItemSlot(slot, owner.getItemBySlot(slot).copy());
            setDropChance(slot, 0.0F);
        }
        setCustomName(owner.getName());
        setCustomNameVisible(false);
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl) {
            ServerPlayer owner = owner().map(u -> sl.getServer().getPlayerList().getPlayer(u)).orElse(null);
            if (--life <= 0 || owner == null || !owner.isAlive() || owner.level() != level() || owner.distanceToSqr(this) > 24 * 24) vanish();
        }
    }

    public void vanish() {
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        discard();
    }

    @Override
    public void die(DamageSource source) {
        vanish();
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // an illusion doesn't survive a reload
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {}
}
