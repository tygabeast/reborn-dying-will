package com.reborndw.flame;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Purity ranks, E lowest to S highest. */
public enum Rank implements StringRepresentable {
    E, D, C, B, A, S;

    public static final Codec<Rank> CODEC = StringRepresentable.fromEnum(Rank::values);
    public static final StreamCodec<ByteBuf, Rank> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], Rank::ordinal);

    /** Total FV a Flame Gem must hold to reach each rank: ten times the rank's minimum flame value. */
    public static final long[] GEM_FV = {1_000L, 10_000L, 50_000L, 200_000L, 500_000L, 1_000_000L};

    @Override
    public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }

    public boolean atLeast(Rank other) { return ordinal() >= other.ordinal(); }

    public static Rank min(Rank a, Rank b) { return a.ordinal() <= b.ordinal() ? a : b; }

    /** The rank a gem holding this much FV has, or null if it is still unranked. */
    public static Rank forGemFv(long fv) {
        Rank r = null;
        for (Rank rank : values()) if (fv >= GEM_FV[rank.ordinal()]) r = rank;
        return r;
    }

    public Rank next() { return this == S ? null : values()[ordinal() + 1]; }
}
