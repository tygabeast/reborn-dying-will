package com.reborndw.gametest;

import com.reborndw.RebornDW;
import com.reborndw.ability.AbilityHandler;
import com.reborndw.ability.FlameDamage;
import com.reborndw.ability.LightningKit;
import com.reborndw.ability.MistKit;
import com.reborndw.ability.Tranquility;
import com.reborndw.config.KitConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.network.ActionPayload;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.reborndw.gametest.FlameGameTests.check;

/** Server-side checks of the Sky-set ring kits. */
@GameTestHolder(RebornDW.MODID)
@PrefixGameTestTemplate(false)
public class KitGameTests {
    /** A player with this flame active at the given rank, wearing a ring of that grade, with a full pool. */
    private static ServerPlayer kitPlayer(GameTestHelper h, Flame flame, Rank rank) {
        ServerPlayer p = FlameGameTests.player(h);
        FlameLogic.unlock(p, flame);
        FlameData d = FlameLogic.get(p);
        d.active = flame;
        d.capacity[flame.ordinal()] = Math.max(100, com.reborndw.config.ServerConfig.rankThreshold(rank.ordinal()));
        d.pool = d.usableCapacity(flame);
        ItemStack ring = new ItemStack(ModRegistry.FLAME_RING.get());
        ring.set(ModRegistry.RING.get(), new RingData(flame, rank));
        p.setItemInHand(InteractionHand.OFF_HAND, ring);
        AbilityHandler.tick(p, d); // picks up the kit and the effective rank
        return p;
    }

    private static Zombie zombie(GameTestHelper h, int x, int z) {
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(x, 1, z));
        zombie.setNoAi(true);
        return zombie;
    }

    @GameTest(template = "test_floor")
    public static void kitNeedsRingAndRank(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.STORM, Rank.D);
        FlameData d = FlameLogic.get(p);
        double pool = d.pool;
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true); // Storm Burst needs C
        check(h, d.pool == pool, "a D ring can't use a C ability");
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        check(h, d.pool == pool, "no ring, no Storm Bolt");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void stormIgnoresArmor(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.STORM, Rank.S);
        Zombie a = zombie(h, 1, 3), b = zombie(h, 3, 3);
        for (Zombie z : new Zombie[]{a, b}) {
            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            z.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            z.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        }
        h.runAfterDelay(2, () -> { // armor counts once the zombies have ticked with it on
            float ha = a.getHealth(), hb = b.getHealth();
            FlameDamage.deal(p, a, Flame.STORM, 10, Rank.S, 0);
            FlameDamage.deal(p, b, Flame.SKY, 10, Rank.S, 0);
            check(h, ha - a.getHealth() > hb - b.getHealth() + 0.5, "Storm should hurt an armored target more than Sky does ("
                    + (ha - a.getHealth()) + " vs " + (hb - b.getHealth()) + ")");
            p.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_floor")
    public static void stormBoltFlies(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.STORM, Rank.D);
        Zombie z = zombie(h, 2, 4);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition().add(0, -0.4, 0));
        float hp = z.getHealth();
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        h.runAfterDelay(10, () -> {
            check(h, z.getHealth() < hp, "Storm Bolt should hit the zombie");
            p.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_floor")
    public static void rainStacksAndCalms(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.RAIN, Rank.E);
        Zombie z = zombie(h, 2, 3);
        int cap = Tranquility.cap(Rank.E);
        for (int i = 0; i < cap; i++) {
            z.invulnerableTime = 0;
            FlameDamage.deal(p, z, Flame.RAIN, 0.5F, Rank.E, 0);
        }
        check(h, Tranquility.stacks(z) == cap, "Rain hits should stack Tranquility to the cap (" + Tranquility.stacks(z) + "/" + cap + ")");
        check(h, z.hasEffect(ModRegistry.CALMED), "full stacks calm the target");
        check(h, z.getAttributeValue(Attributes.MOVEMENT_SPEED) < z.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), "Tranquility slows");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void rainParryNegatesMelee(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.RAIN, Rank.C);
        Zombie z = zombie(h, 2, 3);
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        float health = p.getHealth();
        p.hurt(h.getLevel().damageSources().mobAttack(z), 4F);
        check(h, p.getHealth() == health, "Flame Parry should negate the hit");
        check(h, Tranquility.stacks(z) == Tranquility.cap(Rank.C), "the attacker gets full Tranquility");
        p.invulnerableTime = 0;
        p.hurt(h.getLevel().damageSources().mobAttack(z), 4F);
        check(h, p.getHealth() < health, "only one hit is parried");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void rainCalmFieldHolds(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.RAIN, Rank.D);
        FlameData d = FlameLogic.get(p);
        Zombie z = zombie(h, 2, 3);
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        for (int i = 0; i < 40; i++) AbilityHandler.tick(p, d);
        check(h, Tranquility.stacks(z) >= 2, "Calm Field adds a stack per second");
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, false);
        check(h, d.heldAction == null, "letting go of the key ends the field");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void sunHealsAndCleanses(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.SUN, Rank.D);
        FlameData d = FlameLogic.get(p);
        p.setHealth(10);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        for (int i = 0; i < 100; i++) { p.tickCount++; AbilityHandler.tick(p, d); }
        check(h, p.getHealth() >= 13.5F, "Activation Pulse plus regeneration should heal (" + p.getHealth() + ")");
        var poison = p.getEffect(MobEffects.POISON);
        check(h, poison != null && poison.getDuration() <= 400 - 5 * 5, "harmful effects wear off faster (" + (poison == null ? -1 : poison.getDuration()) + ")");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void sunRallyClearsButKeepsFormWeakness(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.SUN, Rank.C);
        FlameData d = FlameLogic.get(p);
        FlameLogic.applyWeakness(p, d, 600, 1);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Rally clears a harmful effect");
        check(h, p.hasEffect(MobEffects.WEAKNESS), "but not the weakness a Dying Will form leaves");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void lightningChainsAndHardens(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.LIGHTNING, Rank.D);
        Zombie a = zombie(h, 1, 3), b = zombie(h, 3, 3);
        float hb = b.getHealth();
        FlameDamage.deal(p, a, Flame.LIGHTNING, 6, Rank.D, 0);
        check(h, b.getHealth() < hb, "Lightning should chain to the second zombie");
        check(h, p.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(LightningKit.HARDENING_ID) != null || runTickFor(p),
                "Hardening adds toughness");
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        check(h, p.hasEffect(ModRegistry.HARDENED), "Harden is applied");
        float health = p.getHealth();
        p.hurt(h.getLevel().damageSources().generic(), 4F);
        check(h, health - p.getHealth() <= 3.01F, "Harden takes a quarter off damage");
        p.discard();
        h.succeed();
    }

    private static boolean runTickFor(ServerPlayer p) {
        FlameData d = FlameLogic.get(p);
        for (int i = 0; i < 10; i++) { p.tickCount++; AbilityHandler.tick(p, d); }
        return p.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(LightningKit.HARDENING_ID) != null;
    }

    @GameTest(template = "test_floor")
    public static void lightningDischargeStuns(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.LIGHTNING, Rank.C);
        Zombie z = zombie(h, 2, 3);
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        check(h, z.hasEffect(ModRegistry.STUNNED), "Static Discharge stuns");
        float health = p.getHealth();
        p.hurt(h.getLevel().damageSources().mobAttack(z), 4F);
        check(h, p.getHealth() == health, "a stunned mob's blows don't land");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor", timeoutTicks = 60)
    public static void cloudEchoes(GameTestHelper h) {
        double old = KitConfig.ECHO_BASE.get();
        KitConfig.ECHO_BASE.set(1.0);
        ServerPlayer p = kitPlayer(h, Flame.CLOUD, Rank.E);
        Zombie z = zombie(h, 2, 3);
        FlameDamage.deal(p, z, Flame.CLOUD, 4, Rank.E, 0);
        float after = z.getHealth();
        KitConfig.ECHO_BASE.set(old);
        h.runAfterDelay(10, () -> {
            check(h, z.getHealth() < after, "a Cloud hit should echo");
            p.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_floor", timeoutTicks = 40)
    public static void cloudSpreadSplitsArrows(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.CLOUD, Rank.D);
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.setOwner(p);
        arrow.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        arrow.setDeltaMovement(0, 0, 0.01);
        arrow.setNoGravity(true);
        h.getLevel().addFreshEntity(arrow);
        h.runAfterDelay(3, () -> {
            int arrows = h.getLevel().getEntitiesOfClass(Arrow.class, arrow.getBoundingBox().inflate(3)).size();
            check(h, arrows == 3, "the next arrow should split into three (" + arrows + ")");
            check(h, FlameLogic.get(p).cloudSpreadTicks == 0, "Cloud Spread is used up");
            p.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_floor")
    public static void mistDecoyDrawsMobs(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.MIST, Rank.D);
        Zombie z = zombie(h, 2, 4);
        z.setTarget(p);
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        var decoy = MistKit.decoyOf(p);
        check(h, decoy != null, "a decoy appears");
        check(h, z.getTarget() == decoy, "the zombie goes for the decoy");
        z.setTarget(p);
        check(h, z.getTarget() == decoy, "and keeps choosing it while it lasts");
        decoy.vanish();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void mistPhantomTerrain(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.MIST, Rank.C);
        p.setXRot(0); // looking straight ahead: a bridge from your feet
        p.setYRot(0);
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        long placed = net.minecraft.core.BlockPos.betweenClosedStream(p.blockPosition().offset(-1, -2, -1), p.blockPosition().offset(1, 0, 18))
                .filter(pos -> h.getLevel().getBlockState(pos).is(ModRegistry.PHANTOM_BLOCK.get())).count();
        check(h, placed > 0 && placed <= KitConfig.TERRAIN_MAX_BLOCKS.get(), "Phantom Terrain should place up to 8 blocks (" + placed + ")");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void mistHidesFromMobs(GameTestHelper h) {
        ServerPlayer p = kitPlayer(h, Flame.MIST, Rank.E);
        double vis = p.getVisibilityPercent(null);
        check(h, vis < 0.9, "Mist should make mobs notice you from closer (" + vis + ")");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor", timeoutTicks = 40)
    public static void flamePillUnlocksAtLowHealth(GameTestHelper h) {
        ServerPlayer p = FlameGameTests.player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        ItemStack pill = new ItemStack(ModRegistry.FLAME_PILLS.get(Flame.STORM).get());
        pill.finishUsingItem(h.getLevel(), p);
        check(h, !d.unlocked.contains(Flame.STORM) && pill.getCount() == 1, "at full health nothing happens and the pill is kept");
        p.setHealth(2);
        pill.finishUsingItem(h.getLevel(), p);
        check(h, d.unlocked.contains(Flame.STORM), "at low health the Storm Flame awakens");
        check(h, pill.isEmpty(), "and the pill is used");
        check(h, d.active == Flame.STORM && d.pool > 0, "the new flame becomes active, with a full pool");
        h.runAfterDelay(3, () -> {
            check(h, d.form == com.reborndw.flame.Form.DYING_WILL && d.active == Flame.STORM, "then Dying Will starts in the new flame");
            p.discard();
            h.succeed();
        });
    }
}
