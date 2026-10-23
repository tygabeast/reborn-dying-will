package com.reborndw.gametest;

import com.reborndw.RebornDW;
import com.reborndw.ability.AbilityHandler;
import com.reborndw.ability.Tranquility;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.infusion.Infusion;
import com.reborndw.infusion.InfusionEffects;
import com.reborndw.infusion.InfusionHelper;
import com.reborndw.infusion.WeaponCategory;
import com.reborndw.item.FlameGemItem;
import com.reborndw.menu.InfusionAnvilLogic;
import com.reborndw.network.ActionPayload;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.reborndw.gametest.FlameGameTests.check;

/** Server-side checks of flame infusion: the anvil, categories, infused hits, shots and weapon moves. */
@GameTestHolder(RebornDW.MODID)
@PrefixGameTestTemplate(false)
public class InfusionGameTests {
    private static ItemStack infused(net.minecraft.world.item.Item item, Flame flame, Rank grade) {
        ItemStack s = new ItemStack(item);
        s.set(ModRegistry.INFUSION.get(), new Infusion(flame, grade));
        return s;
    }

    private static ServerPlayer flamePlayer(GameTestHelper h, Flame flame, Rank rank) {
        ServerPlayer p = FlameGameTests.player(h);
        FlameLogic.unlock(p, flame);
        FlameData d = FlameLogic.get(p);
        d.active = flame;
        d.capacity[flame.ordinal()] = Math.max(100, com.reborndw.config.ServerConfig.rankThreshold(rank.ordinal()));
        d.pool = d.usableCapacity(flame);
        return p;
    }

    private static Zombie zombie(GameTestHelper h, int x, int z) {
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(x, 1, z));
        zombie.setNoAi(true);
        return zombie;
    }

    @GameTest(template = "test_floor")
    public static void anvilInfuses(GameTestHelper h) {
        ItemStack gemC = FlameGemItem.create(Flame.STORM, Rank.GEM_FV[Rank.C.ordinal()]);
        ItemStack out = InfusionAnvilLogic.result(new ItemStack(Items.IRON_SWORD), gemC);
        Infusion inf = out.get(ModRegistry.INFUSION.get());
        check(h, inf != null && inf.flame() == Flame.STORM && inf.grade() == Rank.C, "sword + C Storm gem = C Storm infusion");
        check(h, InfusionAnvilLogic.result(new ItemStack(Items.IRON_CHESTPLATE), gemC).isEmpty(), "armor only takes Lightning");
        check(h, !InfusionAnvilLogic.result(new ItemStack(Items.IRON_CHESTPLATE), FlameGemItem.create(Flame.LIGHTNING, 1000)).isEmpty(),
                "Lightning infuses armor");
        check(h, InfusionAnvilLogic.result(new ItemStack(Items.IRON_SWORD), FlameGemItem.create(Flame.STORM, 900)).isEmpty(), "unranked gem does nothing");
        check(h, InfusionAnvilLogic.result(new ItemStack(Items.STICK), gemC).isEmpty(), "a stick is no weapon");
        // the anvil needs a D-rank gem
        var level = h.getLevel();
        ItemStack b = new ItemStack(Items.IRON_BLOCK), i = new ItemStack(Items.IRON_INGOT), e = ItemStack.EMPTY;
        CraftingInput good = CraftingInput.of(3, 3, List.of(b, b, b, e, FlameGemItem.create(Flame.SUN, 10000), e, i, i, i));
        check(h, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, good, level).isPresent(), "a D gem makes the Infusion Anvil");
        CraftingInput bad = CraftingInput.of(3, 3, List.of(b, b, b, e, FlameGemItem.create(Flame.SUN, 9999), e, i, i, i));
        check(h, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, bad, level).isEmpty(), "an E gem doesn't");
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void categories(GameTestHelper h) {
        check(h, WeaponCategory.of(new ItemStack(Items.DIAMOND_SWORD)) == WeaponCategory.BLADE, "sword = blade");
        check(h, WeaponCategory.of(new ItemStack(Items.IRON_AXE)) == WeaponCategory.HEAVY, "axe = heavy");
        check(h, WeaponCategory.of(new ItemStack(Items.MACE)) == WeaponCategory.HEAVY, "mace = heavy");
        check(h, WeaponCategory.of(new ItemStack(Items.TRIDENT)) == WeaponCategory.POLEARM, "trident = polearm");
        check(h, WeaponCategory.of(new ItemStack(Items.CROSSBOW)) == WeaponCategory.BOW, "crossbow = bow");
        check(h, WeaponCategory.of(new ItemStack(Items.SHIELD)) == WeaponCategory.SHIELD, "shield = shield");
        check(h, WeaponCategory.of(ItemStack.EMPTY) == WeaponCategory.FISTS, "empty hand = fists");
        check(h, WeaponCategory.of(new ItemStack(Items.STICK)) == null, "stick = nothing");
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void sourceRules(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.RAIN, Rank.B);
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_SWORD, Flame.STORM, Rank.C));
        check(h, InfusionHelper.source(p) == null, "a Storm sword does nothing for a Rain user");
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_SWORD, Flame.RAIN, Rank.C));
        var s = InfusionHelper.source(p);
        check(h, s != null && s.eff() == Rank.C && !s.toggle(), "a C Rain sword for a B Rain user works at C");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        FlameLogic.get(p).infuseToggle = true;
        check(h, InfusionHelper.source(p) == null, "the toggle needs a ring");
        ItemStack ring = new ItemStack(ModRegistry.FLAME_RING.get());
        ring.set(ModRegistry.RING.get(), new RingData(Flame.RAIN, Rank.D));
        p.setItemInHand(InteractionHand.OFF_HAND, ring);
        s = InfusionHelper.source(p);
        check(h, s != null && s.toggle() && s.category() == WeaponCategory.HEAVY && s.eff() == Rank.D, "with a ring the toggle infuses the axe at the ring's rank");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void infusedHitsCarryTheFlame(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.RAIN, Rank.C);
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_SWORD, Flame.RAIN, Rank.C));
        Zombie a = zombie(h, 1, 3), b = zombie(h, 3, 3);
        float ha = a.getHealth(), hb = b.getHealth();
        a.hurt(h.getLevel().damageSources().playerAttack(p), 4F);
        check(h, Tranquility.stacks(a) == 2, "a Rain blade adds two Tranquility stacks (" + Tranquility.stacks(a) + ")");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        b.hurt(h.getLevel().damageSources().playerAttack(p), 4F);
        check(h, ha - a.getHealth() > hb - b.getHealth(), "infused hits hit harder");
        check(h, Tranquility.stacks(b) == 0, "a plain sword adds nothing");
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_SWORD, Flame.STORM, Rank.C));
        FlameLogic.get(p).active = Flame.STORM;
        FlameLogic.unlock(p, Flame.STORM);
        b.invulnerableTime = 0;
        b.hurt(h.getLevel().damageSources().playerAttack(p), 4F);
        check(h, b.hasEffect(ModRegistry.DECAY), "a Storm blade leaves decay");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void togglePaysPerHit(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.LIGHTNING, Rank.D);
        ItemStack ring = new ItemStack(ModRegistry.FLAME_RING.get());
        ring.set(ModRegistry.RING.get(), new RingData(Flame.LIGHTNING, Rank.D));
        p.setItemInHand(InteractionHand.OFF_HAND, ring);
        AbilityHandler.handle(p, ActionPayload.Action.INFUSE, true);
        FlameData d = FlameLogic.get(p);
        check(h, d.infuseToggle, "Infuse switches on with a ring");
        Zombie z = zombie(h, 2, 3);
        double pool = d.pool;
        z.hurt(h.getLevel().damageSources().playerAttack(p), 2F); // bare fists, infused by the toggle
        check(h, d.pool < pool, "each toggled hit costs a little flame");
        AbilityHandler.handle(p, ActionPayload.Action.INFUSE, true);
        check(h, !d.infuseToggle, "and switches off again");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void weaponMovesComeFirst(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.STORM, Rank.C);
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_SWORD, Flame.STORM, Rank.C));
        Zombie z = zombie(h, 2, 4);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, z.getEyePosition().add(0, -0.5, 0));
        float hp = z.getHealth();
        FlameData d = FlameLogic.get(p);
        double pool = d.pool;
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true); // flame slash, no ring needed
        check(h, z.getHealth() < hp, "Flame Slash should hit the zombie in front");
        check(h, d.pool < pool, "and cost flame");
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        check(h, d.counterTicks > 0, "Counter Stance starts");
        z.invulnerableTime = 0;
        float health = p.getHealth(), zh = z.getHealth();
        p.hurt(h.getLevel().damageSources().mobAttack(z), 4F);
        check(h, p.getHealth() == health && z.getHealth() < zh, "the hit is turned back on the attacker");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void heavyChargesOnHold(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.SUN, Rank.C);
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.IRON_AXE, Flame.SUN, Rank.C));
        Zombie z = zombie(h, 2, 4);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, z.getEyePosition().add(0, -0.5, 0));
        FlameData d = FlameLogic.get(p);
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, true);
        check(h, d.chargeCategory == WeaponCategory.HEAVY, "holding the key charges");
        for (int i = 0; i < 30; i++) AbilityHandler.tick(p, d);
        float hp = z.getHealth();
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY, false);
        check(h, z.getHealth() < hp - 2, "a full charge lands a heavy blow (" + (hp - z.getHealth()) + ")"); // the test player never ticks, so the axe adds no attack here
        check(h, d.chargeCategory == null, "and the charge is spent");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void shieldBraceBlocksFront(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.LIGHTNING, Rank.D);
        p.setItemInHand(InteractionHand.OFF_HAND, infused(Items.SHIELD, Flame.LIGHTNING, Rank.D));
        Zombie z = zombie(h, 2, 4);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY, true);
        check(h, FlameLogic.get(p).braceTicks > 0, "Brace from an infused off-hand shield");
        float health = p.getHealth();
        p.hurt(h.getLevel().damageSources().mobAttack(z), 4F);
        check(h, p.getHealth() == health, "a frontal hit is blocked");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void infusedBowShots(GameTestHelper h) {
        ServerPlayer p = flamePlayer(h, Flame.STORM, Rank.D);
        p.setItemInHand(InteractionHand.MAIN_HAND, infused(Items.BOW, Flame.STORM, Rank.D));
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.setOwner(p);
        arrow.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        arrow.setDeltaMovement(0, 0, 1);
        h.getLevel().addFreshEntity(arrow);
        check(h, arrow.getPersistentData().contains(InfusionEffects.TAG_FLAME), "the arrow carries the flame");
        check(h, arrow.getPierceLevel() == 1, "Storm adds a pierce");
        check(h, arrow.getDeltaMovement().length() > 1.4, "and speed");
        arrow.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void lightningArmor(GameTestHelper h) {
        ItemStack chest = infused(Items.IRON_CHESTPLATE, Flame.LIGHTNING, Rank.C);
        double plain = toughness(new ItemStack(Items.IRON_CHESTPLATE)), boosted = toughness(chest);
        check(h, boosted > plain, "Lightning-infused armor adds toughness (" + plain + " -> " + boosted + ")");
        h.succeed();
    }

    private static double toughness(ItemStack stack) {
        double[] sum = {0};
        stack.getAttributeModifiers().forEach(net.minecraft.world.entity.EquipmentSlot.CHEST, (attr, mod) -> {
            if (attr.is(Attributes.ARMOR_TOUGHNESS)) sum[0] += mod.amount();
        });
        return sum[0];
    }
}
