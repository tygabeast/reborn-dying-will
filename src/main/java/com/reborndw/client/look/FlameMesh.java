package com.reborndw.client.look;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Builds flame shapes straight into a vertex consumer (no allocation). Shapes are in the XY plane in preview units
 * (1/48 block), y up, facing the camera. Every shape is drawn in two passes: {@link #BODY} (normal blending, gives the
 * colour and soft edges) and {@link #GLOW} (added light: the bright core, halo and hot base). Each tongue is shaded across
 * its width, bright in the middle and fading to nothing at the edges, which is what makes it read as round rather than
 * flat. Render thread only.
 */
public final class FlameMesh {
    public static final int BODY = 0, GLOW = 1;
    private static final int WHITE = 0xFFFFFF;
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static float ox, oy, oz, sc = 1F, cs = 1F, sn;
    private static boolean entityFormat, doubleSided;

    private FlameMesh() {}

    /**
     * Sets how the next shapes are written: plain position+colour (the mod's own render types), or the entity vertex
     * format for vanilla entity render types (used under shader packs); {@code doubleSided} for render types that cull.
     */
    public static void format(boolean entity, boolean twoSided) { entityFormat = entity; doubleSided = twoSided; }

    private static void frame(float x, float y, float angle, float scale) {
        ox = x; oy = y; sc = scale; cs = Mth.cos(angle); sn = Mth.sin(angle);
    }

    private static void resetFrame() { ox = 0; oy = 0; sc = 1F; cs = 1F; sn = 0F; }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0F, 1F);
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void vtx(VertexConsumer b, PoseStack.Pose p, float x, float y, int rgb, float a) {
        float px = ox + (x * cs - y * sn) * sc, py = oy + (x * sn + y * cs) * sc;
        VertexConsumer v = b.addVertex(p, px, py, oz).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (Mth.clamp(a, 0F, 1F) * 255F));
        if (entityFormat) v.setUv(.5F, .5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0F, 0F, 1F);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose p,
                             float x0, float y0, int c0, float a0, float x1, float y1, int c1, float a1,
                             float x2, float y2, int c2, float a2, float x3, float y3, int c3, float a3) {
        vtx(b, p, x0, y0, c0, a0); vtx(b, p, x1, y1, c1, a1); vtx(b, p, x2, y2, c2, a2); vtx(b, p, x3, y3, c3, a3);
        if (doubleSided) { vtx(b, p, x3, y3, c3, a3); vtx(b, p, x2, y2, c2, a2); vtx(b, p, x1, y1, c1, a1); vtx(b, p, x0, y0, c0, a0); }
    }

    private static void tri(VertexConsumer b, PoseStack.Pose p, float x0, float y0, int c0, float a0, float x1, float y1, int c1, float a1,
                            float x2, float y2, int c2, float a2) {
        quad(b, p, x0, y0, c0, a0, x1, y1, c1, a1, x2, y2, c2, a2, x2, y2, c2, a2);
    }

    // ---- primitives

    private static final int ROWS = 10;
    private static final float[] RX = new float[5], RC = new float[5];

    /**
     * One soft tongue of flame: a round bulb at the base, widening to a shoulder, then narrowing to the tip. {@code aC},
     * {@code aM} are the alpha along the middle and part-way out (the very edge is always clear); {@code whiten} lifts the
     * middle toward white near the root.
     */
    private static void tongue(VertexConsumer b, PoseStack.Pose p, float spread, float h, float wid, float rb, float t, float ph,
                               int c0, int c1, float aC, float aM, float whiten, float ag, float sway) {
        float hh = h * (1F + (ag > 0 ? .12F : .05F) * Mth.sin(t * (ag > 0 ? 9F : 4F) + ph));
        float sw = sway * (ag > 0 ? 1.2F : .5F) * Mth.sin(t * (ag > 0 ? 4F : 2F) + ph * .7F);
        float bulb = rb * 1.15F;
        float pxl = 0, pxlm = 0, pxc = 0, pxrm = 0, pxr = 0, py = 0, pa = 0, pam = 0;
        int pc = c0, pcc = c0;
        for (int i = 0; i <= ROWS; i++) {
            float f = i / (float) ROWS;
            float y = -rb + (hh + rb) * f;
            float w = f < .15F ? bulb * Mth.sqrt(f / .15F)
                    : f < .6F ? bulb + (wid - bulb) * Mth.sqrt((f - .15F) / .45F)
                    : wid * (float) Math.pow(Math.max(0F, 1F - (f - .6F) / .4F), .9F);
            float cx = spread * 1.35F * f + sw * f * f;
            float jl = 1F, jr = 1F;
            if (ag > 1F && i > 1 && i < ROWS) {
                jl += .2F * Mth.sin(t * 19F + i * 2.3F + ph);
                jr += .2F * Mth.sin(t * 19F + i * 2.9F + ph + 1.7F);
            }
            float av = 1F - .85F * smooth(.5F, 1F, f);
            int c = mix(c0, c1, f), cc = mix(c, WHITE, whiten * (1F - f));
            float xl = cx - w * jl, xlm = cx - w * .5F * jl, xrm = cx + w * .5F * jr, xr = cx + w * jr;
            float a = aC * av, am = aM * av;
            if (i > 0) {
                quad(b, p, pxl, py, pc, 0, pxlm, py, pc, pam, xlm, y, c, am, xl, y, c, 0);
                quad(b, p, pxlm, py, pc, pam, pxc, py, pcc, pa, cx, y, cc, a, xlm, y, c, am);
                quad(b, p, pxc, py, pcc, pa, pxrm, py, pc, pam, xrm, y, c, am, cx, y, cc, a);
                quad(b, p, pxrm, py, pc, pam, pxr, py, pc, 0, xr, y, c, 0, xrm, y, c, am);
            }
            pxl = xl; pxlm = xlm; pxc = cx; pxrm = xrm; pxr = xr; py = y; pa = a; pam = am; pc = c; pcc = cc;
        }
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    /** A round soft disc: {@code cc}/{@code ac} at the centre, {@code ce}/0 at the rim, {@code cm}/{@code am} at 55%. */
    private static void disc(VertexConsumer b, PoseStack.Pose p, float cx, float cy, float r, int cc, float ac, int cm, float am, int ce) {
        final int N = 14;
        float pxm = cx + r * .55F, pym = cy, pxo = cx + r, pyo = cy;
        for (int i = 1; i <= N; i++) {
            float ang = i * Mth.TWO_PI / N, co = Mth.cos(ang), si = Mth.sin(ang);
            float nxm = cx + co * r * .55F, nym = cy + si * r * .55F, nxo = cx + co * r, nyo = cy + si * r;
            tri(b, p, cx, cy, cc, ac, pxm, pym, cm, am, nxm, nym, cm, am);
            quad(b, p, pxm, pym, cm, am, pxo, pyo, ce, 0F, nxo, nyo, ce, 0F, nxm, nym, cm, am);
            pxm = nxm; pym = nym; pxo = nxo; pyo = nyo;
        }
    }

    private static void line(VertexConsumer b, PoseStack.Pose p, float x0, float y0, float x1, float y1, float w, int c, float a) {
        float dx = x1 - x0, dy = y1 - y0, l = Mth.sqrt(dx * dx + dy * dy);
        if (l < 1e-4F) return;
        float px = -dy / l * w * .5F, py = dx / l * w * .5F;
        // soft sides: clear at the edges, solid along the middle
        quad(b, p, x0 - px, y0 - py, c, 0, x0, y0, c, a, x1, y1, c, a, x1 - px, y1 - py, c, 0);
        quad(b, p, x0, y0, c, a, x0 + px, y0 + py, c, 0, x1 + px, y1 + py, c, 0, x1, y1, c, a);
    }

    private static float rf(int s) {
        s = s * 374761393 + 668265263;
        s = (s ^ (s >>> 13)) * 1274126177;
        return ((s ^ (s >>> 16)) & 0xFFFFFF) / 16777216F;
    }

    // ---- composite bodies

    private static float sat(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255, mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return mx == 0 ? 0F : (mx - mn) / (float) mx;
    }

    /** The more colourful of a flame's two shades: glow is tinted with it, so pale cores still glow in colour. */
    public static int hue(int c0, int c1) { return sat(c0) >= sat(c1) ? c0 : c1; }

    /** A fan of tongues: the flame's colour (BODY) or its bright core, halo and hot base (GLOW). */
    private static void fan(VertexConsumer b, PoseStack.Pose p, int pass, int n, float w, float h, float t, int c0, int c1, int rim,
                            float ag, float hm, float spr, float sw, float alpha) {
        float mid = (n - 1) / 2F;
        int hue = hue(c0, c1);
        if (pass == GLOW && rim == 0)
            disc(b, p, 0, h * hm * .4F, Math.max(h * hm * .55F, w * n * .42F), hue, .18F * alpha, hue, .08F * alpha, hue);
        for (int i = 0; i < n; i++) {
            float d = Math.abs(i - mid);
            float spread = (i - mid) * w * spr;
            float hh = h * hm * (1F - d * .2F);
            float wid = w * 1.05F * (1F - d * .08F);
            float rb = Math.max(1.4F, w * .5F);
            if (rim != 0) { // dark flame: a violet rim around a near-black body, both blended (light cannot darken)
                if (pass == BODY) {
                    tongue(b, p, spread * 1.05F, hh * 1.14F, wid * 1.32F, rb * 1.2F, t, i * 2.1F, rim, rim, .55F * alpha, .7F * alpha, .2F, ag, sw * 2F);
                    oz = .3F; // a hair nearer the camera, so depth-sorted render types still draw it over the rim
                    tongue(b, p, spread, hh, wid, rb, t, i * 2.1F, c0, c1, .97F * alpha, .93F * alpha, 0F, ag, sw * 2F);
                    oz = 0F;
                }
            } else if (pass == BODY) {
                tongue(b, p, spread, hh, wid, rb, t, i * 2.1F, c0, c1, .9F * alpha, .62F * alpha, .12F, ag, sw * 2F);
            } else {
                tongue(b, p, spread * .9F, hh * .78F, wid * .55F, rb * .7F, t, i * 2.1F, mix(hue, WHITE, .2F), hue, .55F * alpha, .25F * alpha, .3F, ag, sw * 2F);
            }
        }
        if (pass == GLOW && rim == 0) {
            int hot = mix(hue, WHITE, .45F);
            disc(b, p, 0, 0, w * .75F, hot, .5F * alpha, hot, .22F * alpha, hue);
        }
    }

    private static void orb(VertexConsumer b, PoseStack.Pose p, int pass, float cx, float cy, float r, int c0, int c1, float a, float t) {
        float rr = r * (1F + .06F * Mth.sin(t * 22F));
        if (pass == BODY) disc(b, p, cx, cy, rr, mix(c0, WHITE, .3F), a, c0, a * .85F, c1);
        else {
            int h = hue(c0, c1);
            disc(b, p, cx, cy, rr * 1.7F, h, .26F * a, h, .12F * a, h);
            disc(b, p, cx, cy, rr * .6F, mix(h, WHITE, .6F), .65F * a, mix(h, WHITE, .35F), .35F * a, h);
        }
    }

    private static void rays(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, float t, float hm, boolean full, float r) {
        int n = L.rays;
        for (int i = 0; i < n; i++) {
            float a = full ? i * Mth.TWO_PI / n + t * .4F : Mth.PI * (i + .5F) / n;
            float len = L.rayLen * hm * (.65F + .35F * Mth.sin(t * 5F + i * 1.7F)) * ((i & 1) == 1 ? .7F : 1F);
            float w = ((i & 1) == 1 ? .9F : 1.5F), co = Mth.cos(a), si = Mth.sin(a);
            float bx = co * r * .6F, by = si * r * .6F;
            int c = pass == GLOW ? hue(L.core, L.edge) : L.edge;
            float al = pass == GLOW ? .8F : .6F;
            tri(b, p, bx - si * w, by + co * w, c, al, bx + si * w, by - co * w, c, al, co * len, si * len, L.edge, 0F);
        }
    }

    private static void bolts(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, boolean calm, int bucket) {
        int n = calm ? Math.max(2, Math.round(L.bolts * .6F)) : L.bolts;
        float len = L.boltLen * (calm ? .9F : 1.02F);
        int core = mix(L.edge, WHITE, .8F);
        for (int i = 0; i < n; i++) {
            int seed = bucket * 131 + i * 17;
            float a = Mth.HALF_PI + (rf(seed) - .5F) * 2F * L.boltSpread;
            float l = len * (.8F + .4F * rf(seed + 1));
            float x = 0, y = 0;
            for (int s = 0; s < 5; s++) {
                a += (rf(seed + 2 + s) - .5F) * 2F * L.boltJit;
                float nx = x + Mth.cos(a) * l / 5F, ny = y + Mth.sin(a) * l / 5F;
                if (pass == BODY) line(b, p, x, y, nx, ny, 2.6F, L.edge, .7F);
                else { line(b, p, x, y, nx, ny, 5F, L.edge, .45F); line(b, p, x, y, nx, ny, 1.4F, core, 1F); }
                x = nx; y = ny;
            }
        }
    }

    /** Ice crystals either side of the base, meeting at the point the flame rises from (half size). */
    private static void crystals(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, float hm) {
        final float[][] C = {{.62F, 19F, 3.4F}, {1.08F, 12F, 2.8F}, {.34F, 11F, 2.4F}};
        int light = mix(L.frameA, WHITE, .35F);
        frame(0, 0, 0, .5F);
        for (int side = -1; side <= 1; side += 2) {
            for (float[] c : C) {
                float th = c[0], len = c[1] * hm, w = c[2];
                float dx = side * Mth.sin(th), dy = Mth.cos(th), nx = Mth.cos(th), ny = -side * Mth.sin(th);
                float bx = side * 1.4F, by = 1F;
                float blx = bx - nx * w * .55F, bly = by - ny * w * .55F, brx = bx + nx * w * .55F, bry = by + ny * w * .55F;
                float slx = bx + dx * len * .62F - nx * w, sly = by + dy * len * .62F - ny * w;
                float srx = bx + dx * len * .62F + nx * w, sry = by + dy * len * .62F + ny * w;
                float tx = bx + dx * len, ty = by + dy * len;
                if (pass == BODY) {
                    quad(b, p, blx, bly, light, .92F, slx, sly, light, .92F, tx, ty, WHITE, .95F, bx, by, light, .92F);
                    quad(b, p, brx, bry, L.frameB, .92F, srx, sry, L.frameB, .92F, tx, ty, light, .95F, bx, by, L.frameB, .92F);
                } else {
                    line(b, p, bx, by, tx, ty, 1.2F, WHITE, .5F);
                }
            }
        }
        resetFrame();
    }

    /** Slow green gas along both sides of the taper (half size). */
    private static void gas(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, float t, float hm) {
        if (pass != BODY) return;
        final float[] ROWS_ = {.5F, .95F, 24F, 15F};
        frame(0, 0, 0, .5F);
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 2; row++) {
                float th = ROWS_[row], full = ROWS_[2 + row] * hm;
                for (int k = 1; k <= 4; k++) {
                    float f = k / 4F, len = full * f, r = 2.6F + f * 4.2F;
                    float bob = Mth.sin(t * 1.6F + k * 1.3F + side * 2F + th * 5F) * 1.4F;
                    float x = side * (2F + Mth.sin(th) * len) + bob * .6F, y = Mth.cos(th) * len + bob;
                    disc(b, p, x, y, r * .8F, L.frameB, .45F, L.frameA, .25F, L.frameA);
                }
            }
        }
        resetFrame();
    }

    /**
     * A head flame. {@code calm} is Hyper Dying Will: smooth, slower and a little smaller; otherwise the jagged, flickering
     * Dying Will flame. {@code t} is already slowed for calm; {@code sec} is real seconds.
     */
    public static void head(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, boolean calm, float t, float sec) {
        float ag = calm ? 0F : 1.3F, hm = calm ? .9F : 1.02F, spr = calm ? .85F : 1.05F, sw = calm ? .45F : 1.1F;
        int c0 = L.core, c1 = L.edge;
        if (L.frame == FlameLook.Frame.CRYSTALS) crystals(b, p, pass, L, hm);
        else if (L.frame == FlameLook.Frame.GAS) gas(b, p, pass, L, t, hm);
        switch (L.body) {
            case TONGUES -> fan(b, p, pass, L.n, L.w, L.h, t, c0, c1, L.rim, ag, hm, spr, sw, 1F);
            case Y -> {
                float stem = L.stem * hm, arm = L.arm * hm, wid = L.w * 1.05F, sway = ag > 0 ? 1.2F : .5F;
                if (pass == GLOW) disc(b, p, 0, (stem + arm) * .45F, (stem + arm) * .55F, c0, .18F, c0, .08F, c0);
                float aC = pass == BODY ? .9F : .7F, aM = pass == BODY ? .62F : .3F, wm = pass == BODY ? 1F : .55F;
                int r0 = pass == BODY ? c0 : mix(c0, WHITE, .35F);
                float wh = pass == BODY ? .12F : .4F;
                tongue(b, p, 0, stem * 1.1F, wid * .65F * wm, Math.max(1.4F, L.w * .32F), t, 0F, r0, c1, aC, aM, wh, ag, sway);
                for (int s = -1; s <= 1; s += 2) {
                    frame(0, stem * .85F, -s * (L.ang + Mth.sin(t * 5F) * .06F * s), 1F);
                    tongue(b, p, 0, arm, wid * .8F * wm, Math.max(1.4F, L.w * .38F), t + s, s * 1.3F, r0, c1, aC, aM, wh, ag, sway);
                    resetFrame();
                }
                if (pass == GLOW) disc(b, p, 0, 0, L.w * .7F, mix(c0, WHITE, .55F), .55F, mix(c0, WHITE, .55F), .25F, c0);
            }
            case ORB -> {
                fan(b, p, pass, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                orb(b, p, pass, 0, 14F, L.orbR, c0, c1, L.orbAlpha, t);
            }
            case SUN -> {
                fan(b, p, pass, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                rays(b, p, pass, L, t, hm, false, L.orbR);
                orb(b, p, pass, 0, 0F, L.orbR, c0, c1, 1F, t);
            }
            case BOLTS -> {
                fan(b, p, pass, 3, 6F, 26F, t, c0, c1, 0, ag, hm, spr, sw, L.underAlpha / .7F);
                bolts(b, p, pass, L, calm, (int) (sec * 1000F / (L.boltMs * (calm ? 1.9F : 1F))));
                orb(b, p, pass, 0, 0F, L.orbR, mix(c1, WHITE, .7F), c1, 1F, t);
            }
        }
    }

    /**
     * A flame shot, facing the camera. {@code tailAngle} points the comet tail (screen space, radians from up) and
     * {@code tailLen} (0..1) shortens it when the shot flies toward or away from the camera.
     */
    public static void shot(VertexConsumer b, PoseStack.Pose p, int pass, FlameLook L, float sec, float tailAngle, float tailLen) {
        int c0 = L.shotC0 != 0 ? L.shotC0 : L.core, c1 = L.shotC1 != 0 ? L.shotC1 : L.edge;
        float t = sec, r = L.shotR, a = L.shotAlpha;
        if (tailLen > .05F && L.shot != FlameLook.ShotBody.WEB) {
            frame(0, 0, tailAngle, 1F);
            if (pass == BODY) tongue(b, p, 0, r * 3.4F * tailLen, r * .85F, r * .7F, t, 0F, c0, c1, .8F * a, .5F * a, .1F, 1.3F, .4F);
            else tongue(b, p, 0, r * 2.6F * tailLen, r * .5F, r * .5F, t, 0F, mix(c0, WHITE, .4F), c1, .6F * a, .25F * a, .4F, 1.3F, .4F);
            resetFrame();
        }
        switch (L.shot) {
            case ORB -> orb(b, p, pass, 0, 0, r, c0, c1, a, t);
            case SUN -> {
                rays(b, p, pass, L, t, .8F, true, r);
                orb(b, p, pass, 0, 0, r, c0, c1, 1F, t);
            }
            case WEB -> {
                int bucket = (int) (sec * 1000F / 80F), core = mix(c1, WHITE, .8F);
                for (int i = 0; i < 8; i++) {
                    int seed = bucket * 97 + i * 13;
                    float an = i * Mth.TWO_PI / 8F + (rf(seed) - .5F) * .5F, x = 0, y = 0;
                    for (int s = 0; s < 4; s++) {
                        an += (rf(seed + 1 + s) - .5F) * .9F;
                        float k = r / 4F * (.8F + .4F * rf(seed + 9 + s));
                        float nx = x + Mth.cos(an) * k, ny = y + Mth.sin(an) * k;
                        if (pass == BODY) line(b, p, x, y, nx, ny, 2F, c1, .6F);
                        else { line(b, p, x, y, nx, ny, 4F, c1, .4F); line(b, p, x, y, nx, ny, 1.2F, core, 1F); }
                        x = nx; y = ny;
                    }
                }
                orb(b, p, pass, 0, 0, r * .5F, mix(c1, WHITE, .6F), c1, 1F, t);
            }
            case Y -> {
                orb(b, p, pass, 0, 0, r * .9F, c0, c1, 1F, t);
                float w = 5F, stem = 7F, arm = 10F;
                frame(0, 0, tailAngle + Mth.PI, 1F); // the Y points the way it flies
                float aC = pass == BODY ? .85F : .6F, aM = pass == BODY ? .55F : .25F;
                tongue(b, p, 0, stem, w * .6F, 1.4F, t, 0F, c0, c1, aC, aM, .2F, 1F, 1F);
                float bx = ox, by = oy, ang0 = tailAngle + Mth.PI;
                for (int s = -1; s <= 1; s += 2) {
                    float ex = bx - Mth.sin(ang0) * stem * .8F, ey = by + Mth.cos(ang0) * stem * .8F;
                    frame(ex, ey, ang0 - s * .5F, 1F);
                    tongue(b, p, 0, arm, w * .8F, 1.4F, t + s, s * 1.3F, c0, c1, aC, aM, .2F, 1F, 1F);
                }
                resetFrame();
            }
        }
    }

    /**
     * A ribbon of script standing upright around the head, centred on the pose origin (blocks). Draw it with
     * {@link FlameRenderTypes#SCRIPT}.
     */
    public static void ribbon(VertexConsumer b, PoseStack.Pose p, float radius, float halfHeight, int rgb, float a) {
        final int N = 48, REPS = 4, PER = N / REPS;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255, al = (int) (a * 255F);
        float pc = 1F, ps = 0F;
        for (int i = 1; i <= N; i++) {
            float ang = i * Mth.TWO_PI / N, c = Mth.cos(ang), s = Mth.sin(ang);
            float u0 = ((i - 1) % PER) / (float) PER, u1 = u0 + 1F / PER;
            // u runs clockwise seen from outside so the writing reads left to right
            ribVtx(b, p, radius * pc, -halfHeight, radius * ps, 1F - u0, 1F, r, g, bl, al, pc, ps);
            ribVtx(b, p, radius * c, -halfHeight, radius * s, 1F - u1, 1F, r, g, bl, al, c, s);
            ribVtx(b, p, radius * c, halfHeight, radius * s, 1F - u1, 0F, r, g, bl, al, c, s);
            ribVtx(b, p, radius * pc, halfHeight, radius * ps, 1F - u0, 0F, r, g, bl, al, pc, ps);
            pc = c; ps = s;
        }
    }

    private static void ribVtx(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float u, float v, int r, int g, int bl, int a, float nx, float nz) {
        VertexConsumer vc = b.addVertex(p, x, y, z).setColor(r, g, bl, a).setUv(u, v);
        if (entityFormat) vc.setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, nx, 0F, nz);
        else vc.setLight(FULL);
    }
}
