package com.reborndw.fx;

import com.reborndw.flame.Flame;
import com.reborndw.network.FlameFxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Flame effects sent from the server to nearby clients, which draw them in the flame's own look (see the client's
 * {@code FxManager}). One small packet per effect, only to players within 48 blocks.
 */
public final class FlameFx {
    public static final int HIT = 0, BOLT = 1, SLASH = 2, THRUST = 3, SLAM = 4, TETHER = 5, CHARGE = 6, SHIELD = 7, BURST = 8, MUZZLE = 9;
    private static final double RANGE = 48;

    private FlameFx() {}

    public static void send(ServerLevel level, int kind, Flame f, Vec3 p, Vec3 q, float size, int extra) {
        PacketDistributor.sendToPlayersNear(level, null, p.x, p.y, p.z, RANGE,
                new FlameFxPayload(kind, f.ordinal(), (float) p.x, (float) p.y, (float) p.z, (float) q.x, (float) q.y, (float) q.z, size, extra));
    }

    private static Vec3 mid(Entity e) { return e.position().add(0, e.getBbHeight() * .55, 0); }

    /** A flame landing on something. */
    public static void hit(ServerLevel level, Flame f, Entity target, Entity from, float size) {
        Vec3 at = mid(target);
        send(level, HIT, f, at, from == null ? Vec3.ZERO : at.subtract(mid(from)).normalize(), size, 0);
    }

    public static void burst(ServerLevel level, Flame f, Vec3 at, float size) { send(level, BURST, f, at, Vec3.ZERO, size, 0); }

    /** A jagged arc between two points (Lightning's chains; other flames use a tether). */
    public static void bolt(ServerLevel level, Flame f, Vec3 a, Vec3 b) { send(level, BOLT, f, a, b, 1F, 0); }

    public static void bolt(ServerLevel level, Flame f, Entity a, Entity b) { bolt(level, f, mid(a), mid(b)); }

    public static void tether(ServerLevel level, Flame f, Entity a, Entity b) { send(level, TETHER, f, mid(a), mid(b), 1F, 0); }

    /** A crescent swept around the player: {@code degrees} of arc, tilted by {@code roll} degrees. */
    public static void slash(ServerPlayer p, Flame f, double radius, int degrees, int roll) {
        Vec3 at = p.position().add(0, p.getBbHeight() * .62, 0);
        send(p.serverLevel(), SLASH, f, at, p.getLookAngle(), (float) radius, degrees | ((roll + 180) << 10));
    }

    /** A streak from the player along the look direction. */
    public static void thrust(ServerPlayer p, Flame f, double length, float width) {
        Vec3 a = p.getEyePosition().add(0, -.35, 0).add(p.getLookAngle().scale(.5));
        send(p.serverLevel(), THRUST, f, a, a.add(p.getLookAngle().scale(length)), width, 0);
    }

    public static void slam(ServerLevel level, Flame f, Vec3 centre, double radius) { send(level, SLAM, f, centre, Vec3.ZERO, (float) radius, 0); }

    /** The glow gathering in a hand while a strike charges ({@code charge} 0..1). */
    public static void charge(ServerPlayer p, Flame f, double charge) {
        send(p.serverLevel(), CHARGE, f, hand(p, InteractionHand.MAIN_HAND), Vec3.ZERO, (float) charge, 0);
    }

    /** A flame barrier in front of the player (shield brace and blocks, blade counter). */
    public static void shield(ServerPlayer p, Flame f, float size) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        send(p.serverLevel(), SHIELD, f, p.position().add(0, p.getBbHeight() * .55, 0).add(look.scale(.7)), look, size, 0);
    }

    public static void muzzle(ServerPlayer p, Flame f) {
        send(p.serverLevel(), MUZZLE, f, p.getEyePosition().add(0, -.2, 0).add(p.getLookAngle().scale(.9)), p.getLookAngle(), 1F, 0);
    }

    /** Roughly where a hand is, for effects that start there. */
    public static Vec3 hand(ServerPlayer p, InteractionHand hand) {
        double yaw = Math.toRadians(p.getYRot());
        double side = (hand == InteractionHand.MAIN_HAND) == (p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) ? -.35 : .35;
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        return p.position().add(0, p.getBbHeight() * .5, 0).add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(.45)).add(right.scale(side));
    }
}
