package com.reborndw.client.particle;

import com.reborndw.particle.Mote;
import com.reborndw.particle.MoteOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;

/** A flame mote: a tinted, glowing sprite that fades from one colour to another. Cheap: one quad, no collision. */
public class MoteParticle extends TextureSheetParticle {
    private final int r0, g0, b0, r1, g1, b1;
    private final float startSize, endSize, spinSpeed, swayAmount, swayPhase;
    private final boolean sway;
    private final int kind;

    MoteParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, MoteOptions o, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.kind = o.kind();
        setSprite(sprites.get(Mth.clamp(kind, 0, Mote.COUNT - 1), Mote.COUNT - 1));
        int c0 = o.color0(), c1 = o.color1();
        r0 = (c0 >> 16) & 255; g0 = (c0 >> 8) & 255; b0 = c0 & 255;
        r1 = (c1 >> 16) & 255; g1 = (c1 >> 8) & 255; b1 = c1 & 255;
        setColor(r0 / 255f, g0 / 255f, b0 / 255f);
        this.lifetime = Math.max(2, o.life());
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = (o.flags() & Mote.FALL) != 0 ? 0.55F : 0F;
        float size = o.size() * 0.5F;
        this.startSize = size;
        this.endSize = (o.flags() & Mote.COLLAPSE) != 0 ? size * 0.2F : (o.flags() & Mote.SHRINK) != 0 ? size * 0.4F : (o.flags() & Mote.GROW) != 0 ? size * 2.2F : size;
        this.quadSize = size;
        this.spinSpeed = (o.flags() & Mote.SPIN) != 0 ? (random.nextFloat() - 0.5F) * 0.4F : 0F;
        this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
        this.sway = (o.flags() & Mote.SWAY) != 0;
        this.swayAmount = 0.004F + random.nextFloat() * 0.004F;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        // upright motes (sparks, rings, slashes) read better without a random roll
        if (kind == Mote.RING || kind == Mote.GLOW) this.roll = this.oRoll = 0;
        this.alpha = 1F;
    }

    @Override
    public void tick() {
        super.tick();
        float u = (float) age / lifetime;
        float inv = 1F - u;
        setColor((r0 + (r1 - r0) * u) / 255f, (g0 + (g1 - g0) * u) / 255f, (b0 + (b1 - b0) * u) / 255f);
        alpha = kind == Mote.PUFF || kind == Mote.WISP ? 0.7F * inv : inv < 0.35F ? inv / 0.35F : 1F;
        quadSize = startSize + (endSize - startSize) * u;
        oRoll = roll;
        roll += spinSpeed;
        if (sway) xd += Mth.sin(age * 0.45F + swayPhase) * swayAmount;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // flames glow
    }

    public static class Provider implements ParticleProvider<MoteOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(MoteOptions o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new MoteParticle(level, x, y, z, vx, vy, vz, o, sprites);
        }
    }
}
