package com.reborndw.gametest;

import com.reborndw.RebornDW;
import com.reborndw.ability.RingHelper;
import com.reborndw.block.ResonatorBlockEntity;
import com.reborndw.config.ServerConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Form;
import com.reborndw.flame.Rank;
import com.reborndw.item.FlameGemItem;
import com.reborndw.network.ActionPayload;
import com.reborndw.ability.AbilityHandler;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Server-side checks of the flame rules. Run with the gameTestServer Gradle task; not shipped in the jar. */
@GameTestHolder(RebornDW.MODID)
@PrefixGameTestTemplate(false)
public class FlameGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = new TestPlayer(h.getLevel());
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 2))));
        return p;
    }

    private static void check(GameTestHelper h, boolean ok, String msg) {
        if (!ok) h.fail(msg);
    }

    @GameTest(template = "test_floor")
    public static void firstPillNeedsLowHealth(GameTestHelper h) {
        ServerPlayer p = player(h);
        check(h, FlameLogic.tryDyingWillEntry(p, true) != null, "pill at full health should fail");
        check(h, !FlameLogic.get(p).awakened, "should not be awakened yet");
        p.setHealth(2.0F);
        check(h, FlameLogic.tryDyingWillEntry(p, true) == null, "pill at low health should succeed");
        FlameData d = FlameLogic.get(p);
        check(h, d.awakened && d.form == Form.DYING_WILL, "should be awakened and in Dying Will");
        check(h, d.active == FlameLogic.naturalFlame(p), "should awaken the natural flame");
        check(h, Math.abs(d.dwMaxSeconds - ServerConfig.DW_START.get()) < 1e-6, "first Dying Will lasts the start duration");
        check(h, FlameLogic.tryDyingWillEntry(p, true) != null, "second pill must hit the shared cooldown");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void nearDeathAwakens(GameTestHelper h) {
        ServerPlayer p = player(h);
        boolean hurt = p.hurt(h.getLevel().damageSources().generic(), 1000F);
        FlameData d = FlameLogic.get(p);
        check(h, hurt, "lethal hit should land (health " + p.getHealth() + ")");
        check(h, p.isAlive() && p.getHealth() > 0, "player should survive the lethal hit");
        check(h, d.awakened && d.form == Form.DYING_WILL, "near death should awaken into Dying Will");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void dyingWillCurvesAndHyperUnlock(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        double lastMax = 0, lastWeak = Double.MAX_VALUE;
        for (int i = 0; i < 40 && !d.hyperUnlocked; i++) {
            d.form = Form.NONE;
            FlameLogic.enterDyingWill(p);
            check(h, d.dwMaxSeconds >= lastMax, "duration must not shrink");
            check(h, d.weaknessSeconds <= lastWeak, "weakness must not grow");
            lastMax = d.dwMaxSeconds;
            lastWeak = d.weaknessSeconds;
        }
        check(h, d.hyperUnlocked, "reaching the cap must unlock Hyper");
        check(h, d.dwMaxSeconds == ServerConfig.DW_CAP.get(), "duration capped");
        check(h, d.weaknessSeconds >= ServerConfig.WEAKNESS_FLOOR.get(), "weakness floor");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void dyingWillEndsInWeakness(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameLogic.enterDyingWill(p);
        FlameData d = FlameLogic.get(p);
        d.formTicks = 1;
        FlameLogic.tick(p);
        check(h, d.form == Form.NONE && d.weaknessTicks > 0, "Dying Will should end in weakness");
        var mod = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(FlameLogic.WEAKNESS_ID);
        FlameLogic.applyAttributes(p, d);
        mod = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(FlameLogic.WEAKNESS_ID);
        check(h, mod != null && mod.amount() < 0, "weakness should lower attack");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void hyperDrainsAndEndsWithFatigue(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        d.hyperUnlocked = true;
        d.pool = 5;
        FlameLogic.toggleHyper(p);
        check(h, d.form == Form.HYPER, "should be in Hyper");
        double before = d.pool;
        FlameLogic.tick(p);
        check(h, d.pool < before, "Hyper should drain the pool");
        for (int i = 0; i < 2000 && d.form == Form.HYPER; i++) FlameLogic.tick(p);
        check(h, d.form == Form.NONE && d.weaknessTicks > 0, "Hyper should end with fatigue when the pool runs dry");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void masteryOnlyWhileActive(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        d.capacity[Flame.SKY.ordinal()] = 100000;
        d.pool = 100000;
        d.hyperUnlocked = true;
        FlameLogic.toggleHyper(p);
        d.activityTicks = 0;
        for (int i = 0; i < 40; i++) FlameLogic.tick(p);
        check(h, d.masterySeconds == 0, "idle Hyper must not earn mastery");
        d.activityTicks = 200;
        for (int i = 0; i < 40; i++) FlameLogic.tick(p);
        check(h, d.masterySeconds > 1.5, "active Hyper earns mastery");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void ultimateFromHyperWithFullTier(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        d.hyperUnlocked = true;
        d.pool = 50;
        FlameLogic.toggleHyper(p);
        check(h, FlameLogic.tryDyingWillEntry(p, true) != null, "pill in Hyper without the extra tier is blocked");
        d.sharedCooldownTicks = 0;
        d.masterySeconds = FlameLogic.masteryTotal();
        d.ultimateTierSeconds = FlameLogic.masteryTotal();
        check(h, FlameLogic.tryDyingWillEntry(p, true) == null, "pill in Hyper with a full tier enters Ultimate");
        check(h, d.form == Form.ULTIMATE, "should be Ultimate");
        check(h, FlameLogic.spend(p, 1e9, true), "Ultimate makes flame free");
        d.formTicks = 1;
        FlameLogic.tick(p);
        check(h, d.form == Form.NONE && d.ultimateTierSeconds == 0, "Ultimate resets the extra tier");
        check(h, d.weaknessTicks >= ServerConfig.ULTIMATE_WEAKNESS.get() * 20, "Ultimate ends in long weakness");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void spendingGrowsCapacityAndDeathScars(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        double cap = d.capacity(Flame.SKY);
        d.pool = cap;
        check(h, FlameLogic.spend(p, 50, true), "spend");
        check(h, d.capacity(Flame.SKY) > cap, "capacity should grow from spending");
        check(h, !FlameLogic.spend(p, 1e9, true), "cannot overspend");
        double before = d.capacity(Flame.SKY);
        FlameLogic.onDeath(p);
        check(h, d.scar[Flame.SKY.ordinal()] > 0 && d.usableCapacity(Flame.SKY) < before, "death scars capacity");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void flameSwitchEmptiesPool(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameLogic.unlock(p, Flame.RAIN);
        FlameData d = FlameLogic.get(p);
        d.active = Flame.SKY;
        d.pool = 80;
        FlameLogic.switchFlame(p, Flame.RAIN);
        check(h, d.active == Flame.RAIN && d.pool == 0 && d.switchCooldownTicks > 0, "switch empties pool and starts cooldown");
        FlameLogic.switchFlame(p, Flame.SKY);
        check(h, d.active == Flame.RAIN, "cooldown blocks a second switch");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void ringCrafting(GameTestHelper h) {
        var level = h.getLevel();
        CraftingInput in = CraftingInput.of(2, 1, List.of(new ItemStack(ModRegistry.BLANK_RING.get()),
                FlameGemItem.create(Flame.SKY, Rank.GEM_FV[Rank.C.ordinal()])));
        var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level);
        check(h, match.isPresent(), "blank ring + C gem should match");
        ItemStack out = match.get().value().assemble(in, level.registryAccess());
        RingData r = out.get(ModRegistry.RING.get());
        check(h, r != null && r.flame() == Flame.SKY && r.grade() == Rank.C, "ring should be a C-grade Sky ring");
        CraftingInput bad = CraftingInput.of(2, 1, List.of(new ItemStack(ModRegistry.BLANK_RING.get()), FlameGemItem.create(null, 0)));
        check(h, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, bad, level).isEmpty(), "unattuned gem should not make a ring");
        CraftingInput chain = CraftingInput.of(2, 1, List.of(out, new ItemStack(Items.CHAIN)));
        var chained = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, chain, level);
        check(h, chained.isPresent() && chained.get().value().assemble(chain, level.registryAccess()).is(ModRegistry.CHAINED_FLAME_RING.get()), "ring + chain = chained ring");
        h.succeed();
    }

    @GameTest(template = "test_floor")
    public static void flameBurstNeedsRingAndRank(GameTestHelper h) {
        ServerPlayer p = player(h);
        FlameLogic.unlock(p, Flame.SKY);
        FlameData d = FlameLogic.get(p);
        d.capacity[Flame.SKY.ordinal()] = 6000; // C rank
        d.pool = 6000;
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
        float hp = z.getHealth();
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY);
        check(h, z.getHealth() == hp && d.pool == 6000, "no ring: Flame Burst must not fire");
        ItemStack ring = new ItemStack(ModRegistry.FLAME_RING.get());
        ring.set(ModRegistry.RING.get(), new RingData(Flame.SKY, Rank.D));
        p.setItemInHand(InteractionHand.OFF_HAND, ring);
        check(h, RingHelper.effectiveRank(p) == Rank.D, "effective rank is the lower of rank C and grade D");
        AbilityHandler.handle(p, ActionPayload.Action.PRIMARY);
        check(h, z.getHealth() < hp, "Flame Burst should hurt the zombie");
        check(h, d.pool < 6000, "Flame Burst should cost flame");
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY);
        check(h, !d.shieldActive, "Sky Shield needs C grade");
        ring.set(ModRegistry.RING.get(), new RingData(Flame.SKY, Rank.C));
        AbilityHandler.handle(p, ActionPayload.Action.SECONDARY);
        check(h, d.shieldActive && d.shieldHp > 0, "Sky Shield should come up at C");
        float health = p.getHealth();
        p.hurt(h.getLevel().damageSources().generic(), 2F);
        check(h, p.getHealth() == health, "shield should absorb the hit");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "test_floor", timeoutTicks = 100)
    public static void resonatorAttunesAndFills(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModRegistry.BASIC_RESONATOR.get());
        ServerPlayer p = player(h);
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 3))));
        h.getLevel().addFreshEntity(p);
        FlameLogic.unlock(p, Flame.STORM);
        FlameData d = FlameLogic.get(p);
        d.pool = d.usableCapacity(Flame.STORM);
        ResonatorBlockEntity be = (ResonatorBlockEntity) h.getBlockEntity(pos);
        be.insert(p, FlameGemItem.create(null, 0));
        be.toggleFeeding(p);
        h.runAfterDelay(40, () -> {
            GemData g = be.gem().get(ModRegistry.GEM.get());
            check(h, g != null && g.flame().orElse(null) == Flame.STORM, "gem should be attuned to Storm");
            check(h, g.fv() >= 15, "gem should have collected flame, has " + g.fv());
            check(h, d.pool < d.usableCapacity(Flame.STORM), "feeding draws on the pool");
            p.discard();
            h.succeed();
        });
    }

    @SuppressWarnings("unused")
    private static final NonNullList<ItemStack> UNUSED = NonNullList.create();
    @SuppressWarnings("unused")
    private static final BlockHitResult UNUSED2 = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
}
