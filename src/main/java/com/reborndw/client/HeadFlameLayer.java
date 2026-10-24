package com.reborndw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reborndw.RebornDW;
import com.reborndw.client.look.FlameLook;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.client.look.FlameMesh;
import com.reborndw.config.ClientConfig;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** The flame on a player's forehead: two crossed, camera-proof planes of tapered tongues, plus script rings for the Earth set. */
public class HeadFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = RebornDW.id("textures/entity/flame_white.png");
    private static final ResourceLocation SCRIPT = RebornDW.id("textures/entity/script_ring.png");
    private static final float U = 1F / 48F;

    public HeadFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if (!ClientConfig.FLAMES_SHOW.get() || p.isInvisible() || p.isSpectator()) return;
        FlameData d = ClientFlameCache.get(p.getId());
        if (d == null || d.active == null || d.form == Form.NONE) return;
        if (p.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > 64 * 64) return;
        FlameLook L = FlameLooks.of(d.active);
        boolean calm = d.form != Form.DYING_WILL;
        float sec = age / 20F, t = calm ? sec * .5F : sec;
        float form = d.form == Form.ULTIMATE ? 1.3F : 1F;

        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        if (L.script > 0 && ClientConfig.SCRIPT_RINGS.get()) rings(pose, buffers, L, calm, sec);
        pose.translate(0F, -0.455F + (float) (double) ClientConfig.FLAME_HEIGHT.get(), -0.27F);
        float s = U * L.sz * form * (float) (double) ClientConfig.FLAME_SCALE.get();
        pose.scale(s, -s, s);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        FlameMesh.head(vc, pose.last(), L, calm, t, sec);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        FlameMesh.head(vc, pose.last(), L, calm, t + 1.7F, sec + .37F);
        pose.popPose();
    }

    private static void rings(PoseStack pose, MultiBufferSource buffers, FlameLook L, boolean calm, float sec) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(SCRIPT));
        float spin = sec * (calm ? .9F : 1.6F);
        int n = L.script;
        for (int i = 0; i < n; i++) {
            pose.pushPose();
            pose.translate(0F, -0.3F, 0F);
            if (n > 1) pose.mulPose(Axis.ZP.rotation(i == 0 ? .5F : -.5F));
            pose.mulPose(Axis.YP.rotation(i == 0 ? spin : -spin));
            FlameMesh.ring(vc, pose.last(), .62F, .085F, L.edge, .85F);
            pose.popPose();
        }
    }
}
