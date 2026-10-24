package com.reborndw.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reborndw.RebornDW;
import com.reborndw.anim.MoveAnim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Plays weapon-move animations: model poses (via the mixin), whole-body spins and leans, and the first-person hand. */
@EventBusSubscriber(modid = RebornDW.MODID, value = Dist.CLIENT)
public final class MoveAnimClient {
    private static final float[] FP = new float[6];
    private static final boolean[] PUSHED = new boolean[8];
    private static int depth;

    private MoveAnimClient() {}

    private static float partial() { return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false); }

    /** Called from the model mixin after the normal animation. */
    public static void pose(HumanoidModel<?> model, Player p) {
        MoveAnims.Playing pl = MoveAnims.get(p);
        if (pl == null) return;
        float t = MoveAnims.progress(pl, p, partial());
        if (t < 0F) return;
        MovePoses.apply(model, pl.anim(), MoveAnims.arm(pl, p), t);
    }

    @SubscribeEvent
    public static void preRender(RenderPlayerEvent.Pre event) {
        int d = depth++;
        if (d >= PUSHED.length) return;
        PUSHED[d] = false;
        Player p = event.getEntity();
        MoveAnims.Playing pl = MoveAnims.get(p);
        if (pl == null) return;
        float t = MoveAnims.progress(pl, p, event.getPartialTick());
        if (t < 0F) return;
        float spin = MovePoses.spin(pl.anim(), t), lean = MovePoses.lean(pl.anim(), t);
        if (spin == 0F && lean == 0F) return;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        PUSHED[d] = true;
        float side = MoveAnims.arm(pl, p) == HumanoidArm.RIGHT ? 1F : -1F;
        if (spin != 0F) ps.mulPose(Axis.YP.rotation(-spin * side));
        if (lean != 0F) { // lean forward over the feet, in the direction the body faces
            float body = Mth.rotLerp(event.getPartialTick(), p.yBodyRotO, p.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(-body));
            ps.mulPose(Axis.XP.rotationDegrees(lean));
            ps.mulPose(Axis.YP.rotationDegrees(body));
        }
    }

    @SubscribeEvent
    public static void postRender(RenderPlayerEvent.Post event) {
        int d = --depth;
        if (d < 0) { depth = 0; return; }
        if (d < PUSHED.length && PUSHED[d]) { event.getPoseStack().popPose(); PUSHED[d] = false; }
    }

    /** First person: the moving hand is drawn here with the move's offset; the other hand draws as usual. */
    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        MoveAnims.Playing pl = MoveAnims.get(p);
        if (pl == null || event.getHand() != MoveAnims.hand(pl)) return;
        float t = MoveAnims.progress(pl, p, event.getPartialTick());
        if (t < 0F) return;
        MoveAnim a = pl.anim();
        HumanoidArm arm = MoveAnims.arm(pl, p);
        float side = arm == HumanoidArm.RIGHT ? 1F : -1F, w = MovePoses.weight(a, t);
        MovePoses.firstPerson(a, t, FP);
        event.setCanceled(true);
        PoseStack ps = event.getPoseStack();
        var renderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
        var stack = event.getItemStack();
        ps.pushPose();
        if (stack.isEmpty()) {
            ps.translate(FP[0] * side * w, FP[1] * w, FP[2] * w);
            renderer.renderPlayerArm(ps, event.getMultiBufferSource(), event.getPackedLight(), 0F, 0F, arm);
        } else {
            ps.translate(side * 0.56F, -0.52F, -0.72F); // where the game holds an item in first person
            if (a == MoveAnim.BOW) bow(ps, side, t, w);
            else {
                ps.translate(FP[0] * side * w, FP[1] * w, FP[2] * w);
                ps.mulPose(Axis.YP.rotationDegrees(FP[4] * side * w));
                ps.mulPose(Axis.XP.rotationDegrees(FP[3] * w));
                ps.mulPose(Axis.ZP.rotationDegrees(FP[5] * side * w));
            }
            renderer.renderItem(p, stack, side > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    side < 0, ps, event.getMultiBufferSource(), event.getPackedLight());
        }
        ps.popPose();
    }

    /** The game's own bow-drawing hold, pulled back over the draw and let go at the end. */
    private static void bow(PoseStack ps, float side, float t, float w) {
        float draw = t > .8F ? 0F : Mth.clamp(t / .7F, 0F, 1F);
        ps.translate(side * -0.2785682F * w, 0.18344387F * w, 0.15731531F * w);
        ps.mulPose(Axis.XP.rotationDegrees(-13.935F * w));
        ps.mulPose(Axis.YP.rotationDegrees(side * 35.3F * w));
        ps.mulPose(Axis.ZP.rotationDegrees(side * -9.785F * w));
        ps.translate(0F, 0F, draw * 0.04F);
        ps.scale(1F, 1F, 1F + draw * 0.2F);
        ps.mulPose(Axis.YN.rotationDegrees(side * 45F * w));
    }
}
