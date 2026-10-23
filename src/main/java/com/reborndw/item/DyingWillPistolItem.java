package com.reborndw.item;

import com.reborndw.entity.DyingWillBulletEntity;
import com.reborndw.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Fires Dying Will Bullets from the inventory. */
public class DyingWillPistolItem extends Item {
    public DyingWillPistolItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
            return InteractionResultHolder.fail(gun);
        }
        if (!level.isClientSide) {
            DyingWillBulletEntity bullet = new DyingWillBulletEntity(level, player);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 0.5F);
            level.addFreshEntity(bullet);
            if (!player.getAbilities().instabuild) {
                ammo.shrink(1);
                gun.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9F, 1.8F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(gun, level.isClientSide);
    }

    private static ItemStack findAmmo(Player player) {
        if (player.getOffhandItem().is(ModRegistry.DYING_WILL_BULLET.get())) return player.getOffhandItem();
        for (ItemStack s : player.getInventory().items) if (s.is(ModRegistry.DYING_WILL_BULLET.get())) return s;
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reborn_dying_will.pistol").withStyle(ChatFormatting.GRAY));
    }
}
