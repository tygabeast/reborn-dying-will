package com.reborndw.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reborndw.client.InfusionGlint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Flame-coloured glints on infused items (see {@link InfusionGlint}). Every hook is optional: if one misses, items keep the vanilla glint. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void reborn_dying_will$begin(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                         int light, int overlay, BakedModel model, CallbackInfo ci) {
        InfusionGlint.begin(stack);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void reborn_dying_will$end(ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                       int light, int overlay, BakedModel model, CallbackInfo ci) {
        InfusionGlint.end();
    }

    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD"), require = 0)
    private void reborn_dying_will$holder(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose,
                                          MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo ci) {
        InfusionGlint.holder(entity);
    }

    @Inject(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("RETURN"), require = 0)
    private void reborn_dying_will$holderEnd(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand, PoseStack pose,
                                             MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo ci) {
        InfusionGlint.holder(null);
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true, require = 0)
    private static void reborn_dying_will$foil(MultiBufferSource buffers, RenderType type, boolean isItem, boolean glint,
                                               CallbackInfoReturnable<VertexConsumer> cir) {
        VertexConsumer v = InfusionGlint.buffer(buffers, type);
        if (v != null) cir.setReturnValue(v);
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true, require = 0)
    private static void reborn_dying_will$foilDirect(MultiBufferSource buffers, RenderType type, boolean noEntity, boolean glint,
                                                     CallbackInfoReturnable<VertexConsumer> cir) {
        VertexConsumer v = InfusionGlint.buffer(buffers, type);
        if (v != null) cir.setReturnValue(v);
    }
}
