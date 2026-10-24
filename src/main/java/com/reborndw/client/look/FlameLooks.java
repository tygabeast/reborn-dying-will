package com.reborndw.client.look;

import com.reborndw.client.look.FlameLook.Body;
import com.reborndw.client.look.FlameLook.Emit;
import com.reborndw.client.look.FlameLook.Frame;
import com.reborndw.client.look.FlameLook.ShotBody;
import com.reborndw.flame.Flame;
import com.reborndw.particle.Mote;

/**
 * The look of every flame, as chosen from the flame preview. Each has two shades (root to tip), a tapered body that
 * starts at a rounded point on the brow and fans upward, and the motes it throws off. The Earth set takes its Sky
 * counterpart's shades, one shade darker, and has script orbiting it.
 */
public final class FlameLooks {
    private static final FlameLook[] LOOKS = new FlameLook[Flame.values().length];
    private static final int WHITE = 0xFFFFFF;
    private static final int FALL_SPIN = Mote.FALL | Mote.SPIN;

    static {
        // ---- Sky set (80% size)
        FlameLook l = put(Flame.SKY, 0xFF8A1E, 0xFFD98A);
        l.sz = .8F; l.n = 3; l.w = 7; l.h = 22;
        l.head.add(Emit.of(Mote.PX, 12).life(.6F, 1F).at(4, 0, 2).vel(5, 16, 30).size(2).tint(0xFFB347, 0xFFF0B0));
        l.trail.add(Emit.of(Mote.PX, 40).life(.3F, .5F).back(10, 25).vel(0, -6, 6).size(3));
        l.shotR = 5;

        l = put(Flame.STORM, 0xFF4D6D, 0x7D0C20);
        l.sz = .8F; l.n = 2; l.w = 7; l.h = 18;
        l.head.add(Emit.of(Mote.PX, 11).life(.5F, .9F).at(0, 0, 0).vel(18, 4, 20).size(2).tint(0xFF4D6D, 0x3A0410));
        l.head.add(Emit.of(Mote.CHIP, 3).life(.6F, .9F).vel(22, 2, 14).size(3).tint(0xFF6B85, 0x4A0614).flags(FALL_SPIN));
        l.trail.add(Emit.of(Mote.PX, 20).life(.35F, .55F).back(6, 35).vel(0, -20, 20).size(2));
        l.trail.add(Emit.of(Mote.CHIP, 5).life(.4F, .65F).back(6, 30).vel(0, -12, 12).size(2.6F).flags(Mote.SPIN));
        l.shotR = 4;

        l = put(Flame.RAIN, 0x2F80ED, 0x8FE3FF);
        l.sz = .8F; l.body = Body.ORB; l.orbR = 7; l.orbAlpha = .8F; l.underAlpha = .44F;
        l.head.add(Emit.of(Mote.DROP, 16).life(.8F, 1.3F).at(5, 0, 4).vel(6, 18, 34).size(4).flags(Mote.SWAY)); // rises, never falls
        l.trail.add(Emit.of(Mote.DROP, 30).life(.4F, .7F).back(6, 18).vel(0, 6, 22).size(3.5F));
        l.shotR = 5;

        l = put(Flame.SUN, 0xFFFBE0, 0xE3E83A);
        l.sz = .8F; l.body = Body.SUN; l.rays = 10; l.rayLen = 16; l.orbR = 6;
        l.head.add(Emit.of(Mote.SPARK, 12).life(.4F, .8F).vel(30, 6, 40).size(2.5F).tint(0xFFF7B0, 0xFFE14A));
        l.trail.add(Emit.of(Mote.SPARK, 24).life(.3F, .5F).back(10, 40).vel(0, -18, 18).size(2.5F).tint(0xFFF7B0, 0xFFE14A));
        l.shot = ShotBody.SUN; l.shotR = 5;

        l = put(Flame.LIGHTNING, 0xD8FFE0, 0x3DDC64);
        l.sz = .8F; l.body = Body.BOLTS; l.bolts = 5; l.boltLen = 16; l.boltSpread = .6F; l.boltJit = .38F; l.boltMs = 80; l.underAlpha = .25F; l.orbR = 3;
        l.head.add(Emit.of(Mote.SPARK, 14).life(.2F, .45F).vel(34, 10, 60).size(2.5F));
        l.trail.add(Emit.of(Mote.SPARK, 26).life(.2F, .4F).back(10, 50).vel(0, -35, 35).size(2.5F));
        l.shot = ShotBody.WEB; l.shotR = 8;

        l = put(Flame.CLOUD, 0xC026D3, 0xE0C6FF);
        l.sz = .8F; l.n = 1; l.w = 8; l.h = 20;
        l.head.add(Emit.of(Mote.MINI, 6).life(.8F, 1.2F).at(2, 0, 0).vel(22, 6, 16).size(3).flags(Mote.GROW));
        l.trail.add(Emit.of(Mote.MINI, 12).life(.5F, .8F).back(6, 18).vel(0, -6, 20).size(3.4F).flags(Mote.GROW));
        l.shotR = 4;

        l = put(Flame.MIST, 0x6C3FA8, 0xC4B5FF);
        l.sz = .8F; l.body = Body.ORB; l.orbR = 6; l.orbAlpha = .5F; l.underAlpha = .27F;
        l.head.add(Emit.of(Mote.WISP, 8).life(1F, 1.6F).vel(8, 5, 14).size(8).flags(Mote.GROW | Mote.SWAY));
        l.trail.add(Emit.of(Mote.WISP, 14).life(.6F, 1F).back(2, 12).vel(0, -6, 6).size(5).flags(Mote.GROW));
        l.shotR = 4; l.shotAlpha = .35F;

        // ---- Earth set: counterpart shades one shade darker, script orbiting every one
        l = put(Flame.EARTH, 0xF04A1D, 0xFFA23A);
        l.body = Body.Y; l.w = 8; l.stem = 8; l.arm = 11; l.ang = .5F; l.script = 2;
        l.head.add(Emit.of(Mote.PX, 12).life(.6F, 1F).at(4, 0, 2).vel(5, 14, 28).size(2));
        l.trail.add(Emit.of(Mote.PX, 34).life(.3F, .5F).back(10, 25).vel(0, -6, 6).size(2.6F));
        l.shot = ShotBody.Y; l.shotR = 5;

        l = put(Flame.GLACIER, 0x9A1EA9, 0xAF6BFF);
        l.n = 3; l.w = 6; l.h = 18; l.script = 1; l.frame = Frame.CRYSTALS; l.frameA = 0xB8F4FF; l.frameB = 0x22D3EE;
        l.head.add(Emit.of(Mote.SHARD, 11).life(.7F, 1.2F).vel(10, 4, 18).size(3.5F).tint(0xB8F4FF, 0x22D3EE).flags(Mote.SPIN));
        l.head.add(Emit.of(Mote.PX, 8).life(.4F, .8F).at(6, 0, 4).vel(0, 4, 14).size(1).tint(WHITE, WHITE));
        l.trail.add(Emit.of(Mote.SHARD, 24).life(.4F, .7F).back(6, 24).vel(0, -12, 12).size(3).tint(0xB8F4FF, 0x22D3EE).flags(Mote.SPIN));
        l.shotR = 4; l.shotAlpha = .85F;

        l = put(Flame.FOREST, 0xFFEF80, 0xCAD018);
        l.n = 2; l.w = 7; l.h = 16; l.script = 1;
        l.head.add(Emit.of(Mote.LEAF, 8).life(.9F, 1.5F).vel(12, 8, 22).size(3).tint(0x3FAE4B, 0xC8F59A).flags(Mote.SPIN | Mote.SWAY));
        l.trail.add(Emit.of(Mote.LEAF, 18).life(.5F, .9F).back(8, 20).vel(0, -6, 14).size(2.8F).tint(0x3FAE4B, 0xC8F59A).flags(Mote.SPIN | Mote.SWAY));
        l.shotR = 5;

        l = put(Flame.MOUNTAIN, 0x7AFF95, 0x22BE49);
        l.n = 3; l.w = 8; l.h = 14; l.script = 1;
        l.head.add(Emit.of(Mote.CHIP, 8).life(.8F, 1.3F).vel(10, 6, 18).size(3).tint(0xA39A90, 0x5E5248).flags(FALL_SPIN));
        l.trail.add(Emit.of(Mote.CHIP, 18).life(.5F, .9F).back(6, 18).vel(0, -6, 10).size(2.6F).tint(0xA39A90, 0x5E5248).flags(FALL_SPIN));
        l.shotR = 5;

        l = put(Flame.SWAMP, 0xFF0B37, 0x640A1A);
        l.n = 3; l.w = 7; l.h = 14; l.script = 1; l.frame = Frame.GAS; l.frameA = 0x0E4A28; l.frameB = 0x2F7A48;
        l.head.add(Emit.of(Mote.BUBBLE, 6).life(.8F, 1.4F).vel(0, 6, 14).size(3.5F).tint(0x1F6B3A, 0x58C27A).flags(Mote.GROW));
        l.head.add(Emit.of(Mote.PUFF, 6).life(1F, 1.6F).vel(0, 4, 10).size(5).tint(0x14502E, 0x3F9A5C).flags(Mote.GROW));
        l.trail.add(Emit.of(Mote.PUFF, 14).life(.5F, .9F).back(3, 12).vel(0, 0, 8).size(4).tint(0x14502E, 0x3F9A5C).flags(Mote.GROW));
        l.trail.add(Emit.of(Mote.BUBBLE, 8).life(.5F, .8F).back(4, 12).vel(0, 2, 10).size(2.5F).tint(0x1F6B3A, 0x58C27A));
        l.shotR = 5;

        l = put(Flame.DESERT, 0x563286, 0x7E5EFF);
        l.n = 3; l.w = 6; l.h = 18; l.script = 1;
        l.head.add(Emit.of(Mote.PX, 22).life(.6F, 1.1F).at(0, 0, 0).vel(3, 12, 26).size(1.5F).tint(0xECD9A0, 0xC9A257).flags(Mote.SWAY));
        l.trail.add(Emit.of(Mote.PX, 36).life(.3F, .6F).back(6, 24).vel(0, -10, 10).size(1.5F).tint(0xECD9A0, 0xC9A257).flags(Mote.SWAY));
        l.shotR = 4; l.shotAlpha = .8F;

        l = put(Flame.RIVER, 0x1264D1, 0x3FCFFF);
        l.n = 2; l.w = 6; l.h = 16; l.script = 1;
        l.head.add(Emit.of(Mote.SLASH, 9).life(.25F, .5F).at(12, -4, 14).size(9).tint(WHITE, 0x3FCFFF));
        l.head.add(Emit.of(Mote.PX, 8).life(.8F, 1.2F).at(6, 0, 2).vel(4, -6, 6).size(1.5F).tint(0x8FE3FF, 0x1264D1).flags(Mote.FALL));
        l.trail.add(Emit.of(Mote.SLASH, 14).life(.2F, .4F).back(0, 10).at(0, -6, 6).size(10).tint(WHITE, 0x3FCFFF));
        l.trail.add(Emit.of(Mote.PX, 10).life(.4F, .7F).back(3, 12).vel(0, -14, 0).size(1.5F).tint(0x8FE3FF, 0x1264D1).flags(Mote.FALL));
        l.shotR = 3;

        // ---- Night
        l = put(Flame.NIGHT, 0x0B0618, 0x3B1D7A);
        l.rim = 0x9B6BFF; l.n = 2; l.w = 5; l.h = 13;
        l.head.add(Emit.of(Mote.PX, 12).life(.5F, .7F).ring(10, 16, 16, 24).size(2).tint(0x9B6BFF, 0x1A0F33));
        l.head.add(Emit.of(Mote.RING, 2.2F).life(.8F, 1F).at(0, 2, 4).size(15).tint(0x9B6BFF, 0x1A0F33).flags(Mote.COLLAPSE));
        l.trail.add(Emit.of(Mote.PX, 24).life(.4F, .6F).ring(6, 12, 14, 22).size(2).tint(0x9B6BFF, 0x1A0F33));
        l.shotR = 5; l.shotC0 = 0x6A3FD0; l.shotC1 = 0x9B6BFF;
    }

    private static FlameLook put(Flame f, int core, int edge) {
        FlameLook l = new FlameLook(core, edge);
        LOOKS[f.ordinal()] = l;
        return l;
    }

    /** World size of one preview unit for a head flame (1/48 block, scaled by the look, form and the player's setting). */
    public static float unit(Flame f, com.reborndw.flame.Form form) {
        return of(f).sz * (form == com.reborndw.flame.Form.ULTIMATE ? 1.3F : 1F) * (float) (double) com.reborndw.config.ClientConfig.FLAME_SCALE.get() / 48F;
    }

    public static FlameLook of(Flame f) { return LOOKS[f.ordinal()]; }

    private FlameLooks() {}
}
