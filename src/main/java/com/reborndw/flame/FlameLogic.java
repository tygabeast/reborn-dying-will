package com.reborndw.flame;

import com.reborndw.RebornDW;
import com.reborndw.config.ServerConfig;
import com.reborndw.network.FlameSyncPayload;
import com.reborndw.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** All server-side rules of the flame system: ranks, spending, growth, the form ladder and ticking. */
public final class FlameLogic {
    public static final ResourceLocation BOOST_ID = RebornDW.id("form_boost");
    public static final ResourceLocation BOOST_KB_ID = RebornDW.id("form_boost_knockback");
    public static final ResourceLocation WEAKNESS_ID = RebornDW.id("weakness");

    private FlameLogic() {}

    public static FlameData get(Player player) {
        return player.getData(ModRegistry.FLAME_DATA.get());
    }

    // ---------------------------------------------------------------- ranks and capacity

    public static Rank rankForCapacity(double capacity) {
        Rank r = Rank.E;
        for (Rank rank : Rank.values()) if (capacity >= ServerConfig.rankThreshold(rank.ordinal())) r = rank;
        return r;
    }

    public static Rank rank(FlameData d, Flame f) {
        return rankForCapacity(d.capacity(f));
    }

    /** Usable capacity of the active flame (capacity minus scar). */
    public static double activeCapacity(FlameData d) {
        return d.active == null ? 0 : d.usableCapacity(d.active);
    }

    public static Flame naturalFlame(Player player) {
        long bits = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
        return Flame.SKY_SET[(int) Math.floorMod(bits * 0x9E3779B97F4A7C15L >>> 17, Flame.SKY_SET.length)];
    }

    public static Flame naturalEarthFlame(Player player) {
        long bits = player.getUUID().getLeastSignificantBits() * 31 + player.getUUID().getMostSignificantBits();
        return Flame.EARTH_SET[(int) Math.floorMod(bits * 0xC2B2AE3D27D4EB4FL >>> 19, Flame.EARTH_SET.length)];
    }

    // ---------------------------------------------------------------- unlocking

    public static void unlock(ServerPlayer player, Flame flame) {
        FlameData d = get(player);
        d.awakened = true;
        d.unlocked.add(flame);
        double min = ServerConfig.rankThreshold(0);
        if (d.capacity[flame.ordinal()] < min) d.capacity[flame.ordinal()] = min;
        if (d.active == null) {
            d.active = flame;
            d.pool = d.usableCapacity(flame);
        }
        d.markDirty();
    }

    public static void awakenNatural(ServerPlayer player) {
        FlameData d = get(player);
        if (d.awakened) return;
        Flame f = naturalFlame(player);
        unlock(player, f);
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.awakened",
                Component.translatable(f.translationKey()).withColor(f.color)), false);
    }

    // ---------------------------------------------------------------- spending and growth

    public static boolean canAfford(Player player, double amount) {
        FlameData d = get(player);
        return d.active != null && (d.form == Form.ULTIMATE || d.pool >= amount);
    }

    /** Spends flame from the pool. Returns false (spending nothing) if the pool is too small. */
    public static boolean spend(ServerPlayer player, double amount, boolean countsForGrowth) {
        FlameData d = get(player);
        if (d.active == null) return false;
        if (d.form == Form.ULTIMATE) {
            d.activityTicks = ServerConfig.ACTIVITY_WINDOW.get() * 20;
            return true;
        }
        if (d.pool < amount) return false;
        d.pool -= amount;
        d.activityTicks = ServerConfig.ACTIVITY_WINDOW.get() * 20;
        if (countsForGrowth) grow(player, d, d.active, amount);
        d.markDirty();
        return true;
    }

    public static boolean spendFraction(ServerPlayer player, double fraction) {
        return spend(player, activeCapacity(get(player)) * fraction, true);
    }

    private static void grow(ServerPlayer player, FlameData d, Flame flame, double spent) {
        int i = flame.ordinal();
        // scar heals first
        if (d.scar[i] > 0) {
            double heal = spent / ServerConfig.SCAR_RECOVERY_RATIO.get();
            d.scar[i] = Math.max(0, d.scar[i] - heal);
        }
        Rank before = rank(d, flame);
        double gain = spent * ServerConfig.GROWTH_RATE.get() * ServerConfig.growthFactor(before.ordinal());
        boolean lowHealth = player.getHealth() <= player.getMaxHealth() * ServerConfig.LOW_HEALTH_FRACTION.get();
        if (lowHealth || d.form.boosted()) gain *= ServerConfig.RESOLVE_MULTIPLIER.get();
        d.capacity[i] += gain;
        Rank after = rank(d, flame);
        if (after != before) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.rank_up",
                    Component.translatable(flame.translationKey()).withColor(flame.color),
                    Component.literal(after.name()).withStyle(ChatFormatting.GOLD)), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    // ---------------------------------------------------------------- the form ladder

    public static int masteryTier(FlameData d) {
        int tiers = ServerConfig.MASTERY_TIERS.get();
        double need = 0;
        for (int t = 1; t <= tiers; t++) {
            need += ServerConfig.SECONDS_PER_TIER.get() * (double) t;
            if (d.masterySeconds < need) return t - 1;
        }
        return tiers;
    }

    public static double masteryTotal() {
        int tiers = ServerConfig.MASTERY_TIERS.get();
        return ServerConfig.SECONDS_PER_TIER.get() * (double) tiers * (tiers + 1) / 2.0;
    }

    public static double masteryFraction(FlameData d) {
        return Mth.clamp(d.masterySeconds / masteryTotal(), 0, 1);
    }

    public static boolean ultimateReady(FlameData d) {
        return d.masterySeconds >= masteryTotal() && d.ultimateTierSeconds >= masteryTotal();
    }

    public static double boostMultiplier(FlameData d) {
        int rankIndex = d.active == null ? 0 : rank(d, d.active).ordinal();
        double rankBonus = ServerConfig.RANK_BONUS.get() * rankIndex;
        return switch (d.form) {
            case NONE -> 1.0;
            case DYING_WILL -> ServerConfig.DW_BOOST.get() + rankBonus;
            case HYPER -> ServerConfig.HYPER_BOOST.get() + ServerConfig.HYPER_BOOST_PER_TIER.get() * masteryTier(d) + rankBonus;
            case ULTIMATE -> ServerConfig.ULTIMATE_BOOST.get();
        };
    }

    /** Pill or bullet. Returns a message key explaining why nothing happened, or null on success. */
    public static String tryDyingWillEntry(ServerPlayer player, boolean fromPill) {
        FlameData d = get(player);
        if (d.sharedCooldownTicks > 0) return "message.reborn_dying_will.cooldown";
        boolean low = player.getHealth() <= player.getMaxHealth() * ServerConfig.LOW_HEALTH_FRACTION.get();
        if (!d.awakened) {
            // a first pill only works when close to death; a bullet awakens anyone, as in canon
            if (fromPill && !low) return "message.reborn_dying_will.not_desperate";
            awakenNatural(player);
            enterDyingWill(player);
            d.sharedCooldownTicks = ServerConfig.SHARED_COOLDOWN.get() * 20;
            return null;
        }
        switch (d.form) {
            case HYPER -> {
                if (!ultimateReady(d)) return fromPill ? "message.reborn_dying_will.ultimate_blocked" : "message.reborn_dying_will.no_effect";
                enterUltimate(player);
            }
            case DYING_WILL, ULTIMATE -> { return "message.reborn_dying_will.already"; }
            case NONE -> enterDyingWill(player);
        }
        d.sharedCooldownTicks = ServerConfig.SHARED_COOLDOWN.get() * 20;
        return null;
    }

    public static void enterDyingWill(ServerPlayer player) {
        FlameData d = get(player);
        if (d.active == null) return;
        var rand = player.getRandom();
        int cap = ServerConfig.DW_CAP.get();
        if (d.dwEntries == 0 || d.dwMaxSeconds <= 0) {
            d.dwMaxSeconds = Math.min(cap, ServerConfig.DW_START.get());
            d.weaknessSeconds = ServerConfig.WEAKNESS_START.get();
        } else {
            int gmin = ServerConfig.DW_GAIN_MIN.get(), gmax = Math.max(gmin, ServerConfig.DW_GAIN_MAX.get());
            d.dwMaxSeconds = Math.min(cap, d.dwMaxSeconds + gmin + rand.nextInt(gmax - gmin + 1));
            int rmin = ServerConfig.WEAKNESS_REDUCE_MIN.get(), rmax = Math.max(rmin, ServerConfig.WEAKNESS_REDUCE_MAX.get());
            d.weaknessSeconds = Math.max(ServerConfig.WEAKNESS_FLOOR.get(), d.weaknessSeconds - (rmin + rand.nextInt(rmax - rmin + 1)));
        }
        d.dwEntries++;
        d.form = Form.DYING_WILL;
        d.formTicks = (int) Math.round(d.dwMaxSeconds * 20);
        d.pool = Math.min(activeCapacity(d), d.pool + activeCapacity(d) * ServerConfig.ENTRY_POOL_RECOVERY.get());
        d.activityTicks = ServerConfig.ACTIVITY_WINDOW.get() * 20;
        if (!d.hyperUnlocked && d.dwMaxSeconds >= cap) {
            d.hyperUnlocked = true;
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.hyper_unlocked").withStyle(ChatFormatting.GOLD), false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
        d.markDirty();
        syncNow(player);
    }

    public static void toggleHyper(ServerPlayer player) {
        FlameData d = get(player);
        if (d.active == null) return;
        if (d.form == Form.HYPER) {
            endHyper(player, d);
            return;
        }
        if (!d.hyperUnlocked) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.hyper_locked"), true);
            return;
        }
        if (d.form != Form.NONE) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.already"), true);
            return;
        }
        if (d.pool <= 0) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.empty_pool"), true);
            return;
        }
        d.form = Form.HYPER;
        d.formTicks = 0;
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
        d.markDirty();
        syncNow(player);
    }

    private static void endHyper(ServerPlayer player, FlameData d) {
        double m = masteryFraction(d);
        double fatigue = Mth.lerp(m, ServerConfig.HYPER_FATIGUE_START.get(), ServerConfig.HYPER_FATIGUE_MAX.get());
        d.form = Form.NONE;
        d.weaknessTicks += (int) Math.round(fatigue * 20);
        d.markDirty();
        syncNow(player);
    }

    private static void enterUltimate(ServerPlayer player) {
        FlameData d = get(player);
        d.form = Form.ULTIMATE;
        d.formTicks = ServerConfig.ULTIMATE_DURATION.get() * 20;
        d.pool = activeCapacity(d);
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.ultimate").withStyle(ChatFormatting.GOLD), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.6F);
        d.markDirty();
        syncNow(player);
    }

    public static void switchFlame(ServerPlayer player, Flame target) {
        FlameData d = get(player);
        if (target == null || !d.unlocked.contains(target) || target == d.active) return;
        if (d.form != Form.NONE) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.switch_in_form"), true);
            return;
        }
        if (d.switchCooldownTicks > 0) {
            player.displayClientMessage(Component.translatable("message.reborn_dying_will.switch_cooldown", d.switchCooldownTicks / 20 + 1), true);
            return;
        }
        d.active = target;
        d.pool = 0;
        d.shieldActive = false;
        d.switchCooldownTicks = ServerConfig.SWITCH_COOLDOWN.get() * 20;
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.switched",
                Component.translatable(target.translationKey()).withColor(target.color)), true);
        d.markDirty();
        syncNow(player);
    }

    public static void cycleFlame(ServerPlayer player) {
        FlameData d = get(player);
        if (d.unlocked.size() < 2 || d.active == null) return;
        Flame[] all = Flame.values();
        for (int step = 1; step < all.length; step++) {
            Flame f = all[(d.active.ordinal() + step) % all.length];
            if (d.unlocked.contains(f)) { switchFlame(player, f); return; }
        }
    }

    // ---------------------------------------------------------------- death

    public static void onDeath(ServerPlayer player) {
        FlameData d = get(player);
        if (ServerConfig.SCAR_ENABLED.get() && d.awakened) {
            double per = ServerConfig.SCAR_PER_DEATH.get() / 100.0;
            double cap = ServerConfig.SCAR_CAP.get() / 100.0;
            for (Flame f : d.unlocked) {
                int i = f.ordinal();
                d.scar[i] = Math.min(d.capacity[i] * cap, d.scar[i] + d.capacity[i] * per);
            }
        }
        d.form = Form.NONE;
        d.formTicks = 0;
        d.weaknessTicks = 0;
        d.shieldActive = false;
        d.pool = 0;
        d.markDirty();
    }

    // ---------------------------------------------------------------- ticking

    public static void tick(ServerPlayer player) {
        FlameData d = get(player);
        if (d.sharedCooldownTicks > 0) d.sharedCooldownTicks--;
        if (d.switchCooldownTicks > 0) d.switchCooldownTicks--;
        if (d.activityTicks > 0) d.activityTicks--;
        if (!d.awakened || d.active == null) {
            applyAttributes(player, d);
            maybeSync(player, d);
            return;
        }
        double cap = activeCapacity(d);

        switch (d.form) {
            case DYING_WILL -> {
                if (--d.formTicks <= 0) {
                    d.form = Form.NONE;
                    d.weaknessTicks += (int) Math.round(d.weaknessSeconds * 20);
                    d.markDirty();
                    syncNow(player);
                }
            }
            case HYPER -> {
                double drain = Mth.lerp(masteryFraction(d), ServerConfig.HYPER_DRAIN_START.get(), ServerConfig.HYPER_DRAIN_MAX.get());
                d.pool -= cap * drain / 20.0;
                if (d.activityTicks > 0) {
                    double total = masteryTotal();
                    if (d.masterySeconds < total) {
                        int before = masteryTier(d);
                        d.masterySeconds = Math.min(total, d.masterySeconds + 0.05);
                        int after = masteryTier(d);
                        if (after != before) player.displayClientMessage(
                                Component.translatable("message.reborn_dying_will.mastery_tier", after).withStyle(ChatFormatting.GOLD), false);
                    } else if (d.ultimateTierSeconds < total) {
                        d.ultimateTierSeconds = Math.min(total, d.ultimateTierSeconds + 0.05);
                        if (d.ultimateTierSeconds >= total) player.displayClientMessage(
                                Component.translatable("message.reborn_dying_will.ultimate_ready").withStyle(ChatFormatting.GOLD), false);
                    }
                }
                if (d.pool <= 0) {
                    d.pool = 0;
                    endHyper(player, d);
                }
                if (player.tickCount % 20 == 0) d.markDirty();
            }
            case ULTIMATE -> {
                d.pool = cap;
                if (--d.formTicks <= 0) {
                    d.form = Form.NONE;
                    d.ultimateTierSeconds = 0;
                    d.weaknessTicks += ServerConfig.ULTIMATE_WEAKNESS.get() * 20;
                    d.markDirty();
                    syncNow(player);
                }
            }
            case NONE -> {
                if (d.weaknessTicks > 0 && --d.weaknessTicks == 0) d.markDirty();
            }
        }

        // pool regeneration (not while Hyper drains it)
        boolean still = Math.abs(player.getX() - d.lastX) < 0.01 && Math.abs(player.getY() - d.lastY) < 0.01
                && Math.abs(player.getZ() - d.lastZ) < 0.01;
        d.lastX = player.getX(); d.lastY = player.getY(); d.lastZ = player.getZ();
        d.restingTicks = still ? d.restingTicks + 1 : 0;
        if (d.form != Form.HYPER && d.pool < cap) {
            double rate = d.restingTicks >= ServerConfig.RESTING_DELAY.get() * 20
                    ? ServerConfig.POOL_REGEN_RESTING.get() : ServerConfig.POOL_REGEN.get();
            d.pool = Math.min(cap, d.pool + cap * rate / 20.0);
            if (player.tickCount % 10 == 0) d.markDirty();
        }
        if (d.pool > cap) d.pool = cap;

        if (player.tickCount % 5 == 0) applyAttributes(player, d);
        maybeSync(player, d);
    }

    public static void applyAttributes(ServerPlayer player, FlameData d) {
        double mult = d.awakened ? boostMultiplier(d) : 1.0;
        boolean weak = d.form == Form.NONE && d.weaknessTicks > 0;
        setModifier(player, Attributes.ATTACK_DAMAGE, BOOST_ID, mult - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.MOVEMENT_SPEED, BOOST_ID, (mult - 1) * 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.BLOCK_BREAK_SPEED, BOOST_ID, mult - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BOOST_KB_ID, Math.min(0.8, (mult - 1) * 0.25), AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ATTACK_DAMAGE, WEAKNESS_ID, weak ? -ServerConfig.WEAKNESS_ATTACK.get() : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.MOVEMENT_SPEED, WEAKNESS_ID, weak ? -ServerConfig.WEAKNESS_SPEED.get() : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void setModifier(Player player, Holder<Attribute> attr, ResourceLocation id, double value, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        if (Math.abs(value) < 1e-6) {
            if (inst.hasModifier(id)) inst.removeModifier(id);
            return;
        }
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - value) < 1e-6 && existing.operation() == op) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, value, op));
    }

    // ---------------------------------------------------------------- sync

    private static void maybeSync(ServerPlayer player, FlameData d) {
        if (d.syncCooldown > 0) d.syncCooldown--;
        if (d.dirty && d.syncCooldown <= 0) syncNow(player);
    }

    public static void syncNow(ServerPlayer player) {
        FlameData d = get(player);
        d.dirty = false;
        d.syncCooldown = 4;
        FlameSyncPayload payload = FlameSyncPayload.of(player, d);
        PacketDistributor.sendToPlayersTrackingEntity(player, payload);
        if (!player.isFakePlayer() && player.connection != null && player.connection.hasChannel(FlameSyncPayload.TYPE))
            PacketDistributor.sendToPlayer(player, payload);
    }
}
