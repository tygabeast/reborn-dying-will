package com.reborndw.item;

import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Form;
import com.reborndw.util.DelayedTasks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A flame-specific Dying Will Pill. For an awakened player close to death, it awakens one more flame (and the
 * Dying Will state with it). It does nothing at higher health, so it stays a desperate gamble, as the first pill is.
 */
public class FlamePillItem extends DyingWillPillItem {
    private final Flame flame;

    public FlamePillItem(Properties props, Flame flame) {
        super(props);
        this.flame = flame;
    }

    public Flame flame() { return flame; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp)) return stack;
        FlameData d = FlameLogic.get(sp);
        String fail = null;
        if (!d.awakened) fail = "message.reborn_dying_will.flame_pill_unawakened";
        else if (d.unlocked.contains(flame)) fail = "message.reborn_dying_will.flame_pill_known";
        else if (sp.getHealth() > sp.getMaxHealth() * ServerConfig.LOW_HEALTH_FRACTION.get()) fail = "message.reborn_dying_will.not_desperate";
        if (fail != null) {
            sp.displayClientMessage(Component.translatable(fail, name()), true);
            return stack;
        }
        FlameLogic.unlock(sp, flame);
        sp.displayClientMessage(Component.translatable("message.reborn_dying_will.flame_unlocked", name()).withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.7F);
        if (d.form == Form.NONE) {
            // the newly awakened flame takes over, with a full pool, and the Dying Will that follows is in its colour
            d.active = flame;
            d.pool = d.usableCapacity(flame);
            d.shieldActive = false;
            d.markDirty();
            FlameLogic.syncNow(sp);
            DelayedTasks.schedule(1, () -> {
                if (sp.isAlive() && FlameLogic.get(sp).active == flame) FlameLogic.tryDyingWillEntry(sp, true);
            });
        } else {
            sp.displayClientMessage(Component.translatable("message.reborn_dying_will.flame_pill_in_form", name()), true);
            FlameLogic.syncNow(sp);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    private Component name() {
        return Component.translatable(flame.translationKey()).withColor(flame.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reborn_dying_will.flame_pill", name()).withStyle(ChatFormatting.GRAY));
    }
}
