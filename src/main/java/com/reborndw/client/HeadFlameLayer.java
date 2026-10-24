package com.reborndw.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reborndw.client.look.FlameLook;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.client.look.FlameMesh;
import com.reborndw.client.look.FlameRenderTypes;
import com.reborndw.config.ClientConfig;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The flame on a player's forehead. It stands a little in front of the brow (clear of hair and helmets) and turns to face
 * the camera around the head's up axis, leaning partly toward it when seen from above or below, so it never shows a seam
 * or an edge. Earth-set flames add upright bands of script circling the head.
 */
public class HeadFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Where the flame's base sits in head space, in blocks: up from the neck pivot, and forward of it. */
    public static final float BROW_UP = 0.375F, BROW_FORWARD = 0.35F;
    private static final float U = 1F / 48F;
    private static final float OLD_BAND_HEIGHT = 0.27F;
    private static final Matrix4f INV = new Matrix4f();
    private static final Vector3f CAM = new Vector3f();

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
        float s = U * L.sz * form * (float) (double) ClientConfig.FLAME_SCALE.get();
        float up = BROW_UP + (float) (double) ClientConfig.FLAME_RAISE.get();
        if (L.script > 0 && ClientConfig.SCRIPT_RINGS.get()) rings(pose, buffers, L, calm, sec, up + L.height() * .45F * s);
        pose.translate(0F, -up, -(BROW_FORWARD + (float) (double) ClientConfig.FLAME_FORWARD.get()));
        // face the camera. In the world the camera sits at the origin of the render pose (perspective); in a GUI (the
        // inventory doll, orthographic) it looks straight down the screen's z axis instead.
        INV.set(pose.last().pose()).invert();
        if (RenderSystem.getProjectionMatrix().m23() == 0F) INV.transformDirection(CAM.set(0F, 0F, 1F));
        else INV.transformPosition(CAM.set(0F, 0F, 0F));
        pose.mulPose(Axis.YP.rotation((float) Mth.atan2(CAM.x, CAM.z)));
        float elev = (float) Mth.atan2(-CAM.y, Mth.sqrt(CAM.x * CAM.x + CAM.z * CAM.z));
        pose.mulPose(Axis.XP.rotation(elev * .5F));
        pose.scale(s, -s, s);
        boolean compat = FlameRenderTypes.compat();
        FlameMesh.format(compat, false);
        FlameMesh.head(buffers.getBuffer(compat ? FlameRenderTypes.BODY_COMPAT : FlameRenderTypes.BODY), pose.last(), FlameMesh.BODY, L, calm, t, sec);
        FlameMesh.format(compat, compat);
        FlameMesh.head(buffers.getBuffer(compat ? FlameRenderTypes.GLOW_COMPAT : FlameRenderTypes.GLOW), pose.last(), FlameMesh.GLOW, L, calm, t, sec);
        FlameMesh.format(false, false);
        pose.popPose();
    }

    /** Bands of script circling the head, between the brow and the flame's middle, wide enough to clear a helmet's corners. */
    private static void rings(PoseStack pose, MultiBufferSource buffers, FlameLook L, boolean calm, float sec, float height) {
        boolean compat = FlameRenderTypes.compat();
        var vc = buffers.getBuffer(compat ? FlameRenderTypes.SCRIPT_COMPAT : FlameRenderTypes.SCRIPT);
        FlameMesh.format(compat, false);
        float spin = sec * (calm ? .7F : 1.2F);
        int n = L.script;
        int color = FlameMesh.mix(L.core, L.edge, .5F);
        for (int i = 0; i < n; i++) {
            pose.pushPose();
            pose.translate(0F, -(OLD_BAND_HEIGHT + height) * .5F, 0F); // halfway between 0.3.3's height and the flame's middle
            if (n > 1) pose.mulPose(Axis.ZP.rotation(i == 0 ? .28F : -.28F));
            else pose.mulPose(Axis.ZP.rotation(.12F));
            pose.mulPose(Axis.YP.rotation(i == 0 ? spin : -spin));
            FlameMesh.ribbon(vc, pose.last(), .53F, .065F, color, .9F);
            pose.popPose();
        }
        FlameMesh.format(false, false);
    }
}
