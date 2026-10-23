package com.reborndw.block;

import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class ResonatorBlockEntity extends BlockEntity {
    /** A basic resonator accepts gems up to D-rank, so it stops just short of C. */
    public static final long BASIC_FV_CAP = Rank.GEM_FV[Rank.C.ordinal()] - 1;
    private static final double FEED_RANGE = 5.0;

    private ItemStack gem = ItemStack.EMPTY;
    private double fraction;
    private UUID feeder;

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.RESONATOR_BE.get(), pos, state);
    }

    public ItemStack gem() { return gem; }

    public void insert(Player player, ItemStack stack) {
        gem = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        changed();
    }

    public void extract(Player player) {
        if (gem.isEmpty()) return;
        if (!player.getInventory().add(gem)) player.drop(gem, false);
        gem = ItemStack.EMPTY;
        feeder = null;
        changed();
    }

    public void toggleFeeding(Player player) {
        if (gem.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.resonator_empty"), true);
            return;
        }
        if (player.getUUID().equals(feeder)) {
            feeder = null;
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.resonator_stop", status()), true);
            return;
        }
        FlameData d = FlameLogic.get(player);
        if (!d.awakened || d.active == null) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.not_awakened"), true);
            return;
        }
        GemData g = data();
        if (g.flame().isPresent() && g.flame().get() != d.active) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.resonator_wrong_flame",
                    Component.translatable(g.flame().get().translationKey())), true);
            return;
        }
        feeder = player.getUUID();
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.resonator_start", status()), true);
    }

    private Component status() {
        GemData g = data();
        Rank r = g.rank();
        return Component.literal(String.format("%,d FV", g.fv()) + (r == null ? "" : " (" + r.name() + ")"));
    }

    private GemData data() { return gem.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonatorBlockEntity be) {
        boolean lit = !be.gem.isEmpty();
        if (state.getValue(ResonatorBlock.LIT) != lit) level.setBlock(pos, state.setValue(ResonatorBlock.LIT, lit), 3);
        if (be.gem.isEmpty()) return;
        GemData g = be.data();
        if (g.fv() >= BASIC_FV_CAP) { be.feeder = null; return; }

        double add = 0;
        if (g.flame().isPresent()) add += ServerConfig.RESONATOR_AMBIENT.get() / 20.0;

        if (be.feeder != null && level instanceof ServerLevel sl) {
            ServerPlayer p = sl.getPlayerByUUID(be.feeder) instanceof ServerPlayer sp ? sp : null;
            if (p == null || p.level() != level || p.distanceToSqr(pos.getCenter()) > FEED_RANGE * FEED_RANGE) {
                be.feeder = null;
            } else {
                FlameData d = FlameLogic.get(p);
                double want = ServerConfig.RESONATOR_TRANSFER.get() / 20.0;
                double give = Math.min(want, d.pool);
                if (d.active == null || (g.flame().isPresent() && g.flame().get() != d.active) || give <= 0) {
                    if (give <= 0 && level.getGameTime() % 40 == 0)
                        p.displayClientMessage(Component.translatable("message.reborn_dying_will.empty_pool"), true);
                } else if (FlameLogic.spend(p, give, false)) {
                    add += give;
                    if (g.flame().isEmpty()) g = new GemData(Optional.of(d.active), g.fv());
                    if (level.getGameTime() % 4 == 0) {
                        int c = d.active.color;
                        sl.sendParticles(new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.0F),
                                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0);
                    }
                }
            }
        }

        be.fraction += add;
        long whole = (long) be.fraction;
        if (whole > 0 || !g.equals(be.data())) {
            be.fraction -= whole;
            Rank before = be.data().rank();
            GemData updated = g.withFv(Math.min(BASIC_FV_CAP, g.fv() + whole));
            be.gem.set(ModRegistry.GEM.get(), updated);
            Rank after = updated.rank();
            if (after != before) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
                be.changed();
            } else if (level.getGameTime() % 20 == 0) {
                be.changed();
            } else {
                be.setChanged();
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!gem.isEmpty()) tag.put("gem", gem.save(registries));
        tag.putDouble("fraction", fraction);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gem = tag.contains("gem") ? ItemStack.parse(registries, tag.getCompound("gem")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        fraction = tag.getDouble("fraction");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!gem.isEmpty()) tag.put("gem", gem.save(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @SuppressWarnings("unused")
    private static final Flame UNUSED = null;
}
