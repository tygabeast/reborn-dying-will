package com.reborndw.infusion;

import com.reborndw.RebornDW;
import com.reborndw.ability.FlameDamage;
import com.reborndw.ability.Kits;
import com.reborndw.config.KitConfig;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import com.reborndw.util.DelayedTasks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** What an infused weapon does on its normal hits and shots, and what infused shields and armor do. */
@EventBusSubscriber(modid = RebornDW.MODID)
public final class InfusionEffects {
    public static final String TAG_FLAME = RebornDW.MODID + ".flame", TAG_RANK = RebornDW.MODID + ".rank", TAG_TWIN = RebornDW.MODID + ".twin";

    private InfusionEffects() {}

    private static boolean isMelee(net.minecraft.world.damagesource.DamageSource src) {
        return src.getEntity() instanceof ServerPlayer && src.getDirectEntity() == src.getEntity()
                && !src.is(DamageTypeTags.IS_PROJECTILE) && !src.is(DamageTypeTags.IS_EXPLOSION);
    }

    /** The infused melee weapon behind this hit, if any (not for flame ability damage, which has its own property). */
    private static InfusionHelper.Source meleeSource(net.minecraft.world.damagesource.DamageSource src) {
        if (FlameDamage.current() != null || !isMelee(src)) return null;
        InfusionHelper.Source s = InfusionHelper.source((ServerPlayer) src.getEntity(), InteractionHand.MAIN_HAND);
        return s != null && s.category().melee() ? s : null;
    }

    /** Normal hits with an infused weapon hit harder. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void bonusDamage(LivingIncomingDamageEvent event) {
        InfusionHelper.Source s = meleeSource(event.getSource());
        if (s == null) return;
        double bonus = s.toggle() ? KitConfig.TOGGLE_BONUS.get()
                : KitConfig.ANVIL_BONUS_BASE.get() + KitConfig.ANVIL_BONUS_PER_RANK.get() * s.eff().ordinal();
        if (s.flame() == Flame.SUN && s.category() == WeaponCategory.FISTS) bonus += 0.25; // Sun's fists hit hardest
        event.setAmount((float) (event.getAmount() * (1 + bonus)));
    }

    /** ...and carry their flame's property. */
    @SubscribeEvent
    public static void meleeHit(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        var src = event.getSource();
        InfusionHelper.Source s = meleeSource(src);
        if (s != null) {
            ServerPlayer player = (ServerPlayer) src.getEntity();
            if (s.toggle() && !payToggle(player)) return;
            applyMelee(player, target, s, event.getNewDamage());
            return;
        }
        // shots from infused bows and guns
        Entity direct = src.getDirectEntity();
        if (direct instanceof Projectile p && !(p instanceof FlameShotEntity) && src.getEntity() instanceof ServerPlayer player
                && p.getPersistentData().contains(TAG_FLAME)) {
            CompoundTag tag = p.getPersistentData();
            Flame flame = Flame.values()[Math.floorMod(tag.getInt(TAG_FLAME), Flame.values().length)];
            Rank eff = Rank.values()[Math.floorMod(tag.getInt(TAG_RANK), Rank.values().length)];
            FlameDamage.onHit(player, target, flame, event.getNewDamage(), eff, 0, 1, 1.0);
            if (flame == Flame.SKY) skyHit(player, target);
        }
    }

    private static boolean payToggle(ServerPlayer player) {
        if (FlameLogic.spendFraction(player, KitConfig.TOGGLE_HIT_COST.get())) return true;
        FlameData d = FlameLogic.get(player);
        d.infuseToggle = false;
        d.markDirty();
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.infuse_off_empty"), true);
        return false;
    }

    static void applyMelee(ServerPlayer player, LivingEntity target, InfusionHelper.Source s, float damage) {
        Rank eff = s.eff();
        switch (s.flame()) {
            case STORM -> target.addEffect(new MobEffectInstance(ModRegistry.DECAY, 60, 0, false, true, true));
            case RAIN -> FlameDamage.onHit(player, target, Flame.RAIN, damage, eff, 0, s.category() == WeaponCategory.BLADE ? 2 : 1, 1.0);
            case SUN -> {
                double share = s.category() == WeaponCategory.FISTS ? KitConfig.SUN_FIST_HEAL.get() : KitConfig.SUN_MELEE_HEAL.get();
                player.heal((float) (damage * share));
                if (s.category() == WeaponCategory.FISTS) knock(player, target, 0.6);
            }
            case LIGHTNING -> FlameDamage.onHit(player, target, Flame.LIGHTNING, damage, eff, 0, 1, 1.0);
            case CLOUD -> FlameDamage.onHit(player, target, Flame.CLOUD, damage, eff, 0, 1, s.category() == WeaponCategory.SHORT ? 2.0 : 1.0);
            case MIST -> FlameDamage.onHit(player, target, Flame.MIST, damage, eff, 0, 1, s.category() == WeaponCategory.POLEARM ? 2.0 : 1.0);
            case SKY -> skyHit(player, target);
            default -> {}
        }
    }

    /** Sky: extra knockback, and a little flame back to nearby allies. */
    private static void skyHit(ServerPlayer player, LivingEntity target) {
        knock(player, target, 0.5);
        for (LivingEntity e : Kits.alliesAround(player, 6)) {
            if (!(e instanceof ServerPlayer ally)) continue;
            FlameData ad = FlameLogic.get(ally);
            if (!ad.awakened || ad.active == null) continue;
            double cap = ad.usableCapacity(ad.active);
            ad.pool = Math.min(cap, ad.pool + cap * KitConfig.SKY_ALLY_REGEN.get());
            ad.markDirty();
        }
    }

    static void knock(Player player, LivingEntity target, double strength) {
        Vec3 push = target.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(strength);
        target.push(push.x, 0.1, push.z);
        target.hurtMarked = true;
    }

    /** Projectiles fired from an infused bow, crossbow or gun carry the flame. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void projectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Projectile p) || p instanceof FlameShotEntity) return;
        if (!(p.getOwner() instanceof ServerPlayer player) || p.getTags().contains(TAG_TWIN)) return;
        if (p.getPersistentData().contains(TAG_FLAME)) return; // a copy that already carries it
        InfusionHelper.Source s = null;
        for (InteractionHand hand : InteractionHand.values()) {
            InfusionHelper.Source h = InfusionHelper.source(player, hand);
            if (h != null && h.category().ranged()) { s = h; break; }
        }
        if (s == null) return;
        if (s.toggle() && !payToggle(player)) return;
        p.getPersistentData().putInt(TAG_FLAME, s.flame().ordinal());
        p.getPersistentData().putInt(TAG_RANK, s.eff().ordinal());
        Vec3 v = p.getDeltaMovement();
        switch (s.flame()) {
            case STORM -> { // more range and speed, and one more target pierced
                p.setDeltaMovement(v.scale(1.5));
                if (p instanceof AbstractArrow a) a.setPierceLevel((byte) (a.getPierceLevel() + 1));
            }
            case SUN -> { // smaller, faster, with an irregular kick that's hard to read
                var r = player.getRandom();
                p.setDeltaMovement(v.scale(1.3).add((r.nextDouble() - 0.5) * 0.15, (r.nextDouble() - 0.5) * 0.1, (r.nextDouble() - 0.5) * 0.15));
            }
            case CLOUD -> splitLater(p, v.length());
            case MIST -> spawnTwin(p, player);
            default -> {}
        }
    }

    /** Cloud: the shot splits in two at 60% damage each after about 10 blocks. */
    private static void splitLater(Projectile p, double speed) {
        int ticks = (int) Math.max(2, Math.ceil(10 / Math.max(0.1, speed)));
        DelayedTasks.schedule(ticks, () -> {
            if (p.isRemoved() || !(p.level() instanceof ServerLevel level)) return;
            if (p instanceof AbstractArrow a && a.getDeltaMovement().lengthSqr() < 0.01) return; // already stuck
            Entity copy = p.getType().create(level);
            if (copy == null) return;
            CompoundTag tag = p.saveWithoutId(new CompoundTag());
            tag.remove("UUID");
            copy.load(tag);
            copy.setUUID(java.util.UUID.randomUUID());
            copy.addTag(FlameDamage.TAG_NO_SPLIT);
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.yRot((float) Math.toRadians(-6)));
            copy.setDeltaMovement(v.yRot((float) Math.toRadians(6)));
            if (p instanceof AbstractArrow a) a.setBaseDamage(a.getBaseDamage() * 0.6);
            if (copy instanceof AbstractArrow a) { a.setBaseDamage(a.getBaseDamage() * 0.6); a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY; }
            if (copy instanceof Projectile cp && p.getOwner() != null) cp.setOwner(p.getOwner());
            level.addFreshEntity(copy);
        });
    }

    /** Mist: an illusory twin flies beside the real shot. It does no damage, but it draws the eye. */
    private static void spawnTwin(Projectile p, ServerPlayer player) {
        DelayedTasks.schedule(0, () -> {
            if (!(p.level() instanceof ServerLevel level) || p.isRemoved()) return;
            Entity twin = p.getType().create(level);
            if (twin == null) return;
            CompoundTag tag = p.saveWithoutId(new CompoundTag());
            tag.remove("UUID");
            twin.load(tag);
            twin.setUUID(java.util.UUID.randomUUID());
            twin.getPersistentData().remove(TAG_FLAME);
            twin.addTag(TAG_TWIN);
            twin.addTag(FlameDamage.TAG_NO_SPLIT);
            twin.setDeltaMovement(p.getDeltaMovement().yRot((float) Math.toRadians(5)));
            if (twin instanceof AbstractArrow a) { a.setBaseDamage(0); a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY; }
            if (twin instanceof Projectile tp) tp.setOwner(player);
            level.addFreshEntity(twin);
        });
    }

    /** Lightning shields send blocked projectiles back. */
    @SubscribeEvent
    public static void shieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        InfusionHelper.Source s = shieldSource(player);
        if (s == null || s.flame() != Flame.LIGHTNING) return;
        if (event.getDamageSource().getDirectEntity() instanceof Projectile proj) reflect(player, proj);
    }

    static InfusionHelper.Source shieldSource(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            InfusionHelper.Source s = InfusionHelper.source(player, hand);
            if (s != null && s.category() == WeaponCategory.SHIELD) return s;
        }
        return null;
    }

    /** Sends a projectile back the way it came, now owned by the defender. */
    public static void reflect(ServerPlayer defender, Projectile proj) {
        DelayedTasks.schedule(0, () -> {
            if (proj.isRemoved()) return;
            Entity shooter = proj.getOwner();
            Vec3 dir = shooter != null ? shooter.getEyePosition().subtract(proj.position()).normalize() : proj.getDeltaMovement().scale(-1).normalize();
            double speed = Math.max(1.0, proj.getDeltaMovement().length());
            proj.setOwner(defender);
            proj.setDeltaMovement(dir.scale(speed * 1.2));
            proj.hurtMarked = true;
        });
    }

    /** Lightning-infused armor adds a little toughness. */
    @SubscribeEvent
    public static void armorAttributes(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        Infusion inf = stack.get(ModRegistry.INFUSION.get());
        if (inf == null || inf.flame() != Flame.LIGHTNING || !(stack.getItem() instanceof ArmorItem armor)) return;
        double base = 0.5, perRank = 0.1;
        try { // server config; not loaded yet when tooltips are built on the title screen
            base = KitConfig.LIGHTNING_ARMOR_TOUGHNESS.get();
            perRank = KitConfig.LIGHTNING_ARMOR_TOUGHNESS_PER_RANK.get();
        } catch (IllegalStateException ignored) {}
        double toughness = base + perRank * inf.grade().ordinal();
        ResourceLocation id = RebornDW.id("lightning_infusion_" + armor.getType().getName());
        event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
    }
}
