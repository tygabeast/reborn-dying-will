package com.reborndw.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** A ring's flame and grade, fixed by the gem set into it. */
public record RingData(Flame flame, Rank grade) {
    public static final Codec<RingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Flame.CODEC.fieldOf("flame").forGetter(RingData::flame),
            Rank.CODEC.fieldOf("grade").forGetter(RingData::grade)
    ).apply(i, RingData::new));

    public static final StreamCodec<ByteBuf, RingData> STREAM_CODEC = StreamCodec.composite(
            Flame.STREAM_CODEC, RingData::flame,
            Rank.STREAM_CODEC, RingData::grade,
            RingData::new);
}
