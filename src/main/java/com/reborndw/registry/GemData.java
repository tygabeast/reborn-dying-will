package com.reborndw.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/** A Flame Gem's attunement and stored FV. */
public record GemData(Optional<Flame> flame, long fv) {
    public static final GemData EMPTY = new GemData(Optional.empty(), 0);

    public static final Codec<GemData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Flame.CODEC.optionalFieldOf("flame").forGetter(GemData::flame),
            Codec.LONG.optionalFieldOf("fv", 0L).forGetter(GemData::fv)
    ).apply(i, GemData::new));

    public static final StreamCodec<ByteBuf, GemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Flame.STREAM_CODEC), GemData::flame,
            ByteBufCodecs.VAR_LONG, GemData::fv,
            GemData::new);

    public Rank rank() { return Rank.forGemFv(fv); }

    public GemData withFv(long newFv) { return new GemData(flame, newFv); }
}
