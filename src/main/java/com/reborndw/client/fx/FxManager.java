package com.reborndw.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reborndw.client.look.FlameEmitter;
import com.reborndw.client.look.FlameLook;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.client.look.FlameMesh;
import com.reborndw.client.look.FlameRenderTypes;
import com.reborndw.flame.Flame;
import com.reborndw.fx.FlameFx;
import com.reborndw.network.FlameFxPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays the flame effects the server sends ({@link FlameFx}): short-lived shapes drawn after particles, in each flame's
 * colours and style, plus a burst of that flame's motes. At most {@link #CAP} effects live at once (the oldest go first),
 * each lasts under a second, and nothing is drawn when the list is empty.
 */
public final class FxManager {
    private static final int CAP = 192;

    private static final class Fx {
        int kind, extra, age, life, seed;
        Flame flame;
        FlameLook look;
        double x, y, z;
        float a, b, c, size;
    }

    private enum Style { STAR, CROSS, RIPPLE, RAYS, BOLTS, DOUBLE, SWIRL, COLLAPSE }

    private static final List<Fx> FX = new ArrayList<>();
    private static final Quaternionf Q = new Quaternionf();
    private static final Matrix3f M = new Matrix3f();
    private static final Vector3f V0 = new Vector3f(), V1 = new Vector3f(), V2 = new Vector3f();

    private FxManager() {}

    private static Style style(Flame f) {
        return switch (f) {
            case SKY, EARTH -> Style.STAR;
            case STORM, SWAMP -> Style.CROSS;
            case RAIN, RIVER -> Style.RIPPLE;
            case SUN, FOREST -> Style.RAYS;
            case LIGHTNING, MOUNTAIN -> Style.BOLTS;
            case CLOUD, GLACIER -> Style.DOUBLE;
            case MIST, DESERT -> Style.SWIRL;
            case NIGHT -> Style.COLLAPSE;
        };
    }

    private static int life(int kind) {
        return switch (kind) {
            case FlameFx.HIT -> 7; case FlameFx.BOLT -> 6; case FlameFx.SLASH -> 8; case FlameFx.THRUST -> 7;
            case FlameFx.SLAM -> 12; case FlameFx.TETHER -> 9; case FlameFx.CHARGE -> 7; case FlameFx.SHIELD -> 9;
            case FlameFx.BURST -> 9; default -> 4;
        };
    }

    public static void add(FlameFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (cam.distanceToSqr(p.x(), p.y(), p.z()) > 64 * 64) return;
        if (FX.size() >= CAP) FX.remove(0);
        Fx f = new Fx();
        f.kind = p.kind(); f.extra = p.extra();
        f.flame = Flame.values()[Math.floorMod(p.flame(), Flame.values().length)];
        f.look = FlameLooks.of(f.flame);
        f.x = p.x(); f.y = p.y(); f.z = p.z(); f.a = p.a(); f.b = p.b(); f.c = p.c(); f.size = p.size();
        f.life = life(f.kind);
        f.seed = mc.level.random.nextInt(1 << 20);
        motes(mc, f);
        FX.add(f);
    }

    public static void tick() {
        if (FX.isEmpty()) return;
        FX.removeIf(f -> ++f.age >= f.life);
    }

    public static void clear() { FX.clear(); }

    private static void motes(Minecraft mc, Fx f) {
        Vec3 at = new Vec3(f.x, f.y, f.z);
        switch (f.kind) {
            case FlameFx.HIT -> FlameEmitter.burst(mc, f.look, at, Math.round(6 * f.size), .06F);
            case FlameFx.BURST -> FlameEmitter.burst(mc, f.look, at, Math.round(14 * f.size), .1F);
            case FlameFx.SLAM -> {
                for (int i = 0; i < 16; i++) {
                    double ang = i * Math.PI * 2 / 16;
                    FlameEmitter.burst(mc, f.look, at.add(Math.cos(ang) * f.size * .6, .1, Math.sin(ang) * f.size * .6), 1, .08F);
                }
            }
            case FlameFx.BOLT, FlameFx.TETHER, FlameFx.THRUST -> {
                Vec3 b = new Vec3(f.a, f.b, f.c);
                for (int i = 1; i <= 4; i++) FlameEmitter.burst(mc, f.look, at.lerp(b, i / 4.0), 1, .04F);
            }
            case FlameFx.SLASH -> FlameEmitter.burst(mc, f.look, at.add(new Vec3(f.a, 0, f.c).normalize().scale(f.size * .8)), 8, .07F);
            case FlameFx.SHIELD -> FlameEmitter.burst(mc, f.look, at, 8, .05F);
            case FlameFx.MUZZLE -> FlameEmitter.burst(mc, f.look, at, 5, .08F);
            case FlameFx.CHARGE -> FlameEmitter.burst(mc, f.look, at, 1, .03F);
            default -> {}
        }
    }

    // ------------------------------------------------------------------ rendering

    public static void render(RenderLevelStageEvent event) {
        if (FX.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        Camera cam = event.getCamera();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean compat = FlameRenderTypes.compat();
        PoseStack ps = new PoseStack();
        for (int pass = FlameMesh.BODY; pass <= FlameMesh.GLOW; pass++) {
            RenderType type = compat ? (pass == FlameMesh.BODY ? FlameRenderTypes.BODY_COMPAT : FlameRenderTypes.GLOW_COMPAT)
                    : (pass == FlameMesh.BODY ? FlameRenderTypes.BODY : FlameRenderTypes.GLOW);
            FlameMesh.format(compat, compat && pass == FlameMesh.GLOW);
            VertexConsumer vc = buffers.getBuffer(type);
            for (Fx f : FX) draw(vc, ps, f, pass, pt, cam);
            buffers.endBatch(type);
        }
        FlameMesh.format(false, false);
    }

    private static int hue(FlameLook L) { return L.rim != 0 ? L.rim : FlameMesh.hue(L.core, L.edge); }

    private static void at(PoseStack ps, Camera cam, double x, double y, double z) {
        Vec3 c = cam.getPosition();
        ps.translate(x - c.x, y - c.y, z - c.z);
    }

    /** Faces the camera. */
    private static void billboard(PoseStack ps, Camera cam, Fx f) {
        at(ps, cam, f.x, f.y, f.z);
        ps.mulPose(cam.rotation());
    }

    /** Local x runs from (x, y, z) toward the second point; the plane turns to face the camera around that line. */
    private static float lineBasis(PoseStack ps, Camera cam, Fx f) {
        at(ps, cam, f.x, f.y, f.z);
        Vec3 c = cam.getPosition();
        V0.set((float) (f.a - f.x), (float) (f.b - f.y), (float) (f.c - f.z));
        float len = V0.length();
        if (len < 1e-4F) return 0F;
        V0.div(len);
        V2.set((float) (c.x - f.x), (float) (c.y - f.y), (float) (c.z - f.z));
        V0.cross(V2, V1);                  // width axis
        if (V1.lengthSquared() < 1e-8F) V1.set(0, 1, 0).cross(V0);
        V1.normalize();
        V0.cross(V1, V2).normalize();      // plane normal
        basis(ps, V0, V1, V2);
        return len;
    }

    private static void basis(PoseStack ps, Vector3f x, Vector3f y, Vector3f z) {
        M.set(x, y, z);
        ps.mulPose(Q.setFromNormalized(M));
    }

    private static void draw(VertexConsumer vc, PoseStack ps, Fx f, int pass, float pt, Camera cam) {
        float u = Mth.clamp((f.age + pt) / f.life, 0F, 1F), fade = 1F - u;
        FlameLook L = f.look;
        int hue = hue(L);
        ps.pushPose();
        switch (f.kind) {
            case FlameFx.HIT, FlameFx.BURST -> {
                billboard(ps, cam, f);
                float s = f.size * (f.kind == FlameFx.BURST ? .75F : .5F);
                if (pass == FlameMesh.BODY) FlameMesh.glow(vc, ps.last(), s * (.6F + .6F * u), hue, .3F * fade);
                else FlameMesh.flash(vc, ps.last(), s * (.3F + .35F * u), hue, .85F * fade);
                accent(vc, ps, f, pass, s, u, fade, hue);
            }
            case FlameFx.BOLT -> {
                float len = lineBasis(ps, cam, f);
                if (len > 0) FlameMesh.zigzag(vc, ps.last(), pass, len, Math.min(.25F, .1F + len * .04F), (f.age / 2) * 31 + f.seed, L.edge, .05F, fade);
            }
            case FlameFx.TETHER -> {
                float len = lineBasis(ps, cam, f);
                if (len > 0) FlameMesh.wave(vc, ps.last(), pass, len, .12F, (f.age + pt) * .9F, L.core, L.edge, .07F, fade);
            }
            case FlameFx.THRUST -> {
                float len = lineBasis(ps, cam, f);
                if (len > 0) FlameMesh.streak(vc, ps.last(), pass, len, Math.max(.15F, f.size * .35F), L.edge, L.core, Math.min(1F, fade * 1.4F));
            }
            case FlameFx.SLASH -> {
                at(ps, cam, f.x, f.y, f.z);
                int deg = f.extra & 1023, roll = (f.extra >> 10) - 180;
                V0.set(f.a, 0, f.c);
                if (V0.lengthSquared() < 1e-6F) V0.set(0, 0, 1);
                V0.normalize();                                       // forward
                V2.set(0, 1, 0).rotateAxis((float) Math.toRadians(roll), V0.x, V0.y, V0.z); // the slash plane's normal
                V2.cross(V0, V1).normalize();                         // sideways
                basis(ps, V0, V1, V2);
                float half = (float) Math.toRadians(deg) * .5F, span = half * 2F;
                float lead = -half + span * Math.min(1F, u * 2.6F), tail = Math.max(-half, lead - span * .85F);
                FlameMesh.crescent(vc, ps.last(), pass, f.size, tail, lead, L.core, L.edge, (float) Math.pow(fade, .7));
            }
            case FlameFx.SLAM -> {
                at(ps, cam, f.x, f.y + .06, f.z);
                ps.mulPose(Axis.XP.rotationDegrees(90F)); // lie flat
                float r = f.size * (.25F + .75F * Mth.sqrt(u));
                if (pass == FlameMesh.BODY) FlameMesh.ring(vc, ps.last(), r, .3F, hue, .75F * fade);
                else FlameMesh.ring(vc, ps.last(), r, .08F, FlameMesh.mix(hue, 0xFFFFFF, .4F), .9F * fade);
            }
            case FlameFx.CHARGE -> {
                billboard(ps, cam, f);
                float r = .1F + .16F * f.size + .03F * Mth.sin((f.age + pt) * 1.3F);
                if (pass == FlameMesh.BODY) FlameMesh.glow(vc, ps.last(), r * 1.8F, hue, .3F * fade);
                else FlameMesh.flash(vc, ps.last(), r, hue, .9F * fade);
            }
            case FlameFx.SHIELD -> {
                at(ps, cam, f.x, f.y, f.z);
                V2.set(f.a, 0, f.c);
                if (V2.lengthSquared() < 1e-6F) V2.set(0, 0, 1);
                V2.normalize();                          // facing
                V1.set(0, 1, 0);                         // up
                V1.cross(V2, V0).normalize();            // side = up x facing
                basis(ps, V0, V1, V2);
                float r = .75F * f.size * (.85F + .15F * u);
                if (pass == FlameMesh.BODY) FlameMesh.glow(vc, ps.last(), r, hue, .28F * fade);
                else {
                    FlameMesh.ring(vc, ps.last(), r * .9F, .05F, FlameMesh.mix(hue, 0xFFFFFF, .3F), .9F * fade);
                    accent(vc, ps, f, pass, r * .7F, u, fade, hue);
                }
            }
            case FlameFx.MUZZLE -> {
                billboard(ps, cam, f);
                if (pass == FlameMesh.BODY) FlameMesh.glow(vc, ps.last(), .35F, hue, .35F * fade);
                else {
                    FlameMesh.flash(vc, ps.last(), .18F, hue, .95F * fade);
                    FlameMesh.burstRays(vc, ps.last(), 6, .05F, .4F, .03F, hue, .8F * fade, f.seed);
                }
            }
            default -> {}
        }
        ps.popPose();
    }

    /** Each flame's mark on a hit (the Earth set borrows its counterpart's, in its own colours). */
    private static void accent(VertexConsumer vc, PoseStack ps, Fx f, int pass, float s, float u, float fade, int hue) {
        FlameLook L = f.look;
        var p = ps.last();
        int core = FlameMesh.mix(hue, 0xFFFFFF, .5F);
        switch (style(f.flame)) {
            case STAR -> {
                if (pass == FlameMesh.GLOW) FlameMesh.burstRays(vc, p, 4, .02F, s * (1F + .6F * u), .03F, core, .9F * fade, .785F);
            }
            case CROSS -> {
                float l = s * .6F;
                FlameMesh.stroke(vc, p, -l, -l, l, l, pass == FlameMesh.BODY ? .1F : .035F, pass == FlameMesh.BODY ? hue : core, .85F * fade);
                FlameMesh.stroke(vc, p, -l, l, l, -l, pass == FlameMesh.BODY ? .1F : .035F, pass == FlameMesh.BODY ? hue : core, .85F * fade);
            }
            case RIPPLE -> FlameMesh.ring(vc, p, s * (.3F + 1.1F * u), pass == FlameMesh.BODY ? .07F : .025F, pass == FlameMesh.BODY ? hue : core, .85F * fade);
            case RAYS -> {
                if (pass == FlameMesh.GLOW) FlameMesh.burstRays(vc, p, 10, s * .15F, s * (1F + .8F * u), .025F, hue, .9F * fade, f.seed);
            }
            case BOLTS -> {
                for (int k = 0; k < 4; k++) {
                    ps.pushPose();
                    ps.mulPose(Axis.ZP.rotation(k * Mth.HALF_PI + (f.seed % 100) * .01F));
                    FlameMesh.zigzag(vc, ps.last(), pass, s * 1.3F, s * .14F, (f.age / 2) * 17 + k * 101 + f.seed, L.edge, .035F, fade);
                    ps.popPose();
                }
            }
            case DOUBLE -> {
                FlameMesh.ring(vc, p, s * (.3F + u), pass == FlameMesh.BODY ? .06F : .02F, pass == FlameMesh.BODY ? hue : core, .8F * fade);
                float u2 = u - .3F;
                if (u2 > 0) FlameMesh.ring(vc, p, s * (.3F + u2), pass == FlameMesh.BODY ? .06F : .02F, pass == FlameMesh.BODY ? hue : core, .8F * (1F - u2));
            }
            case SWIRL -> FlameMesh.ring(vc, p, s * (1.1F - .6F * u), pass == FlameMesh.BODY ? .12F : .03F, hue, .6F * fade);
            case COLLAPSE -> {
                if (pass == FlameMesh.BODY) FlameMesh.glow(vc, p, s * .5F, L.core, .8F * fade);
                FlameMesh.ring(vc, p, s * 1.3F * (1F - u) + .02F, pass == FlameMesh.BODY ? .06F : .02F, L.rim, .9F * fade);
            }
        }
    }
}
