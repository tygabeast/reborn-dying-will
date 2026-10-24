package com.reborndw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reborndw.RebornDW;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.client.look.FlameMesh;
import com.reborndw.entity.FlameShotEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A flame shot: a glowing body that always faces the camera. The trail is motes, thrown from the entity's tick. */
public class FlameShotRenderer extends EntityRenderer<FlameShotEntity> {
    private static final ResourceLocation WHITE = RebornDW.id("textures/entity/flame_white.png");

    public FlameShotRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(FlameShotEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var look = FlameLooks.of(e.flame());
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = look.sz / 48F;
        pose.scale(s, s, s);
        FlameMesh.shot(buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), pose.last(), look, (e.tickCount + partial) / 20F);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FlameShotEntity e) { return WHITE; }
}
