package com.reborndw.client.look;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Builds flame shapes straight into a vertex consumer: no meshes or arrays are allocated, and a whole flame is a few
 * dozen quads. Shapes are drawn in the XY plane in preview units (1/48 block), y up, and every quad is written with
 * both windings so culling never hides it. Not thread safe (the render thread only).
 */
public final class FlameMesh {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    // a small local transform for rotated, scaled sub-shapes (Y arms, half-size frames)
    private static float ox, oy, sc = 1F, cs = 1F, sn;

    private FlameMesh() {}

    private static void frame(float x, float y, float angle, float scale) {
        ox = x; oy = y; sc = scale; cs = Mth.cos(angle); sn = Mth.sin(angle);
    }

    private static void resetFrame() { ox = 0; oy = 0; sc = 1F; cs = 1F; sn = 0F; }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void vtx(VertexConsumer b, PoseStack.Pose p, float x, float y, int rgb, float a) {
        float px = ox + (x * cs - y * sn) * sc, py = oy + (x * sn + y * cs) * sc;
        b.addVertex(p, px, py, 0F).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (Mth.clamp(a, 0F, 1F) * 255F))
                .setUv(.5F, .5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0F, 0F, 1F);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose p,
                             float x0, float y0, int c0, float a0, float x1, float y1, int c1, float a1,
                             float x2, float y2, int c2, float a2, float x3, float y3, int c3, float a3) {
        vtx(b, p, x0, y0, c0, a0); vtx(b, p, x1, y1, c1, a1); vtx(b, p, x2, y2, c2, a2); vtx(b, p, x3, y3, c3, a3);
        vtx(b, p, x3, y3, c3, a3); vtx(b, p, x2, y2, c2, a2); vtx(b, p, x1, y1, c1, a1); vtx(b, p, x0, y0, c0, a0);
    }

    private static void tri(VertexConsumer b, PoseStack.Pose p, float x0, float y0, int c0, float a0, float x1, float y1, int c1, float a1,
                            float x2, float y2, int c2, float a2) {
        quad(b, p, x0, y0, c0, a0, x1, y1, c1, a1, x2, y2, c2, a2, x2, y2, c2, a2);
    }

    /** A flat strip between two edges. */
    private static void strip(VertexConsumer b, PoseStack.Pose p, float lx0, float ly0, float rx0, float ry0, int c0, float a0,
                              float lx1, float ly1, float rx1, float ry1, int c1, float a1) {
        quad(b, p, lx0, ly0, c0, a0, rx0, ry0, c0, a0, rx1, ry1, c1, a1, lx1, ly1, c1, a1);
    }

    /** One tongue of flame: a rounded point at the base, widening to a shoulder, then narrowing to the tip. */
    private static void tongue(VertexConsumer b, PoseStack.Pose p, float spread, float h, float wid, float rb, float t, float ph,
                               int c0, int c1, float a, float ag, float sway) {
        final int S = 7;
        float hh = h * (1F + (ag > 0 ? .14F : .05F) * Mth.sin(t * (ag > 0 ? 9F : 4F) + ph));
        float sw = sway * (ag > 0 ? 1.2F : .5F) * Mth.sin(t * (ag > 0 ? 4F : 2F) + ph * .7F);
        tri(b, p, -rb, 0, c0, a, rb, 0, c0, a, 0, -rb * 1.6F, c0, a); // the rounded base
        float pl = -rb, pr = rb, py = 0F;
        int pc = c0;
        float pa = a;
        for (int i = 1; i <= S; i++) {
            float f = i / (float) S;
            float cx = spread * 1.35F * f + sw * f * f;
            float w = f < .62F ? rb + (wid - rb) * Mth.sqrt(f / .62F) : wid * (float) Math.pow(1F - (f - .62F) / .38F, .85F);
            float jl = 1F, jr = 1F;
            if (ag > 1F && i < S) {
                jl += .22F * Mth.sin(t * 20F + i * 2.3F + ph);
                jr += .22F * Mth.sin(t * 20F + i * 2.9F + ph + 1.7F);
            }
            float lx = cx - w * jl, rx = cx + w * jr, y = hh * f;
            int c = mix(c0, c1, f);
            float al = a * (1F - .45F * f);
            strip(b, p, pl, py, pr, py, pc, pa, lx, y, rx, y, c, al);
            pl = lx; pr = rx; py = y; pc = c; pa = al;
        }
    }

    /** The fan of tongues that is a head flame's body (two passes: the flame, then a brighter core). */
    private static void tongues(VertexConsumer b, PoseStack.Pose p, int n, float w, float h, float t, int c0, int c1, int rim,
                                float ag, float hm, float spr, float sw, float alpha) {
        float mid = (n - 1) / 2F;
        int passes = 2;
        for (int pass = 0; pass < passes; pass++) {
            float r; float a; int a0, a1;
            if (rim != 0) { r = pass == 0 ? 1.14F : 1F; a = pass == 0 ? .95F : 1F; a0 = pass == 0 ? rim : c0; a1 = pass == 0 ? rim : c1; }
            else { r = pass == 0 ? 1F : .52F; a = pass == 0 ? .7F : .6F; a0 = pass == 0 ? c0 : mix(c0, 0xFFFFFF, .22F); a1 = pass == 0 ? c1 : mix(c1, 0xFFFFFF, .15F); }
            for (int i = 0; i < n; i++) {
                float d = Math.abs(i - mid);
                float spread = (i - mid) * w * spr * r;
                float hh = h * hm * r * (1F - d * .2F);
                float wid = w * 1.05F * r * (1F - d * .08F);
                tongue(b, p, spread, hh, wid, Math.max(1.4F, w * .5F * r), t, i * 2.1F, a0, a1, a * alpha, ag, sw * 2F);
            }
        }
    }

    private static void orb(VertexConsumer b, PoseStack.Pose p, float cx, float cy, float r, int c0, int c1, float a, float t) {
        float rr = r * (1F + .07F * Mth.sin(t * 22F));
        int mid = c0, centre = mix(c0, 0xFFFFFF, .65F);
        final int N = 10;
        float pxm = cx + rr * .45F, pym = cy, pxo = cx + rr, pyo = cy;
        for (int i = 1; i <= N; i++) {
            float ang = i * Mth.TWO_PI / N, co = Mth.cos(ang), si = Mth.sin(ang);
            float nxm = cx + co * rr * .45F, nym = cy + si * rr * .45F, nxo = cx + co * rr, nyo = cy + si * rr;
            tri(b, p, cx, cy, centre, a, pxm, pym, mid, a * .9F, nxm, nym, mid, a * .9F);
            quad(b, p, pxm, pym, mid, a * .9F, pxo, pyo, c1, 0F, nxo, nyo, c1, 0F, nxm, nym, mid, a * .9F);
            pxm = nxm; pym = nym; pxo = nxo; pyo = nyo;
        }
    }

    private static void rays(VertexConsumer b, PoseStack.Pose p, FlameLook L, float t, float hm, boolean full) {
        int n = L.rays;
        for (int i = 0; i < n; i++) {
            float a = full ? i * Mth.TWO_PI / n + t * .4F : Mth.PI * (i + .5F) / n;
            float len = L.rayLen * hm * (.65F + .35F * Mth.sin(t * 5F + i * 1.7F)) * ((i & 1) == 1 ? .7F : 1F);
            float w = (i & 1) == 1 ? .6F : 1F, co = Mth.cos(a), si = Mth.sin(a);
            tri(b, p, -si * w, co * w, L.core, .95F, si * w, -co * w, L.core, .95F, co * len, si * len, L.edge, 0F);
        }
    }

    private static float rf(int s) {
        s = s * 374761393 + 668265263;
        s = (s ^ (s >>> 13)) * 1274126177;
        return ((s ^ (s >>> 16)) & 0xFFFFFF) / 16777216F;
    }

    private static void line(VertexConsumer b, PoseStack.Pose p, float x0, float y0, float x1, float y1, float w, int c, float a) {
        float dx = x1 - x0, dy = y1 - y0, l = Mth.sqrt(dx * dx + dy * dy);
        if (l < 1e-4F) return;
        float px = -dy / l * w * .5F, py = dx / l * w * .5F;
        quad(b, p, x0 - px, y0 - py, c, a, x0 + px, y0 + py, c, a, x1 + px, y1 + py, c, a, x1 - px, y1 - py, c, a);
    }

    private static void bolts(VertexConsumer b, PoseStack.Pose p, FlameLook L, boolean calm, int bucket) {
        int n = calm ? Math.max(2, Math.round(L.bolts * .6F)) : L.bolts;
        float len = L.boltLen * (calm ? .9F : 1.02F);
        int core = mix(L.edge, 0xFFFFFF, .85F);
        for (int i = 0; i < n; i++) {
            int seed = bucket * 131 + i * 17;
            float a = Mth.HALF_PI + (rf(seed) - .5F) * 2F * L.boltSpread;
            float l = len * (.8F + .4F * rf(seed + 1));
            float x = 0, y = 0;
            for (int s = 0; s < 5; s++) {
                a += (rf(seed + 2 + s) - .5F) * 2F * L.boltJit;
                float nx = x + Mth.cos(a) * l / 5F, ny = y + Mth.sin(a) * l / 5F;
                line(b, p, x, y, nx, ny, 3F, L.edge, .28F);
                line(b, p, x, y, nx, ny, 1.3F, core, .95F);
                x = nx; y = ny;
            }
        }
    }

    /** Ice crystals either side of the flame's base, meeting at the point it rises from (half size). */
    private static void crystals(VertexConsumer b, PoseStack.Pose p, FlameLook L, float t, float hm) {
        final float[][] C = {{.62F, 19F, 3.4F}, {1.08F, 12F, 2.8F}, {.34F, 11F, 2.4F}};
        int light = mix(L.frameA, 0xFFFFFF, .35F);
        for (int side = -1; side <= 1; side += 2) {
            for (float[] c : C) {
                float th = c[0], len = c[1] * hm, w = c[2];
                float dx = side * Mth.sin(th), dy = Mth.cos(th), nx = Mth.cos(th), ny = -side * Mth.sin(th);
                float bx = side * 1.4F, by = 1F;
                float blx = bx - nx * w * .55F, bly = by - ny * w * .55F, brx = bx + nx * w * .55F, bry = by + ny * w * .55F;
                float slx = bx + dx * len * .62F - nx * w, sly = by + dy * len * .62F - ny * w;
                float srx = bx + dx * len * .62F + nx * w, sry = by + dy * len * .62F + ny * w;
                float tx = bx + dx * len, ty = by + dy * len;
                frame(0, 0, 0, .5F);
                quad(b, p, blx, bly, light, .95F, slx, sly, light, .95F, tx, ty, light, .95F, bx, by, light, .95F);
                quad(b, p, brx, bry, L.frameB, .95F, srx, sry, L.frameB, .95F, tx, ty, L.frameB, .95F, bx, by, L.frameB, .95F);
                resetFrame();
            }
        }
    }

    /** Slow green gas along both sides of the taper (half size). */
    private static void gas(VertexConsumer b, PoseStack.Pose p, FlameLook L, float t, float hm) {
        final float[] ROWS = {.5F, .95F, 24F, 15F};
        frame(0, 0, 0, .5F);
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 2; row++) {
                float th = ROWS[row], full = ROWS[2 + row] * hm;
                for (int k = 1; k <= 4; k++) {
                    float f = k / 4F, len = full * f, r = 2.6F + f * 4.2F;
                    float bob = Mth.sin(t * 1.6F + k * 1.3F + side * 2F + th * 5F) * 1.4F;
                    float x = side * (2F + Mth.sin(th) * len) + bob * .6F, y = Mth.cos(th) * len + bob;
                    final int N = 6;
                    float px = x + r, py = y;
                    for (int i = 1; i <= N; i++) {
                        float ang = i * Mth.TWO_PI / N, nx = x + Mth.cos(ang) * r, ny = y + Mth.sin(ang) * r;
                        tri(b, p, x, y, L.frameB, .78F, px, py, L.frameA, 0F, nx, ny, L.frameA, 0F);
                        px = nx; py = ny;
                    }
                }
            }
        }
        resetFrame();
    }

    /**
     * A head flame in the plane. {@code calm} is Hyper Dying Will: smooth, slower and a little smaller; otherwise it is the
     * jagged, flickering Dying Will flame. {@code t} is already slowed for calm; {@code sec} is real seconds.
     */
    public static void head(VertexConsumer b, PoseStack.Pose p, FlameLook L, boolean calm, float t, float sec) {
        float ag = calm ? 0F : 1.3F, hm = calm ? .9F : 1.02F, spr = calm ? .85F : 1.05F, sw = calm ? .45F : 1.1F;
        int c0 = L.core, c1 = L.edge;
        if (L.frame == FlameLook.Frame.CRYSTALS) crystals(b, p, L, t, hm);
        else if (L.frame == FlameLook.Frame.GAS) gas(b, p, L, t, hm);
        switch (L.body) {
            case TONGUES -> tongues(b, p, L.n, L.w, L.h, t, c0, c1, L.rim, ag, hm, spr, sw, 1F);
            case Y -> {
                float stem = L.stem * hm, arm = L.arm * hm, wid = L.w * 1.05F, sway = ag > 0 ? 1.2F : .5F;
                tongue(b, p, 0, stem, wid * .6F, Math.max(1.4F, L.w * .3F), t, 0F, c0, c1, .7F, ag, sway);
                for (int s = -1; s <= 1; s += 2) {
                    frame(0, stem * .8F, -s * (L.ang + Mth.sin(t * 5F) * .06F * s), 1F);
                    tongue(b, p, 0, arm, wid * .85F, Math.max(1.4F, L.w * .4F), t + s, s * 1.3F, c0, c1, .7F, ag, sway);
                    resetFrame();
                }
            }
            case ORB -> {
                tongues(b, p, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                orb(b, p, 0, 14F, L.orbR, c0, c1, L.orbAlpha, t);
            }
            case SUN -> {
                tongues(b, p, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                rays(b, p, L, t, hm, false);
                orb(b, p, 0, 0F, L.orbR, c0, c1, 1F, t);
            }
            case BOLTS -> {
                tongues(b, p, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                bolts(b, p, L, calm, (int) (sec * 1000F / (L.boltMs * (calm ? 1.9F : 1F))));
                orb(b, p, 0, 0F, L.orbR, mix(c1, 0xFFFFFF, .7F), c1, 1F, t);
            }
        }
    }

    /** A flame shot, drawn facing the camera at its position (the trail is motes). */
    public static void shot(VertexConsumer b, PoseStack.Pose p, FlameLook L, float sec) {
        int c0 = L.shotC0 != 0 ? L.shotC0 : L.core, c1 = L.shotC1 != 0 ? L.shotC1 : L.edge;
        float t = sec;
        switch (L.shot) {
            case ORB -> orb(b, p, 0, 0, L.shotR, c0, c1, L.shotAlpha, t);
            case SUN -> {
                rays(b, p, shotSun(L), t, 1F, true);
                orb(b, p, 0, 0, L.shotR, c0, c1, 1F, t);
            }
            case WEB -> {
                int bucket = (int) (sec * 1000F / 80F), core = mix(c1, 0xFFFFFF, .85F);
                for (int i = 0; i < 8; i++) {
                    int seed = bucket * 97 + i * 13;
                    float a = i * Mth.TWO_PI / 8F + (rf(seed) - .5F) * .5F, x = 0, y = 0;
                    for (int s = 0; s < 4; s++) {
                        a += (rf(seed + 1 + s) - .5F) * .9F;
                        float nx = x + Mth.cos(a) * L.shotR / 4F * (.8F + .4F * rf(seed + 9 + s)), ny = y + Mth.sin(a) * L.shotR / 4F * (.8F + .4F * rf(seed + 9 + s));
                        line(b, p, x, y, nx, ny, 2.4F, c1, .28F);
                        line(b, p, x, y, nx, ny, 1.1F, core, .95F);
                        x = nx; y = ny;
                    }
                }
                orb(b, p, 0, 0, L.shotR * .55F, mix(c1, 0xFFFFFF, .7F), c1, 1F, t);
            }
            case Y -> {
                orb(b, p, 0, 0, L.shotR, c0, c1, 1F, t);
                float w = 5F, stem = 7F, arm = 10F;
                tongue(b, p, 0, stem, w * .6F, 1.4F, t, 0F, c0, c1, .8F, 1F, 1F);
                for (int s = -1; s <= 1; s += 2) {
                    frame(0, stem * .8F, -s * .5F, 1F);
                    tongue(b, p, 0, arm, w * .85F, 1.4F, t + s, s * 1.3F, c0, c1, .8F, 1F, 1F);
                    resetFrame();
                }
            }
        }
    }

    private static FlameLook shotSun(FlameLook L) { return L; }

    /** A flat ring of script, lying in the XZ plane (rotate the pose to tilt or spin it). */
    public static void ring(VertexConsumer b, PoseStack.Pose p, float radius, float halfWidth, int rgb, float a) {
        final int N = 48, REPS = 3, PER = N / REPS;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255, al = (int) (a * 255F);
        float ri = radius - halfWidth, ro = radius + halfWidth;
        float pc = 1F, ps = 0F;
        for (int i = 1; i <= N; i++) {
            float ang = i * Mth.TWO_PI / N, c = Mth.cos(ang), s = Mth.sin(ang);
            float u0 = ((i - 1) % PER) / (float) PER, u1 = u0 + 1F / PER;
            ringVtx(b, p, ri * pc, ri * ps, u0, 0F, r, g, bl, al);
            ringVtx(b, p, ro * pc, ro * ps, u0, 1F, r, g, bl, al);
            ringVtx(b, p, ro * c, ro * s, u1, 1F, r, g, bl, al);
            ringVtx(b, p, ri * c, ri * s, u1, 0F, r, g, bl, al);
            ringVtx(b, p, ri * c, ri * s, u1, 0F, r, g, bl, al);
            ringVtx(b, p, ro * c, ro * s, u1, 1F, r, g, bl, al);
            ringVtx(b, p, ro * pc, ro * ps, u0, 1F, r, g, bl, al);
            ringVtx(b, p, ri * pc, ri * ps, u0, 0F, r, g, bl, al);
            pc = c; ps = s;
        }
    }

    private static void ringVtx(VertexConsumer b, PoseStack.Pose p, float x, float z, float u, float v, int r, int g, int bl, int a) {
        b.addVertex(p, x, 0F, z).setColor(r, g, bl, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0F, 1F, 0F);
    }
}
