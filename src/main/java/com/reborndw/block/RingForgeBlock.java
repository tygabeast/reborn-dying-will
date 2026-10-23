package com.reborndw.block;

import com.reborndw.menu.RingForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Ring Forge: all ring work happens here. In this version: forging Flame Rings and putting rings on chains. */
public class RingForgeBlock extends Block {
    private static final Component TITLE = Component.translatable("container.reborn_dying_will.ring_forge");

    public RingForgeBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new RingForgeMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
