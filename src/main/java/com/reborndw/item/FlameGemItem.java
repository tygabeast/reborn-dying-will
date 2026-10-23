package com.reborndw.item;

import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FlameGemItem extends Item {
    public FlameGemItem(Properties props) { super(props); }

    public static ItemStack create(Flame flame, long fv) {
        ItemStack s = new ItemStack(ModRegistry.FLAME_GEM.get());
        s.set(ModRegistry.GEM.get(), new GemData(java.util.Optional.ofNullable(flame), fv));
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        GemData g = stack.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
        if (g.flame().isEmpty()) return Component.translatable("item.reborn_dying_will.flame_gem.unattuned");
        Flame f = g.flame().get();
        Rank r = g.rank();
        Component flame = Component.translatable(f.translationKey());
        return (r == null
                ? Component.translatable("item.reborn_dying_will.flame_gem.attuned", flame)
                : Component.translatable("item.reborn_dying_will.flame_gem.ranked", r.name(), flame)).withColor(f.color);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        GemData g = stack.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
        return g.rank() != null && g.rank().atLeast(Rank.B);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        GemData g = stack.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
        tooltip.add(Component.translatable("tooltip.reborn_dying_will.gem_fv", String.format("%,d", g.fv())).withStyle(ChatFormatting.GRAY));
        Rank r = g.rank();
        Rank next = r == null ? Rank.E : r.next();
        if (next != null) tooltip.add(Component.translatable("tooltip.reborn_dying_will.gem_next", next.name(),
                String.format("%,d", Rank.GEM_FV[next.ordinal()])).withStyle(ChatFormatting.DARK_GRAY));
        if (g.flame().isEmpty()) tooltip.add(Component.translatable("tooltip.reborn_dying_will.gem_unattuned").withStyle(ChatFormatting.DARK_GRAY));
    }
}
