package com.reborndw.ability;

import com.reborndw.config.KitConfig;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Rank;
import com.reborndw.infusion.InfusionHelper;
import com.reborndw.infusion.WeaponCategory;
import com.reborndw.network.ActionPayload;
import com.reborndw.registry.ModRegistry;
import com.reborndw.fx.FlameFx;
import com.reborndw.util.DelayedTasks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * An infused weapon's Primary and Secondary: the category gives the shape of the move, the flame what it does
 * (all damage goes through FlameDamage, so the flame's property rides on every hit). Primary from D, Secondary from C.
 */
public final class WeaponMoves {
    public static final Rank PRIMARY_RANK = Rank.D, SECONDARY_RANK = Rank.C;
    private static final int MAX_CHARGE = 30;

    private WeaponMoves() {}

    public static void use(ServerPlayer player, FlameData d, InfusionHelper.Source s, ActionPayload.Action action) {
        boolean primary = action == ActionPayload.Action.PRIMARY;
        Rank need = primary ? PRIMARY_RANK : SECONDARY_RANK;
        if (!s.eff().atLeast(need)) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.needs_rank", need.name()), true);
            return;
        }
        boolean charging = !primary && (s.category() == WeaponCategory.HEAVY || s.category() == WeaponCategory.FISTS);
        if (charging) { // hold to charge, release to strike; paid on release
            if (d.chargeCategory != null) return;
            d.chargeCategory = s.category();
            d.chargeTicks = 0;
            d.heldAction = action;
            return;
        }
        if (!Kits.spend(player, primary ? KitConfig.MOVE_PRIMARY_COST.get() : KitConfig.MOVE_SECONDARY_COST.get())) return;
        if (primary) primary(player, d, s);
        else secondary(player, d, s);
    }

    /** The key was let go: a charged strike lands. */
    public static boolean release(ServerPlayer player, FlameData d, ActionPayload.Action action) {
        if (d.chargeCategory == null || action != ActionPayload.Action.SECONDARY) return false;
        WeaponCategory cat = d.chargeCategory;
        double charge = Math.min(1.0, d.chargeTicks / (double) MAX_CHARGE);
        d.chargeCategory = null;
        d.chargeTicks = 0;
        InfusionHelper.Source s = InfusionHelper.source(player);
        if (s == null || s.category() != cat) return true; // swapped weapons mid-charge
        if (!Kits.spend(player, KitConfig.MOVE_SECONDARY_COST.get())) return true;
        LivingEntity t = target(player, cat == WeaponCategory.HEAVY ? 4.5 : 3.5, 25);
        if (t == null) {
            sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F);
            return true;
        }
        float base = cat == WeaponCategory.HEAVY ? attack(player) : fistDamage(player, s);
        float dmg = (float) (base * (1.5 + 1.5 * charge) * s.strength());
        player.swing(s.hand(), true);
        hit(player, s, t, dmg);
        InfusionEffectsBridge.knock(player, t, 0.6 + charge);
        if (cat == WeaponCategory.HEAVY) FlameFx.slam(player.serverLevel(), s.flame(), t.position(), 1.5 + 1.5 * charge);
        else FlameFx.burst(player.serverLevel(), s.flame(), t.position().add(0, t.getBbHeight() / 2, 0), (float) (1 + charge));
        sound(player, cat == WeaponCategory.HEAVY ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.PLAYER_ATTACK_STRONG, 1.0F);
        return true;
    }

    /** Counters, braces and charging. */
    public static void tick(ServerPlayer player, FlameData d) {
        if (d.counterTicks > 0) d.counterTicks--;
        if (d.braceTicks > 0) d.braceTicks--;
        if (d.chargeCategory != null) {
            if (d.chargeTicks < MAX_CHARGE) d.chargeTicks++;
            if (d.chargeTicks % 5 == 0 && d.active != null) FlameFx.charge(player, d.active, d.chargeTicks / (double) MAX_CHARGE);
        }
    }

    // ------------------------------------------------------------------ primaries

    private static void primary(ServerPlayer player, FlameData d, InfusionHelper.Source s) {
        ServerLevel level = player.serverLevel();
        player.swing(s.hand(), true);
        switch (s.category()) {
            case BLADE -> { // flame slash arc
                for (LivingEntity e : cone(player, 4.0, 50)) hit(player, s, e, attack(player) * (float) s.strength());
                FlameFx.slash(player, s.flame(), 3.0, 110, -25);
                sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F);
            }
            case HEAVY -> { // ground slam
                for (LivingEntity e : Kits.enemiesAround(player, player.position(), 3.0)) {
                    hit(player, s, e, attack(player) * 0.8F * (float) s.strength());
                    e.push(0, 0.5, 0);
                    e.hurtMarked = true;
                }
                FlameFx.slam(level, s.flame(), player.position(), 3.0);
                sound(player, SoundEvents.MACE_SMASH_GROUND, 1.0F);
            }
            case POLEARM -> { // piercing thrust line
                for (LivingEntity e : line(player, 6.0, 1.0)) hit(player, s, e, attack(player) * (float) s.strength());
                FlameFx.thrust(player, s.flame(), 6.0, 1F);
                sound(player, SoundEvents.TRIDENT_THROW.value(), 1.2F);
            }
            case SHORT -> { // strike flurry: four quick hits
                LivingEntity t = target(player, 3.5, 40);
                if (t == null) { sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F); return; }
                float dmg = attack(player) * 0.4F * (float) s.strength();
                for (int i = 0; i < 4; i++) {
                    final int n = i;
                    DelayedTasks.schedule(i * 2, () -> {
                        if (!t.isAlive() || player.isRemoved()) return;
                        t.invulnerableTime = 0;
                        if (n > 0) player.swing(n % 2 == 0 ? s.hand() : other(s.hand()), true);
                        hit(player, s, t, dmg);
                        FlameFx.slash(player, s.flame(), 1.6, 70, n % 2 == 0 ? -40 : 40);
                        sound(player, SoundEvents.PLAYER_ATTACK_WEAK, 1.4F);
                    });
                }
            }
            case WHIP -> { // lash that pulls the target in
                LivingEntity t = lineTarget(player, 8.0);
                if (t == null) { sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F); return; }
                hit(player, s, t, attack(player) * 0.6F * (float) s.strength());
                Vec3 pull = player.position().subtract(t.position()).normalize().scale(1.2);
                t.push(pull.x, 0.3, pull.z);
                t.hurtMarked = true;
                FlameFx.tether(level, s.flame(), player, t);
                sound(player, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.4F);
            }
            case BOW -> { shoot(player, s, 0, 3.0, KitConfig.FLAME_ARROW_DAMAGE.get(), 32); FlameFx.muzzle(player, s.flame()); }
            case GUN -> {
                shoot(player, s, 0, 4.0, KitConfig.FLAME_ROUND_DAMAGE.get(), 48);
                FlameFx.muzzle(player, s.flame());
                sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F);
            }
            case SHIELD -> { // brace: blocks frontal hits and sends projectiles back
                d.braceTicks = 40;
                sound(player, SoundEvents.SHIELD_BLOCK, 0.8F);
                FlameFx.shield(player, s.flame(), 1.2F);
            }
            case FISTS -> { // flame punch
                LivingEntity t = target(player, 3.5, 30);
                if (t == null) { sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F); return; }
                hit(player, s, t, fistDamage(player, s));
                InfusionEffectsBridge.knock(player, t, s.flame() == Flame.SUN ? 1.2 : 0.8);
                FlameFx.thrust(player, s.flame(), Math.min(3.0, player.distanceTo(t)), .6F);
                FlameFx.burst(level, s.flame(), t.position().add(0, t.getBbHeight() * 0.6, 0), 1F);
                sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F);
            }
        }
    }

    // ------------------------------------------------------------------ secondaries

    private static void secondary(ServerPlayer player, FlameData d, InfusionHelper.Source s) {
        ServerLevel level = player.serverLevel();
        player.swing(s.hand(), true);
        switch (s.category()) {
            case BLADE -> { // counter stance: the next melee hit within a second is turned back
                d.counterTicks = 20;
                sound(player, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.4F);
                FlameFx.shield(player, s.flame(), .9F);
            }
            case POLEARM -> { // sweeping spin
                for (LivingEntity e : Kits.enemiesAround(player, player.position().add(0, 1, 0), 3.5)) {
                    hit(player, s, e, attack(player) * 0.8F * (float) s.strength());
                    InfusionEffectsBridge.knock(player, e, 0.8);
                }
                FlameFx.slash(player, s.flame(), 3.0, 360, 0);
                sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F);
            }
            case SHORT -> { // dash strike
                Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
                for (LivingEntity e : line(player, 5.0, 1.2)) hit(player, s, e, attack(player) * 1.2F * (float) s.strength());
                player.setDeltaMovement(look.scale(1.6).add(0, 0.15, 0));
                player.hurtMarked = true;
                FlameFx.thrust(player, s.flame(), 5.0, 1.2F);
                sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F);
            }
            case WHIP -> { // bind that roots the target (Cloud: the bind spreads to a second enemy)
                LivingEntity t = lineTarget(player, 8.0);
                if (t == null) { sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F); return; }
                bind(player, s, t);
                FlameFx.tether(level, s.flame(), player, t);
                FlameFx.slam(level, s.flame(), t.position(), .9);
                if (s.flame() == Flame.CLOUD) {
                    level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4),
                                    e -> e != t && e != player && e.isAlive() && !SkyAbilities.isAlly(player, e))
                            .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(t)))
                            .ifPresent(second -> { bind(player, s, second); FlameFx.tether(level, s.flame(), t, second); FlameFx.slam(level, s.flame(), second.position(), .9); });
                }
                sound(player, SoundEvents.CHAIN_PLACE, 1.0F);
            }
            case BOW -> { // spread shot
                for (int i = -2; i <= 2; i++) shoot(player, s, i * 10, 3.0, KitConfig.FLAME_ARROW_DAMAGE.get() * 0.7, 24);
                FlameFx.muzzle(player, s.flame());
            }
            case GUN -> { // burst fire
                for (int i = 0; i < 3; i++) {
                    DelayedTasks.schedule(i * 3, () -> {
                        if (player.isRemoved()) return;
                        shoot(player, s, 0, 4.0, KitConfig.FLAME_ROUND_DAMAGE.get() * 0.8, 48);
                        FlameFx.muzzle(player, s.flame());
                        sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 1.8F);
                    });
                }
            }
            case SHIELD -> { // charging bash (Lightning: the bash chains to every enemy nearby)
                Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
                player.setDeltaMovement(look.scale(1.3).add(0, 0.1, 0));
                player.hurtMarked = true;
                FlameFx.shield(player, s.flame(), 1.1F);
                FlameFx.thrust(player, s.flame(), 3.0, 1.4F);
                float dmg = Math.max(4F, attack(player)) * 0.6F * (float) s.strength();
                for (LivingEntity e : cone(player, 3.0, 60)) {
                    hit(player, s, e, dmg);
                    InfusionEffectsBridge.knock(player, e, 1.5);
                }
                if (s.flame() == Flame.LIGHTNING) {
                    for (LivingEntity e : Kits.enemiesAround(player, player.position().add(0, 1, 0), 4.0)) {
                        e.invulnerableTime = 0;
                        FlameDamage.deal(player, e, Flame.LIGHTNING, dmg * 0.5F, s.eff(), FlameDamage.NO_CHAIN);
                        FlameFx.bolt(level, Flame.LIGHTNING, player, e);
                    }
                }
                sound(player, SoundEvents.SHIELD_BLOCK, 0.6F);
            }
            default -> {} // heavy and fists charge instead (see use/release)
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void hit(ServerPlayer player, InfusionHelper.Source s, LivingEntity e, float dmg) {
        FlameDamage.deal(player, e, s.flame(), dmg, s.eff(), 0);
    }

    private static void bind(ServerPlayer player, InfusionHelper.Source s, LivingEntity e) {
        e.addEffect(new MobEffectInstance(ModRegistry.ROOTED, 40 + 10 * s.eff().ordinal(), 0, false, true, true));
        hit(player, s, e, attack(player) * 0.3F * (float) s.strength());
    }

    /** The player's melee damage with the weapon in hand (includes Dying Will boosts). */
    private static float attack(ServerPlayer player) {
        return (float) Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private static float fistDamage(ServerPlayer player, InfusionHelper.Source s) {
        float base = FlameDamage.scaled(KitConfig.FIST_BASE_DAMAGE.get(), s.eff()) + attack(player) - 1;
        if (s.flame() == Flame.SUN) base *= 1.25F; // Sun is the fist flame
        return base * (float) s.strength();
    }

    private static void shoot(ServerPlayer player, InfusionHelper.Source s, double yawOffset, double speed, double damage, double range) {
        Vec3 dir = player.getLookAngle().yRot((float) Math.toRadians(yawOffset));
        double sp = speed, rg = range;
        float size = 1.0F;
        if (s.flame() == Flame.STORM) { sp *= 1.5; rg *= 1.5; } // Storm's ranged shots go further and faster
        if (s.flame() == Flame.SUN) { sp *= 1.3; size = 0.6F; } // Sun's are small and quick
        FlameShotEntity.shoot(player, s.flame(), dir, sp, FlameDamage.scaled(damage, s.eff()) * (float) s.strength(), rg, s.eff(), 0, size);
        if (s.category() == WeaponCategory.BOW) sound(player, SoundEvents.ARROW_SHOOT, 1.3F);
    }

    /** The closest enemy within range inside a cone of half-angle {@code halfAngle} degrees. */
    static LivingEntity target(ServerPlayer player, double range, double halfAngle) {
        return cone(player, range, halfAngle).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    static List<LivingEntity> cone(ServerPlayer player, double range, double halfAngle) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return Kits.enemiesAround(player, eye, range + 1).stream().filter(e -> {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return to.length() <= range + e.getBbWidth() && to.normalize().dot(look) >= cos && player.hasLineOfSight(e);
        }).toList();
    }

    /** Enemies within {@code width} of the line from the eyes along the look direction. */
    static List<LivingEntity> line(ServerPlayer player, double length, double width) {
        Vec3 a = player.getEyePosition().add(0, -0.3, 0);
        Vec3 look = player.getLookAngle();
        return Kits.enemiesAround(player, a.add(look.scale(length / 2)), length / 2 + width).stream().filter(e -> {
            Vec3 c = e.getBoundingBox().getCenter().subtract(a);
            double t = Math.max(0, Math.min(length, c.dot(look)));
            return c.subtract(look.scale(t)).length() <= width + e.getBbWidth() / 2;
        }).toList();
    }

    static LivingEntity lineTarget(ServerPlayer player, double length) {
        return line(player, length, 1.2).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    private static net.minecraft.world.InteractionHand other(net.minecraft.world.InteractionHand h) {
        return h == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
    }
}
