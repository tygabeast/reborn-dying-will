package com.reborndw.client.look;

import java.util.ArrayList;
import java.util.List;

/**
 * How one flame looks on a head and in flight. Sizes are in "preview units": one unit is 1/48 of a block, so a head
 * (half a block) is 24 units wide. Colours are 0xRRGGBB. The two shades run from the root of the flame to its tip.
 */
public final class FlameLook {
    public enum Body { TONGUES, Y, ORB, SUN, BOLTS }

    public enum Frame { NONE, CRYSTALS, GAS }

    public enum ShotBody { ORB, Y, WEB, SUN }

    /** One kind of mote a flame throws off. Numbers are preview units and seconds; vertical speed is up-positive. */
    public static final class Emit {
        public final int kind;
        public final float rate;
        public float lifeLo = .6F, lifeHi = 1F;
        public float ox, oyLo, oyHi;
        public float vx, vyLo, vyHi;
        public float size = 2F;
        public int c0, c1; // 0 means the flame's own two shades
        public int flags;
        public float ringLo, ringHi, pullLo, pullHi;
        public float backLo, backHi;
        public float alpha = 1F;

        Emit(int kind, float rate) { this.kind = kind; this.rate = rate; }

        public static Emit of(int kind, float rate) { return new Emit(kind, rate); }

        public Emit life(float lo, float hi) { lifeLo = lo; lifeHi = hi; return this; }

        public Emit at(float ox, float oyLo, float oyHi) { this.ox = ox; this.oyLo = oyLo; this.oyHi = oyHi; return this; }

        public Emit vel(float vx, float vyLo, float vyHi) { this.vx = vx; this.vyLo = vyLo; this.vyHi = vyHi; return this; }

        public Emit size(float s) { size = s; return this; }

        public Emit tint(int a, int b) { c0 = a; c1 = b; return this; }

        public Emit flags(int f) { flags = f; return this; }

        /** Spawns on a ring of this radius and moves inward at {@code pull}. */
        public Emit ring(float lo, float hi, float pullLo, float pullHi) { ringLo = lo; ringHi = hi; this.pullLo = pullLo; this.pullHi = pullHi; return this; }

        /** For shots: thrown backwards along the shot's path at this speed. */
        public Emit back(float lo, float hi) { backLo = lo; backHi = hi; return this; }
    }

    public final int core, edge;
    public Body body = Body.TONGUES;
    public int n = 3;
    public float w = 7F, h = 22F;
    /** Overall scale (the Sky-set flames are 80%). */
    public float sz = 1F;
    /** Outline colour for flames that are dark inside (Night); 0 for none. */
    public int rim;
    public float orbR = 6F, orbAlpha = .8F, underAlpha = .55F;
    public int rays = 10;
    public float rayLen = 16F;
    public int bolts = 5;
    public float boltLen = 22F, boltSpread = .6F, boltJit = .38F;
    public int boltMs = 80;
    public float stem = 8F, arm = 11F, ang = .5F;
    public Frame frame = Frame.NONE;
    public int frameA, frameB;
    /** Script orbiting the head (the Earth set): 0 none, 1 one flat ring, 2 two crossing rings. */
    public int script;
    public ShotBody shot = ShotBody.ORB;
    public float shotR = 5F, shotAlpha = 1F;
    public int shotC0, shotC1;
    public final List<Emit> head = new ArrayList<>();
    public final List<Emit> trail = new ArrayList<>();

    FlameLook(int core, int edge) { this.core = core; this.edge = edge; }
}
