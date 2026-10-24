package com.reborndw.registry;

import com.mojang.serialization.MapCodec;
import com.reborndw.RebornDW;
import com.reborndw.particle.MoteOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, RebornDW.MODID);

    /** The one flame particle; its options pick the sprite, colours, size and behaviour. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MoteOptions>> MOTE = PARTICLES.register("mote",
            () -> new ParticleType<MoteOptions>(false) {
                @Override
                public MapCodec<MoteOptions> codec() { return MoteOptions.CODEC; }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, MoteOptions> streamCodec() { return MoteOptions.STREAM_CODEC; }
            });

    private ModParticles() {}
}
