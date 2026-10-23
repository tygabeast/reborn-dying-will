package com.reborndw.item;

import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A ring with a flame and grade. Worn in the off-hand, a Curios ring slot, or (chained) the necklace slot. */
public class FlameRingItem extends Item {
    private final boolean chained;

    public FlameRingItem(Properties props, boolean chained) {
        super(props);
        this.chained = chained;
    }

    public boolean chained() { return chained; }

    @Override
    public Component getName(ItemStack stack) {
        RingData r = stack.get(ModRegistry.RING.get());
        if (r == null) return super.getName(stack);
        return Component.translatable(chained ? "item.reborn_dying_will.chained_flame_ring.named" : "item.reborn_dying_will.flame_ring.named",
                Component.translatable(r.flame().translationKey()), r.grade().name()).withColor(r.flame().color);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        RingData r = stack.get(ModRegistry.RING.get());
        if (r != null) {
            tooltip.add(Component.translatable("tooltip.reborn_dying_will.ring_grade", r.grade().name()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.reborn_dying_will.ring_property", r.flame().property).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable(chained ? "tooltip.reborn_dying_will.ring_wear_chained" : "tooltip.reborn_dying_will.ring_wear")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
