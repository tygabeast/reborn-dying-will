package com.reborndw.event;

import com.reborndw.RebornDW;
import com.reborndw.ability.CloudKit;
import com.reborndw.ability.MistKit;
import com.reborndw.ability.Tranquility;
import com.reborndw.config.KitConfig;
import com.reborndw.entity.MistDecoyEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Passive and reactive parts of the ring kits that hook into game events. */
@EventBusSubscriber(modid = RebornDW.MODID)
public final class KitEvents {
    private KitEvents() {}

    private static FlameData active(LivingEntity e, Flame flame) {
        if (!(e instanceof ServerPlayer p)) return null;
        FlameData d = FlameLogic.get(p);
        return d.awakened && d.active == flame ? d : null;
    }

    private static boolean isMelee(net.minecraft.world.damagesource.DamageSource src) {
        return src.getEntity() instanceof LivingEntity && src.getDirectEntity() == src.getEntity()
                && !src.is(DamageTypeTags.IS_PROJECTILE) && !src.is(DamageTypeTags.IS_EXPLOSION);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        var src = event.getSource();
        // a stunned creature's blows don't land
        if (isMelee(src) && src.getEntity() instanceof LivingEntity attacker && attacker.hasEffect(ModRegistry.STUNNED)) {
            event.setCanceled(true);
            return;
        }
        // Rain's Flame Parry: negate one melee hit; the attacker gets full Tranquility
        FlameData rain = active(victim, Flame.RAIN);
        if (rain != null && rain.parryTicks > 0 && isMelee(src) && src.getEntity() instanceof LivingEntity attacker) {
            rain.parryTicks = 0;
            event.setCanceled(true);
            Tranquility.fill(attacker, rain.cachedRank);
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.4F);
            if (victim.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SPLASH, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
            return;
        }
        // Lightning's Harden
        if (victim.hasEffect(ModRegistry.HARDENED) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setAmount((float) (event.getAmount() * (1 - KitConfig.HARDEN_REDUCTION.get())));
    }

    /** Lightning: your armor wears more slowly. */
    @SubscribeEvent
    public static void armorHurt(ArmorHurtEvent event) {
        if (active(event.getEntity(), Flame.LIGHTNING) == null) return;
        double keep = 1 - KitConfig.LIGHTNING_DURABILITY_SAVING.get();
        for (EquipmentSlot slot : event.getArmorMap().keySet())
            event.setNewDamage(slot, (float) (event.getNewDamage(slot) * keep));
    }

    /** Lightning: and so does your shield. */
    @SubscribeEvent
    public static void shieldBlock(LivingShieldBlockEvent event) {
        if (active(event.getEntity(), Flame.LIGHTNING) == null || !event.getBlocked()) return;
        event.setShieldDamage((float) (event.shieldDamage() * (1 - KitConfig.LIGHTNING_DURABILITY_SAVING.get())));
    }

    /** Cloud Spread copies the next projectile its owner fires. */
    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (event.getEntity() instanceof Projectile p) CloudKit.onProjectileSpawn(p);
    }

    /** Mobs that turn on a Mist user with a live decoy go for the decoy instead. */
    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer target)) return;
        MistDecoyEntity decoy = MistKit.decoyOf(target);
        if (decoy != null && decoy.distanceToSqr(event.getEntity()) < 32 * 32) event.setNewAboutToBeSetTarget(decoy);
    }

    /** Mist: mobs notice you from closer. */
    @SubscribeEvent
    public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        FlameData mist = active(event.getEntity(), Flame.MIST);
        if (mist == null) return;
        double cut = KitConfig.MIST_VISIBILITY_BASE.get() + KitConfig.MIST_VISIBILITY_PER_RANK.get() * mist.cachedRank.ordinal();
        event.modifyVisibility(Math.max(0.05, 1 - cut));
    }

    /** Tranquility falls off one stack per second once it runs out. */
    @SubscribeEvent
    public static void effectExpired(MobEffectEvent.Expired event) {
        var inst = event.getEffectInstance();
        if (inst != null && inst.getEffect().value() == ModRegistry.TRANQUILITY.get())
            Tranquility.onExpired(event.getEntity(), inst.getAmplifier());
    }
}
