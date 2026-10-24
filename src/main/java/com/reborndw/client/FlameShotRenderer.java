package com.reborndw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reborndw.RebornDW;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.client.look.FlameMesh;
import com.reborndw.client.look.FlameRenderTypes;
import com.reborndw.entity.FlameShotEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A flame shot: a glowing body facing the camera with a comet tail pointing back along its flight; motes do the trail. */
public class FlameShotRenderer extends EntityRenderer<FlameShotEntity> {
    private static final ResourceLocation WHITE = RebornDW.id("textures/entity/flame_white.png");
    private static final Quaternionf Q = new Quaternionf();
    private static final Vector3f V = new Vector3f();

    public FlameShotRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(FlameShotEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        var look = FlameLooks.of(e.flame());
        Quaternionf cam = entityRenderDispatcher.cameraOrientation();
        // flight direction in the camera's frame: its x/y give the tail's angle on screen
        var m = e.getDeltaMovement();
        V.set((float) -m.x, (float) -m.y, (float) -m.z);
        float len = V.length();
        float tailAngle = 0F, tailLen = 0F;
        if (len > 1e-4F) {
            V.div(len).rotate(Q.set(cam).conjugate());
            float sxy = Mth.sqrt(V.x * V.x + V.y * V.y);
            tailLen = Mth.clamp(sxy, 0F, 1F);
            tailAngle = (float) Mth.atan2(-V.x, V.y);
        }
        pose.pushPose();
        pose.translate(0F, e.getBbHeight() * .5F, 0F);
        pose.mulPose(cam);
        float s = look.sz / 48F;
        pose.scale(s, s, s);
        float sec = (e.tickCount + partial) / 20F;
        boolean compat = FlameRenderTypes.compat();
        FlameMesh.format(compat, false);
        FlameMesh.shot(buffers.getBuffer(compat ? FlameRenderTypes.BODY_COMPAT : FlameRenderTypes.BODY), pose.last(), FlameMesh.BODY, look, sec, tailAngle, tailLen);
        FlameMesh.format(compat, compat);
        FlameMesh.shot(buffers.getBuffer(compat ? FlameRenderTypes.GLOW_COMPAT : FlameRenderTypes.GLOW), pose.last(), FlameMesh.GLOW, look, sec, tailAngle, tailLen);
        FlameMesh.format(false, false);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FlameShotEntity e) { return WHITE; }
}
