package com.reborndw.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** A permanent infusion from the Infusion Anvil: the gem's flame and rank, fixed on the item. */
public record Infusion(Flame flame, Rank grade) {
    public static final Codec<Infusion> CODEC = RecordCodecBuilder.create(i -> i.group(
            Flame.CODEC.fieldOf("flame").forGetter(Infusion::flame),
            Rank.CODEC.fieldOf("grade").forGetter(Infusion::grade)
    ).apply(i, Infusion::new));

    public static final StreamCodec<ByteBuf, Infusion> STREAM_CODEC = StreamCodec.composite(
            Flame.STREAM_CODEC, Infusion::flame,
            Rank.STREAM_CODEC, Infusion::grade,
            Infusion::new);
}
