package com.reborndw.particle;

/** The kinds of flame mote, in the order of the sprites in particles/mote.json. */
public final class Mote {
    public static final int PX = 0, DROP = 1, BUBBLE = 2, SPARK = 3, PUFF = 4, WISP = 5, SHARD = 6, CHIP = 7,
            LEAF = 8, MINI = 9, SLASH = 10, RING = 11, GLOW = 12;
    public static final int COUNT = 13;

    /** Falls under gravity. */
    public static final int FALL = 1;
    /** Shrinks to 40% over its life. */
    public static final int SHRINK = 2;
    /** Grows to 220% over its life (smoke, haze, rings). */
    public static final int GROW = 4;
    /** Spins. */
    public static final int SPIN = 8;
    /** Drifts side to side. */
    public static final int SWAY = 16;

    /** Collapses to 20% of its size (a ring closing in). */
    public static final int COLLAPSE = 32;

    private Mote() {}
}
