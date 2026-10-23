package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.entity.MistDecoyEntity;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mist: Construction. Passive: mobs notice you from closer (KitEvents), Mist hits can confuse (FlameDamage),
 * Mist shots are nearly invisible (FlameShotEntity).
 */
public class MistKit implements FlameKit {
    private static final Map<UUID, MistDecoyEntity> DECOYS = new HashMap<>();

    @Override public Rank primaryRank() { return Rank.D; }
    @Override public Rank secondaryRank() { return Rank.C; }

    /** Decoy: an illusory copy of you that mobs target. */
    @Override
    public void primary(ServerPlayer player, FlameData d, Rank eff) {
        if (!Kits.spend(player, KitConfig.DECOY_COST.get())) return;
        ServerLevel level = player.serverLevel();
        MistDecoyEntity old = DECOYS.remove(player.getUUID());
        if (old != null && !old.isRemoved()) old.vanish();
        MistDecoyEntity decoy = ModRegistry.MIST_DECOY.get().create(level);
        if (decoy == null) return;
        decoy.copyOf(player, KitConfig.DECOY_SECONDS.get() * 20);
        level.addFreshEntity(decoy);
        DECOYS.put(player.getUUID(), decoy);
        // mobs already after you switch to the decoy
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.getTarget() == player))
            mob.setTarget(decoy);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 25, 0.4, 0.7, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** The living decoy for this player, if any. */
    public static MistDecoyEntity decoyOf(ServerPlayer player) {
        MistDecoyEntity d = DECOYS.get(player.getUUID());
        if (d == null) return null;
        if (d.isRemoved() || !d.isAlive()) { DECOYS.remove(player.getUUID()); return null; }
        return d;
    }

    /**
     * Phantom Terrain: real illusions. Look at the ground for a wall (4 wide, 2 high) across your view;
     * look ahead or out over a drop for a bridge from your feet.
     */
    @Override
    public void secondary(ServerPlayer player, FlameData d, Rank eff) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> spots = terrainSpots(player);
        BlockState phantom = ModRegistry.PHANTOM_BLOCK.get().defaultBlockState();
        List<BlockPos> ok = new ArrayList<>();
        for (BlockPos p : spots) {
            if (ok.size() >= KitConfig.TERRAIN_MAX_BLOCKS.get()) break;
            if (level.getBlockState(p).isAir() && level.mayInteract(player, p) && level.isUnobstructed(phantom, p, CollisionContext.empty()))
                ok.add(p);
        }
        if (ok.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.no_room"), true);
            return;
        }
        if (!Kits.spend(player, KitConfig.TERRAIN_COST.get() + KitConfig.TERRAIN_COST_PER_BLOCK.get() * ok.size())) return;
        for (BlockPos p : ok) {
            level.setBlock(p, phantom, 3);
            level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    static List<BlockPos> terrainSpots(ServerPlayer player) {
        List<BlockPos> out = new ArrayList<>();
        Direction facing = player.getDirection();
        HitResult hit = player.pick(12, 0, false);
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK && bh.getDirection() == Direction.UP
                && bh.getBlockPos().distSqr(player.blockPosition()) > 4) {
            BlockPos base = bh.getBlockPos().above();
            Direction side = facing.getClockWise();
            for (int h = 0; h < 2; h++)
                for (int w = -2; w < 2; w++) out.add(base.relative(side, w).above(h));
        } else {
            BlockPos start = player.blockPosition().below().relative(facing);
            for (int i = 0; i < 16; i++) out.add(start.relative(facing, i));
        }
        return out;
    }
}
