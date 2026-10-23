package com.reborndw.item;

import com.reborndw.flame.FlameLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Enters the Dying Will state. The very first one only works at low health. */
public class DyingWillPillItem extends Item {
    public DyingWillPillItem(Properties props) { super(props); }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 12; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            String fail = FlameLogic.tryDyingWillEntry(sp, true);
            if (fail != null) {
                sp.displayClientMessage(Component.translatable(fail), true);
                return stack;
            }
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reborn_dying_will.pill").withStyle(ChatFormatting.GRAY));
    }
}
