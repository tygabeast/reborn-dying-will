package com.reborndw.client.look;

import com.reborndw.config.ClientConfig;
import com.reborndw.particle.MoteOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Throws the motes of a flame's look: from a head each tick, or along a shot's path. No state is kept. */
public final class FlameEmitter {
    private static final float U = 1F / 48F;

    private FlameEmitter() {}

    /** 1 for all particles, less when the player has turned particles down. */
    private static float density(Minecraft mc) {
        float d = (float) (double) ClientConfig.FLAME_DENSITY.get();
        ParticleStatus s = mc.options.particles().get();
        return s == ParticleStatus.ALL ? d : s == ParticleStatus.DECREASED ? d * .45F : d * .15F;
    }

    private static int count(float rate, float scale, RandomSource r) {
        float n = rate * .05F * scale;
        int whole = (int) n;
        return whole + (r.nextFloat() < n - whole ? 1 : 0);
    }

    private static float between(RandomSource r, float lo, float hi) { return lo + (hi - lo) * r.nextFloat(); }

    /** Motes for the flame on this player's head; call once a tick. {@code k} is the world size of one unit. */
    public static void head(Minecraft mc, Entity p, FlameLook L, boolean calm, float k) {
        float dens = density(mc) * (calm ? .55F : 1F);
        if (dens <= 0F || L.head.isEmpty()) return;
        RandomSource r = p.getRandom();
        double yaw = Math.toRadians(p.getYHeadRot()), pitch = Math.toRadians(p.getXRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = Math.cos(yaw), rz = Math.sin(yaw);
        // the flame's base in head space (see HeadFlameLayer), turned by the head's pitch, then into the world
        double up = com.reborndw.client.HeadFlameLayer.BROW_UP + ClientConfig.FLAME_RAISE.get();
        double fwd = com.reborndw.client.HeadFlameLayer.BROW_FORWARD + ClientConfig.FLAME_FORWARD.get();
        double up2 = up * Math.cos(pitch) - fwd * Math.sin(pitch), fwd2 = fwd * Math.cos(pitch) + up * Math.sin(pitch);
        double model = 0.9375 * (p.getBbHeight() / 1.8); // player model scale (baby/scaled players follow their size)
        double pivot = (p.isCrouching() ? 1.501 - 0.2625 : 1.501) * model;
        double bx = p.getX() + fx * fwd2 * model, bz = p.getZ() + fz * fwd2 * model;
        double by = p.getY() + pivot + up2 * model;
        for (FlameLook.Emit e : L.head) {
            int n = count(e.rate, dens, r);
            for (int i = 0; i < n; i++) {
                float side = e.ox * (r.nextFloat() * 2F - 1F), lift = between(r, e.oyLo, e.oyHi);
                double vs = e.vx * (r.nextFloat() * 2F - 1F), vy = between(r, e.vyLo, e.vyHi), vf = 0;
                double px, pz;
                if (e.ringHi > 0F) { // spawns on a ring and drifts inward
                    float ang = r.nextFloat() * Mth.TWO_PI, rad = between(r, e.ringLo, e.ringHi), pull = between(r, e.pullLo, e.pullHi);
                    double cx = Math.cos(ang), cz = Math.sin(ang);
                    px = bx + cx * rad * k; pz = bz + cz * rad * k;
                    vs = -cx * pull; vf = -cz * pull;
                    emit(mc, e, L, k, px, by + lift * k, pz, vs * k / 20, vy * k / 20, vf * k / 20, r);
                    continue;
                }
                px = bx + rx * side * k; pz = bz + rz * side * k;
                emit(mc, e, L, k, px, by + lift * k, pz, rx * vs * k / 20, vy * k / 20, rz * vs * k / 20, r);
            }
        }
    }

    /** Motes along a flame shot's path; call once a tick on the client. */
    public static void shot(Minecraft mc, Entity s, FlameLook L) {
        if (L.trail.isEmpty()) return;
        float dens = density(mc);
        if (dens <= 0F) return;
        RandomSource r = s.getRandom();
        Vec3 m = s.getDeltaMovement();
        double len = m.length();
        Vec3 dir = len > 1e-4 ? m.scale(1 / len) : Vec3.ZERO;
        float k = U * L.sz;
        for (FlameLook.Emit e : L.trail) {
            int n = count(e.rate, dens, r);
            for (int i = 0; i < n; i++) {
                double along = r.nextDouble(); // anywhere on the last tick's move
                double back = between(r, e.backLo, e.backHi) * k;
                double x = s.getX() - m.x * along - dir.x * back, y = s.getY() - m.y * along - dir.y * back, z = s.getZ() - m.z * along - dir.z * back;
                float sp = e.vx, vy = between(r, e.vyLo, e.vyHi);
                emit(mc, e, L, k, x, y, z, (r.nextFloat() * 2F - 1F) * sp * k / 20, vy * k / 20, (r.nextFloat() * 2F - 1F) * sp * k / 20, r);
            }
        }
    }

    private static void emit(Minecraft mc, FlameLook.Emit e, FlameLook L, float k, double x, double y, double z,
                             double vx, double vy, double vz, RandomSource r) {
        int c0 = e.c0 != 0 ? e.c0 : L.core, c1 = e.c1 != 0 ? e.c1 : L.edge;
        int life = Math.max(2, Math.round(between(r, e.lifeLo, e.lifeHi) * 20F));
        mc.level.addParticle(new MoteOptions(e.kind, c0, c1, e.size * k, life, e.flags), x, y, z, vx, vy, vz);
    }
}
