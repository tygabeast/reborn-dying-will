package com.reborndw.mixin;

import com.reborndw.client.anim.MoveAnimClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Weapon-move poses on top of the normal player animation. Optional: if it misses, moves just play without a pose. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void reborn_dying_will$movePose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                            float netHeadYaw, float headPitch, CallbackInfo ci) {
        // the first-person arm calls this with all zeros; that arm is posed separately
        if (entity instanceof Player p && ageInTicks != 0F) MoveAnimClient.pose((HumanoidModel<?>) (Object) this, p);
    }
}
