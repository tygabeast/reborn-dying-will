package com.reborndw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reborndw.block.ResonatorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the gem floating and spinning above the resonator. */
public class ResonatorRenderer implements BlockEntityRenderer<ResonatorBlockEntity> {
    public ResonatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ResonatorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack gem = be.gem();
        if (gem.isEmpty() || be.getLevel() == null) return;
        float t = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.25 + Math.sin(t / 10.0) * 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(t * 3 % 360));
        pose.scale(0.6F, 0.6F, 0.6F);
        Minecraft.getInstance().getItemRenderer().renderStatic(gem, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY,
                pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }
}
