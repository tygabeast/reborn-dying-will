package com.reborndw.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reborndw.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One flame mote: a kind (which sprite), a colour that fades from {@code color0} to {@code color1}, a size in blocks
 * (full width), a lifetime in ticks and behaviour flags (see {@link Mote}). Motion comes from the speed arguments of
 * {@code addParticle}.
 */
public record MoteOptions(int kind, int color0, int color1, float size, int life, int flags) implements ParticleOptions {
    public static final MapCodec<MoteOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("kind").forGetter(MoteOptions::kind),
            Codec.INT.fieldOf("color0").forGetter(MoteOptions::color0),
            Codec.INT.fieldOf("color1").forGetter(MoteOptions::color1),
            Codec.FLOAT.fieldOf("size").forGetter(MoteOptions::size),
            Codec.INT.fieldOf("life").forGetter(MoteOptions::life),
            Codec.INT.fieldOf("flags").forGetter(MoteOptions::flags)).apply(i, MoteOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoteOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoteOptions::kind,
            ByteBufCodecs.INT, MoteOptions::color0,
            ByteBufCodecs.INT, MoteOptions::color1,
            ByteBufCodecs.FLOAT, MoteOptions::size,
            ByteBufCodecs.VAR_INT, MoteOptions::life,
            ByteBufCodecs.VAR_INT, MoteOptions::flags,
            MoteOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.MOTE.get();
    }
}
