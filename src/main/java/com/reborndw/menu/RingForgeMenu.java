package com.reborndw.menu;

import com.reborndw.registry.ModRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The Ring Forge screen: metal, gem and chain in, ring out. Items left in the inputs are returned when it closes. */
public class RingForgeMenu extends AbstractContainerMenu {
    public static final int METAL = 0, GEM = 1, CHAIN = 2, OUT = 3;
    private final ContainerLevelAccess access;
    private final SimpleContainer inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer output = new ResultContainer();
    private RingForgeLogic.Result current = RingForgeLogic.result(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

    public RingForgeMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public RingForgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModRegistry.RING_FORGE_MENU.get(), id);
        this.access = access;
        addSlot(new Slot(inputs, METAL, 27, 47) {
            @Override public boolean mayPlace(ItemStack s) { return RingForgeLogic.isIron(s) || s.is(ModRegistry.BLANK_RING.get()) || s.is(ModRegistry.FLAME_RING.get()); }
        });
        addSlot(new Slot(inputs, GEM, 76, 47) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(ModRegistry.FLAME_GEM.get()); }
        });
        addSlot(new Slot(inputs, CHAIN, 76, 21) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Items.CHAIN); }
        });
        addSlot(new Slot(output, 0, 134, 47) {
            @Override public boolean mayPlace(ItemStack s) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                RingForgeLogic.Result r = current;
                inputs.removeItem(METAL, r.metal());
                inputs.removeItem(GEM, r.gem());
                inputs.removeItem(CHAIN, r.chain());
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.3F));
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == inputs) {
            current = RingForgeLogic.result(inputs.getItem(METAL), inputs.getItem(GEM), inputs.getItem(CHAIN));
            output.setItem(0, current.output().copy());
            broadcastChanges();
        }
        super.slotsChanged(container);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.RING_FORGE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = 4, invEnd = slots.size();
        if (index == OUT) {
            // take as many rings as the inputs allow
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
            return copy;
        } else if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < OUT && !moved; i++) {
                if (slots.get(i).mayPlace(stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
