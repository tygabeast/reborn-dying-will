package com.reborndw.client.anim;

import com.reborndw.anim.MoveAnim;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Keyframed poses for weapon moves. Everything is written for a right-handed move (the weapon in the right arm) and
 * mirrored for the left. Third person: rotations (radians) of the weapon arm, the other arm and the torso's turn.
 * First person: an offset (blocks) and turn (degrees) of the held item or arm. Between keys the motion eases in and
 * out; at the start and end each move blends with the normal pose so nothing snaps.
 */
public final class MovePoses {
    private static final float PI = Mth.PI, HALF = Mth.HALF_PI;

    private MovePoses() {}

    // third person keys: t, weapon arm x y z, other arm x y z, torso turn
    private static final float[][] SLASH = {
            {0F, -2.6F, 0.1F, 0.7F, 0.2F, 0F, -0.1F, 0.35F},
            {.3F, -2.7F, 0.1F, 0.8F, 0.2F, 0F, -0.1F, 0.4F},
            {.6F, -1.1F, -0.9F, -0.2F, 0.3F, 0F, -0.15F, -0.35F},
            {1F, -0.5F, -0.7F, -0.4F, 0.2F, 0F, -0.1F, -0.4F}};
    private static final float[][] PUNCH = {
            {0F, 0.4F, 0F, 0.15F, -0.9F, 0.4F, 0F, 0.3F},
            {.3F, -1.57F, -0.1F, 0F, -0.8F, 0.4F, 0F, -0.3F},
            {1F, -0.3F, 0F, 0.05F, -0.6F, 0.3F, 0F, 0F}};
    private static final float[][] HEAVY_PUNCH = {
            {0F, 0.6F, 0F, 0.2F, -1.0F, 0.4F, 0F, 0.6F},
            {.35F, -1.6F, -0.15F, 0F, -0.6F, 0.4F, 0F, -0.55F},
            {1F, -0.6F, 0F, 0.05F, -0.5F, 0.3F, 0F, -0.2F}};
    private static final float[][] COUNTER = {{0F, -1.15F, -0.95F, 0F, -0.6F, 0.5F, 0F, -0.15F}, {1F, -1.15F, -0.95F, 0F, -0.6F, 0.5F, 0F, -0.15F}};
    // the arm stays high (like a spear throw) so the weapon points forward, then drives forward and down
    private static final float[][] THRUST = {
            {0F, -3.0F, 0F, 0.2F, -0.9F, 0.6F, 0F, 0.3F},
            {.4F, -3.1F, 0F, 0.25F, -0.9F, 0.6F, 0F, 0.35F},
            {.55F, -2.75F, -0.1F, 0F, -1.2F, 0.7F, 0F, -0.2F},
            {1F, -2.85F, -0.05F, 0F, -1.0F, 0.6F, 0F, -0.1F}};
    private static final float[][] SPIN = {{0F, -1.5F, 0F, 1.25F, -1.5F, 0F, -1.25F, 0F}, {1F, -1.5F, 0F, 1.25F, -1.5F, 0F, -1.25F, 0F}};
    private static final float[][] DASH = {{0F, 0.35F, 0F, 0.3F, 0.45F, 0F, -0.3F, 0F}, {1F, 0.35F, 0F, 0.3F, 0.45F, 0F, -0.3F, 0F}};
    private static final float[][] LASH = {
            {0F, -3.0F, 0F, 0.25F, 0.1F, 0F, -0.1F, 0.25F},
            {.45F, -3.05F, 0F, 0.25F, 0.1F, 0F, -0.1F, 0.3F},
            {.65F, -1.2F, -0.1F, 0F, 0.2F, 0F, -0.1F, -0.25F},
            {1F, -0.9F, -0.1F, 0F, 0.15F, 0F, -0.1F, -0.15F}};
    private static final float[][] BIND = {
            {0F, -3.0F, 0F, 0.25F, 0.1F, 0F, -0.1F, 0.25F},
            {.35F, -3.05F, 0F, 0.25F, 0.1F, 0F, -0.1F, 0.3F},
            {.55F, -1.2F, -0.1F, 0F, 0.2F, 0F, -0.1F, -0.25F},
            {1F, -0.55F, 0.1F, 0.3F, 0.2F, 0F, -0.1F, 0.2F}};
    private static final float[][] BRACE = {{0F, -0.94F, -0.52F, 0F, -0.3F, 0F, 0F, 0F}, {1F, -0.94F, -0.52F, 0F, -0.3F, 0F, 0F, 0F}};
    private static final float[][] BASH = {
            {0F, -0.94F, -0.52F, 0F, -0.3F, 0F, 0F, 0.2F},
            {.4F, -1.45F, -0.25F, 0F, 0.3F, 0F, -0.1F, -0.15F},
            {1F, -1.1F, -0.4F, 0F, 0F, 0F, 0F, 0F}};
    private static final float[][] SLAM = {
            {0F, -2.9F, -0.3F, 0F, -2.9F, 0.3F, 0F, 0F},
            {.45F, -3.0F, -0.3F, 0F, -3.0F, 0.3F, 0F, 0F},
            {.6F, -0.75F, -0.3F, 0F, -0.75F, 0.3F, 0F, 0F},
            {1F, -0.55F, -0.25F, 0F, -0.55F, 0.25F, 0F, 0F}};
    private static final float[][] SMASH = {
            {0F, -2.85F, -0.2F, 0.2F, -2.85F, 0.2F, -0.2F, 0F},
            {.35F, -0.6F, -0.3F, 0F, -0.6F, 0.3F, 0F, 0F},
            {1F, -0.5F, -0.25F, 0F, -0.5F, 0.25F, 0F, 0F}};
    private static final float[][] CHARGE_HEAVY = {{0F, -2.8F, -0.2F, 0.2F, -2.8F, 0.2F, -0.2F, 0F}, {1F, -2.8F, -0.2F, 0.2F, -2.8F, 0.2F, -0.2F, 0F}};
    private static final float[][] CHARGE_FIST = {{0F, 0.55F, 0F, 0.2F, -1.0F, 0.5F, 0F, 0.5F}, {1F, 0.55F, 0F, 0.2F, -1.0F, 0.5F, 0F, 0.5F}};

    private static final float[] V = new float[7];

    private static void sample(float[][] keys, float t, float[] out) {
        int n = out.length;
        if (t <= keys[0][0]) { System.arraycopy(keys[0], 1, out, 0, n); return; }
        for (int i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                float[] a = keys[i - 1], b = keys[i];
                float u = (t - a[0]) / Math.max(1e-4F, b[0] - a[0]);
                u = u * u * (3F - 2F * u);
                for (int k = 0; k < n; k++) out[k] = a[k + 1] + (b[k + 1] - a[k + 1]) * u;
                return;
            }
        }
        System.arraycopy(keys[keys.length - 1], 1, out, 0, n);
    }

    private static boolean held(MoveAnim a) {
        return a == MoveAnim.COUNTER || a == MoveAnim.BRACE || a == MoveAnim.CHARGE_HEAVY || a == MoveAnim.CHARGE_FIST;
    }

    /** How much of the move's pose shows: eases in at the start and back out at the end. */
    public static float weight(MoveAnim a, float t) {
        float in = Mth.clamp(t / .08F, 0F, 1F), out = Mth.clamp((1F - t) / (held(a) ? .1F : .15F), 0F, 1F);
        if (a == MoveAnim.SMASH || a == MoveAnim.HEAVY_PUNCH) in = 1F; // they follow straight on from a held charge
        return Math.min(in, out);
    }

    /** Fills V with the right-handed third-person pose; {@code hx}/{@code hy} = head pitch/yaw (already mirrored). */
    private static void pose(MoveAnim a, float t, float hx, float hy) {
        switch (a) {
            case SLASH -> sample(SLASH, t, V);
            case PUNCH -> sample(PUNCH, t, V);
            case HEAVY_PUNCH -> sample(HEAVY_PUNCH, t, V);
            case COUNTER -> sample(COUNTER, t, V);
            case THRUST -> sample(THRUST, t, V);
            case SPIN -> sample(SPIN, t, V);
            case DASH -> sample(DASH, t, V);
            case LASH -> sample(LASH, t, V);
            case BIND -> sample(BIND, t, V);
            case BRACE -> sample(BRACE, t, V);
            case BASH -> sample(BASH, t, V);
            case SLAM -> sample(SLAM, t, V);
            case SMASH -> sample(SMASH, t, V);
            case CHARGE_HEAVY -> { sample(CHARGE_HEAVY, t, V); V[0] += .05F * Mth.sin(t * 90F); V[3] += .05F * Mth.sin(t * 90F); }
            case CHARGE_FIST -> sample(CHARGE_FIST, t, V);
            case FLURRY -> { // four quick stabs, arms taking turns
                float q = t * 4F;
                int k = Math.min(3, (int) q);
                float j = Mth.sin((q - k) * PI);
                boolean weaponArm = (k & 1) == 0;
                V[0] = -0.5F - (weaponArm ? 1.1F * j : 0F); V[1] = -0.1F; V[2] = 0F;
                V[3] = -0.5F - (weaponArm ? 0F : 1.1F * j); V[4] = 0.1F; V[5] = 0F;
                V[6] = weaponArm ? -0.2F * j : 0.2F * j;
            }
            case BOW -> { // the bow arm points at the target, the other draws the string, then lets go
                float draw = Mth.clamp(t / .7F, 0F, 1F), release = t > .78F ? Mth.clamp((t - .78F) / .1F, 0F, 1F) : 0F;
                V[0] = -HALF + hx; V[1] = -0.1F + hy; V[2] = 0F;
                V[3] = -HALF + hx + release * 0.25F; V[4] = 0.1F + hy + 0.4F * draw - 0.7F * release; V[5] = -0.25F * release;
                V[6] = 0F;
            }
            case GUN -> { // aimed with both hands, kicked up by the recoil
                float kick = t < .35F ? Mth.sin(t / .35F * PI) : 0F;
                V[0] = -HALF + hx - 0.45F * kick; V[1] = -0.05F + hy; V[2] = 0F;
                V[3] = -HALF + hx - 0.35F * kick; V[4] = 0.45F + hy; V[5] = 0F;
                V[6] = 0F;
            }
        }
    }

    /** Poses a humanoid model (called after its own animation). */
    public static void apply(HumanoidModel<?> m, MoveAnim a, HumanoidArm arm, float t) {
        float side = arm == HumanoidArm.RIGHT ? 1F : -1F;
        pose(a, t, m.head.xRot, m.head.yRot * side);
        float w = weight(a, t);
        ModelPart wp = arm == HumanoidArm.RIGHT ? m.rightArm : m.leftArm, op = arm == HumanoidArm.RIGHT ? m.leftArm : m.rightArm;
        float body = Mth.lerp(w, m.body.yRot, V[6] * side);
        m.body.yRot = body;
        wp.xRot = Mth.lerp(w, wp.xRot, V[0]);
        wp.yRot = Mth.lerp(w, wp.yRot, V[1] * side + body);
        wp.zRot = Mth.lerp(w, wp.zRot, V[2] * side);
        op.xRot = Mth.lerp(w, op.xRot, V[3]);
        op.yRot = Mth.lerp(w, op.yRot, V[4] * side + body);
        op.zRot = Mth.lerp(w, op.zRot, V[5] * side);
        // the shoulders turn with the torso
        m.rightArm.z = Mth.sin(body) * 5F;
        m.rightArm.x = -Mth.cos(body) * 5F;
        m.leftArm.z = -Mth.sin(body) * 5F;
        m.leftArm.x = Mth.cos(body) * 5F;
        if (a == MoveAnim.THRUST) { // the shoulder drives forward on the jab (model z: forward is negative)
            float jab = t < .45F ? -Mth.clamp(t / .45F, 0F, 1F) : Mth.sin(Mth.clamp((t - .45F) / .55F, 0F, 1F) * PI);
            wp.z += -4F * jab * w;
        }
    }

    /** A whole-body turn for spins (radians around the vertical), else 0. */
    public static float spin(MoveAnim a, float t) {
        if (a != MoveAnim.SPIN) return 0F;
        float u = Mth.clamp(t / .85F, 0F, 1F);
        return u * u * (3F - 2F * u) * Mth.TWO_PI;
    }

    /** A forward lean for dashes and bashes (degrees), else 0. */
    public static float lean(MoveAnim a, float t) {
        return switch (a) {
            case DASH -> 25F * weight(a, t);
            case THRUST -> t < .45F ? -6F * weight(a, t) : 14F * Mth.sin(Mth.clamp((t - .45F) / .55F, 0F, 1F) * PI);
            case BASH -> 15F * Mth.sin(Mth.clamp(t / .7F, 0F, 1F) * PI);
            default -> 0F;
        };
    }

    // ------------------------------------------------------------------ first person

    // keys: t, offset x y z (blocks), turn x y z (degrees); right hand
    private static final float[][] FP_SLASH = {
            {0F, .15F, .3F, .1F, 0F, 0F, 50F},
            {.3F, .18F, .33F, .12F, 5F, 0F, 55F},
            {.6F, -.35F, -.05F, -.15F, -40F, 20F, -40F},
            {1F, -.45F, -.2F, -.05F, -20F, 10F, -60F}};
    private static final float[][] FP_PUNCH = {{0F, 0F, -.02F, .08F, 0F, 0F, 0F}, {.3F, -.3F, .22F, -.35F, 0F, 0F, 0F}, {1F, 0F, 0F, 0F, 0F, 0F, 0F}};
    private static final float[][] FP_HEAVY_PUNCH = {{0F, .05F, -.05F, .15F, 0F, 0F, 0F}, {.35F, -.32F, .24F, -.45F, 0F, 0F, 0F}, {1F, 0F, 0F, -.05F, 0F, 0F, 0F}};
    private static final float[][] FP_COUNTER = {{0F, -.3F, .12F, 0F, 0F, 0F, 75F}, {1F, -.3F, .12F, 0F, 0F, 0F, 75F}};
    private static final float[][] FP_THRUST = {
            {0F, 0F, .05F, .2F, 0F, 0F, 0F}, {.4F, 0F, .06F, .22F, 5F, 0F, 0F},
            {.55F, -.05F, .05F, -.45F, -55F, 0F, 0F}, {1F, 0F, 0F, -.3F, -50F, 0F, 0F}};
    private static final float[][] FP_SPIN = {{0F, .25F, 0F, 0F, 0F, 0F, 0F}, {.5F, -.1F, .05F, -.2F, 0F, 45F, 0F}, {1F, -.45F, 0F, -.05F, 0F, 90F, 0F}};
    private static final float[][] FP_DASH = {{0F, 0F, -.1F, -.25F, -15F, 0F, 0F}, {1F, 0F, -.1F, -.25F, -15F, 0F, 0F}};
    private static final float[][] FP_LASH = {
            {0F, .05F, .35F, .15F, 50F, 0F, 0F}, {.45F, .05F, .37F, .16F, 55F, 0F, 0F},
            {.65F, 0F, -.05F, -.25F, -60F, 0F, 0F}, {1F, 0F, -.1F, -.15F, -40F, 0F, 0F}};
    private static final float[][] FP_BIND = {
            {0F, .05F, .35F, .15F, 50F, 0F, 0F}, {.35F, .05F, .37F, .16F, 55F, 0F, 0F},
            {.55F, 0F, -.05F, -.25F, -60F, 0F, 0F}, {1F, .05F, 0F, .1F, 10F, 0F, 0F}};
    private static final float[][] FP_BRACE = {{0F, -.32F, .14F, .05F, 0F, -15F, 0F}, {1F, -.32F, .14F, .05F, 0F, -15F, 0F}};
    private static final float[][] FP_BASH = {{0F, -.32F, .14F, .05F, 0F, -15F, 0F}, {.4F, -.3F, .14F, -.3F, 0F, -15F, 0F}, {1F, -.32F, .14F, 0F, 0F, -15F, 0F}};
    private static final float[][] FP_SLAM = {
            {0F, 0F, .45F, .1F, 60F, 0F, 0F}, {.45F, 0F, .47F, .1F, 62F, 0F, 0F},
            {.6F, 0F, -.2F, -.2F, -50F, 0F, 0F}, {1F, 0F, -.15F, -.1F, -30F, 0F, 0F}};
    private static final float[][] FP_SMASH = {{0F, 0F, .42F, .1F, 60F, 0F, 0F}, {.35F, 0F, -.2F, -.2F, -50F, 0F, 0F}, {1F, 0F, -.15F, -.1F, -30F, 0F, 0F}};
    private static final float[][] FP_CHARGE_HEAVY = {{0F, 0F, .4F, .15F, 60F, 0F, 0F}, {1F, 0F, .4F, .15F, 60F, 0F, 0F}};
    private static final float[][] FP_CHARGE_FIST = {{0F, .05F, -.05F, .15F, 0F, 0F, 0F}, {1F, .05F, -.05F, .15F, 0F, 0F, 0F}};

    /** First-person offset and turn for the moving hand (right-handed; mirror x, turn y and z for the left). */
    public static void firstPerson(MoveAnim a, float t, float[] out) {
        switch (a) {
            case SLASH -> sample(FP_SLASH, t, out);
            case PUNCH -> sample(FP_PUNCH, t, out);
            case HEAVY_PUNCH -> sample(FP_HEAVY_PUNCH, t, out);
            case COUNTER -> sample(FP_COUNTER, t, out);
            case THRUST -> sample(FP_THRUST, t, out);
            case SPIN -> sample(FP_SPIN, t, out);
            case DASH -> sample(FP_DASH, t, out);
            case LASH -> sample(FP_LASH, t, out);
            case BIND -> sample(FP_BIND, t, out);
            case BRACE -> sample(FP_BRACE, t, out);
            case BASH -> sample(FP_BASH, t, out);
            case SLAM -> sample(FP_SLAM, t, out);
            case SMASH -> sample(FP_SMASH, t, out);
            case CHARGE_HEAVY -> { sample(FP_CHARGE_HEAVY, t, out); out[1] += .01F * Mth.sin(t * 90F); }
            case CHARGE_FIST -> sample(FP_CHARGE_FIST, t, out);
            case FLURRY -> {
                float q = t * 4F;
                int k = Math.min(3, (int) q);
                float j = (k & 1) == 0 ? Mth.sin((q - k) * PI) : 0F;
                out[0] = -.06F * j; out[1] = .03F * j; out[2] = -.3F * j; out[3] = -10F * j; out[4] = 0F; out[5] = 0F;
            }
            case GUN -> {
                float kick = t < .35F ? Mth.sin(t / .35F * PI) : 0F;
                out[0] = -.2F; out[1] = .05F; out[2] = -.05F + .08F * kick; out[3] = 18F * kick; out[4] = 0F; out[5] = 0F;
            }
            case BOW -> java.util.Arrays.fill(out, 0F); // handled specially (the vanilla bow-drawing hold)
        }
    }
}
