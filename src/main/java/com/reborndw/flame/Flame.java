package com.reborndw.flame;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The fifteen Dying Will Flames. */
public enum Flame implements StringRepresentable {
    SKY(Family.SKY, 0xFF8A1E, "Harmony"),
    STORM(Family.SKY, 0xD7263D, "Disintegration"),
    RAIN(Family.SKY, 0x2F80ED, "Tranquility"),
    SUN(Family.SKY, 0xFFD93B, "Activation"),
    LIGHTNING(Family.SKY, 0x3DDC64, "Hardening"),
    CLOUD(Family.SKY, 0x8E44EC, "Propagation"),
    MIST(Family.SKY, 0x6C3FA8, "Construction"),
    EARTH(Family.EARTH, 0xB5482B, "Gravity"),
    GLACIER(Family.EARTH, 0x9FE6FF, "Freezing"),
    FOREST(Family.EARTH, 0x2E8B3A, "Growth"),
    MOUNTAIN(Family.EARTH, 0x8C6A4A, "Landform"),
    SWAMP(Family.EARTH, 0x5D6B2F, "Fermentation"),
    DESERT(Family.EARTH, 0xE3C07A, "Mirage"),
    VOLCANO(Family.EARTH, 0xFF4A12, "Eruption"),
    NIGHT(Family.NIGHT, 0x1B1033, "Warp");

    public enum Family { SKY, EARTH, NIGHT }

    public static final Codec<Flame> CODEC = StringRepresentable.fromEnum(Flame::values);
    public static final StreamCodec<ByteBuf, Flame> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], Flame::ordinal);

    public final Family family;
    public final int color;
    public final String property;

    Flame(Family family, int color, String property) {
        this.family = family;
        this.color = color;
        this.property = property;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String translationKey() { return "flame.reborn_dying_will." + id(); }

    @Override
    public String getSerializedName() { return id(); }

    public static Flame byId(String id) {
        for (Flame f : values()) if (f.id().equals(id)) return f;
        return null;
    }

    /** The seven flames a natural awakening can reveal. */
    public static final Flame[] SKY_SET = {SKY, STORM, RAIN, SUN, LIGHTNING, CLOUD, MIST};
    public static final Flame[] EARTH_SET = {EARTH, GLACIER, FOREST, MOUNTAIN, SWAMP, DESERT, VOLCANO};
}
