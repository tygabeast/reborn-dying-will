package com.reborndw.menu;

import com.reborndw.registry.ModRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Infusion Anvil screen: item and gem in, infused item out. Inputs are returned when it closes. */
public class InfusionAnvilMenu extends AbstractContainerMenu {
    public static final int ITEM = 0, GEM = 1, OUT = 2;
    private final ContainerLevelAccess access;
    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer output = new ResultContainer();

    public InfusionAnvilMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public InfusionAnvilMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModRegistry.INFUSION_ANVIL_MENU.get(), id);
        this.access = access;
        addSlot(new Slot(inputs, ITEM, 27, 47) {
            @Override public boolean mayPlace(ItemStack s) { return InfusionAnvilLogic.canInfuse(s); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inputs, GEM, 76, 47) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(ModRegistry.FLAME_GEM.get()); }
        });
        addSlot(new Slot(output, 0, 134, 47) {
            @Override public boolean mayPlace(ItemStack s) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                inputs.removeItem(ITEM, 1);
                inputs.removeItem(GEM, 1);
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.8F));
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
            output.setItem(0, InfusionAnvilLogic.result(inputs.getItem(ITEM), inputs.getItem(GEM)));
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
        return stillValid(access, player, ModRegistry.INFUSION_ANVIL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = 3, invEnd = slots.size();
        if (index == OUT) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
            return copy;
        } else if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (slots.get(GEM).mayPlace(stack)) moved = moveItemStackTo(stack, GEM, GEM + 1, false);
            else if (slots.get(ITEM).mayPlace(stack) && !slots.get(ITEM).hasItem()) {
                slots.get(ITEM).setByPlayer(stack.split(1));
                moved = true;
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
