package com.reborndw.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Server-authoritative tuning. Every number here is a placeholder for playtesting. */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue NEAR_DEATH_AWAKENING;
    public static final ModConfigSpec.BooleanValue BULLET_PVP;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_FRACTION;

    public static final ModConfigSpec.IntValue DW_START;
    public static final ModConfigSpec.IntValue DW_GAIN_MIN;
    public static final ModConfigSpec.IntValue DW_GAIN_MAX;
    public static final ModConfigSpec.IntValue DW_CAP;
    public static final ModConfigSpec.IntValue WEAKNESS_START;
    public static final ModConfigSpec.IntValue WEAKNESS_REDUCE_MIN;
    public static final ModConfigSpec.IntValue WEAKNESS_REDUCE_MAX;
    public static final ModConfigSpec.IntValue WEAKNESS_FLOOR;
    public static final ModConfigSpec.IntValue SHARED_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ENTRY_POOL_RECOVERY;
    public static final ModConfigSpec.DoubleValue DW_BOOST;
    public static final ModConfigSpec.DoubleValue RANK_BONUS;
    public static final ModConfigSpec.DoubleValue WEAKNESS_ATTACK;
    public static final ModConfigSpec.DoubleValue WEAKNESS_SPEED;

    public static final ModConfigSpec.DoubleValue POOL_REGEN;
    public static final ModConfigSpec.DoubleValue POOL_REGEN_RESTING;
    public static final ModConfigSpec.IntValue RESTING_DELAY;

    public static final ModConfigSpec.ConfigValue<List<? extends Long>> RANK_THRESHOLDS;
    public static final ModConfigSpec.DoubleValue GROWTH_RATE;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> GROWTH_RANK_FACTORS;
    public static final ModConfigSpec.DoubleValue RESOLVE_MULTIPLIER;
    public static final ModConfigSpec.IntValue SWITCH_COOLDOWN;

    public static final ModConfigSpec.DoubleValue HYPER_DRAIN_START;
    public static final ModConfigSpec.DoubleValue HYPER_DRAIN_MAX;
    public static final ModConfigSpec.IntValue MASTERY_TIERS;
    public static final ModConfigSpec.IntValue SECONDS_PER_TIER;
    public static final ModConfigSpec.DoubleValue HYPER_BOOST;
    public static final ModConfigSpec.DoubleValue HYPER_BOOST_PER_TIER;
    public static final ModConfigSpec.IntValue HYPER_FATIGUE_START;
    public static final ModConfigSpec.IntValue HYPER_FATIGUE_MAX;
    public static final ModConfigSpec.IntValue ACTIVITY_WINDOW;

    public static final ModConfigSpec.DoubleValue ULTIMATE_BOOST;
    public static final ModConfigSpec.IntValue ULTIMATE_DURATION;
    public static final ModConfigSpec.IntValue ULTIMATE_WEAKNESS;

    public static final ModConfigSpec.BooleanValue SCAR_ENABLED;
    public static final ModConfigSpec.IntValue SCAR_PER_DEATH;
    public static final ModConfigSpec.IntValue SCAR_CAP;
    public static final ModConfigSpec.DoubleValue SCAR_RECOVERY_RATIO;

    public static final ModConfigSpec.DoubleValue RESONATOR_AMBIENT;
    public static final ModConfigSpec.DoubleValue RESONATOR_TRANSFER;

    public static final ModConfigSpec.DoubleValue HARMONY_RADIUS;
    public static final ModConfigSpec.DoubleValue HARMONY_SHARE;
    public static final ModConfigSpec.BooleanValue HARMONY_TEAMLESS_ALLIES;
    public static final ModConfigSpec.DoubleValue FLAME_BURST_COST;
    public static final ModConfigSpec.DoubleValue FLAME_BURST_DAMAGE;
    public static final ModConfigSpec.DoubleValue FLAME_BURST_RADIUS;
    public static final ModConfigSpec.DoubleValue SKY_SHIELD_COST;
    public static final ModConfigSpec.DoubleValue SKY_SHIELD_UPKEEP;
    public static final ModConfigSpec.DoubleValue SKY_SHIELD_HEARTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("awakening");
        NEAR_DEATH_AWAKENING = b.comment("Lethal damage to an unawakened player awakens their natural flame instead (once).")
                .define("nearDeathAwakening", true);
        BULLET_PVP = b.comment("Dying Will Bullets can hit other players.").define("bulletPvp", true);
        LOW_HEALTH_FRACTION = b.comment("Health fraction counted as 'low health' for a first pill and for the resolve bonus.")
                .defineInRange("lowHealthFraction", 0.3, 0.05, 1.0);
        b.pop();

        b.push("dyingWill");
        DW_START = b.comment("Seconds the first Dying Will state lasts.").defineInRange("startSeconds", 30, 1, 3600);
        DW_GAIN_MIN = b.defineInRange("gainPerEntryMin", 5, 0, 600);
        DW_GAIN_MAX = b.defineInRange("gainPerEntryMax", 10, 0, 600);
        DW_CAP = b.comment("Duration cap in seconds. Reaching it unlocks Hyper Dying Will.").defineInRange("capSeconds", 180, 1, 36000);
        WEAKNESS_START = b.defineInRange("weaknessStartSeconds", 60, 0, 36000);
        WEAKNESS_REDUCE_MIN = b.defineInRange("weaknessReductionMin", 3, 0, 600);
        WEAKNESS_REDUCE_MAX = b.defineInRange("weaknessReductionMax", 5, 0, 600);
        WEAKNESS_FLOOR = b.defineInRange("weaknessFloorSeconds", 3, 0, 600);
        SHARED_COOLDOWN = b.comment("Seconds between pills or bullets.").defineInRange("sharedCooldownSeconds", 20, 0, 3600);
        ENTRY_POOL_RECOVERY = b.comment("Share of capacity restored when entering.").defineInRange("entryPoolRecovery", 0.10, 0.0, 1.0);
        DW_BOOST = b.defineInRange("boostMultiplier", 1.5, 1.0, 10.0);
        RANK_BONUS = b.comment("Extra boost per rank above E.").defineInRange("rankBonusPerRank", 0.05, 0.0, 1.0);
        WEAKNESS_ATTACK = b.defineInRange("weaknessAttackPenalty", 0.30, 0.0, 1.0);
        WEAKNESS_SPEED = b.defineInRange("weaknessSpeedPenalty", 0.20, 0.0, 1.0);
        b.pop();

        b.push("pool");
        POOL_REGEN = b.comment("Share of capacity regenerated per second.").defineInRange("regenPerSecond", 0.005, 0.0, 1.0);
        POOL_REGEN_RESTING = b.defineInRange("restingRegenPerSecond", 0.015, 0.0, 1.0);
        RESTING_DELAY = b.comment("Seconds standing still before resting regen applies.").defineInRange("restingDelaySeconds", 3, 0, 600);
        b.pop();

        b.push("capacity");
        RANK_THRESHOLDS = b.comment("Minimum capacity in FV for ranks E, D, C, B, A, S. E is also the starting capacity.")
                .defineList("rankThresholds", List.of(100L, 1000L, 5000L, 20000L, 50000L, 100000L), o -> o instanceof Long);
        GROWTH_RATE = b.comment("Capacity gained per FV spent, before the rank factor.").defineInRange("growthRate", 0.04, 0.0, 10.0);
        GROWTH_RANK_FACTORS = b.comment("Growth multiplier at ranks E to S.")
                .defineList("growthRankFactors", List.of(1.0, 0.6, 0.35, 0.2, 0.1, 0.05), o -> o instanceof Double);
        RESOLVE_MULTIPLIER = b.comment("Growth multiplier at low health or in a Dying Will form.").defineInRange("resolveMultiplier", 1.5, 1.0, 10.0);
        SWITCH_COOLDOWN = b.defineInRange("flameSwitchCooldownSeconds", 30, 0, 3600);
        b.pop();

        b.push("hyper");
        HYPER_DRAIN_START = b.comment("Share of capacity drained per second at mastery 0.").defineInRange("drainAtZeroMastery", 0.01, 0.0, 1.0);
        HYPER_DRAIN_MAX = b.defineInRange("drainAtMaxMastery", 0.004, 0.0, 1.0);
        MASTERY_TIERS = b.defineInRange("masteryTiers", 10, 1, 100);
        SECONDS_PER_TIER = b.comment("Active Hyper seconds needed for tier t is this times t.").defineInRange("secondsPerTier", 600, 1, 100000);
        HYPER_BOOST = b.defineInRange("boostMultiplier", 2.0, 1.0, 20.0);
        HYPER_BOOST_PER_TIER = b.defineInRange("boostPerTier", 0.05, 0.0, 5.0);
        HYPER_FATIGUE_START = b.defineInRange("fatigueAtZeroSeconds", 10, 0, 3600);
        HYPER_FATIGUE_MAX = b.defineInRange("fatigueAtMaxSeconds", 2, 0, 3600);
        ACTIVITY_WINDOW = b.comment("Seconds after spending flame or dealing/taking damage that count toward mastery.")
                .defineInRange("activityWindowSeconds", 10, 1, 600);
        b.pop();

        b.push("ultimate");
        ULTIMATE_BOOST = b.defineInRange("boostMultiplier", 5.0, 1.0, 50.0);
        ULTIMATE_DURATION = b.defineInRange("durationSeconds", 180, 1, 36000);
        ULTIMATE_WEAKNESS = b.defineInRange("weaknessSeconds", 3600, 0, 360000);
        b.pop();

        b.push("scar");
        SCAR_ENABLED = b.define("enabled", true);
        SCAR_PER_DEATH = b.comment("Percent of every flame's capacity scarred per death.").defineInRange("percentPerDeath", 3, 0, 9);
        SCAR_CAP = b.defineInRange("capPercent", 15, 0, 100);
        SCAR_RECOVERY_RATIO = b.comment("FV that must be spent to heal 1 FV of scar.").defineInRange("recoveryRatio", 5.0, 0.1, 1000.0);
        b.pop();

        b.push("gems");
        RESONATOR_AMBIENT = b.comment("Ambient FV per second collected by a basic resonator.").defineInRange("basicAmbientPerSecond", 1.0, 0.0, 10000.0);
        RESONATOR_TRANSFER = b.comment("Max FV per second a basic resonator accepts from a player.").defineInRange("basicTransferPerSecond", 10.0, 0.0, 100000.0);
        b.pop();

        b.push("sky");
        HARMONY_RADIUS = b.defineInRange("harmonyRadius", 10.0, 0.0, 64.0);
        HARMONY_SHARE = b.comment("Share of your boost given to allies.").defineInRange("harmonyShare", 0.25, 0.0, 1.0);
        HARMONY_TEAMLESS_ALLIES = b.comment("If you are on no scoreboard team, treat every nearby player without a team as an ally.")
                .define("harmonyTeamlessAllies", true);
        FLAME_BURST_COST = b.defineInRange("flameBurstCost", 0.03, 0.0, 1.0);
        FLAME_BURST_DAMAGE = b.defineInRange("flameBurstDamage", 6.0, 0.0, 1000.0);
        FLAME_BURST_RADIUS = b.defineInRange("flameBurstRadius", 3.0, 0.5, 32.0);
        SKY_SHIELD_COST = b.defineInRange("skyShieldCost", 0.05, 0.0, 1.0);
        SKY_SHIELD_UPKEEP = b.comment("Share of capacity per second while the shield is up.").defineInRange("skyShieldUpkeep", 0.01, 0.0, 1.0);
        SKY_SHIELD_HEARTS = b.comment("Hearts absorbed at C, plus this many per rank above C.").defineInRange("skyShieldHeartsPerRank", 2.0, 0.0, 100.0);
        b.pop();

        SPEC = b.build();
    }

    private ServerConfig() {}

    public static long rankThreshold(int index) {
        List<? extends Long> list = RANK_THRESHOLDS.get();
        long[] defaults = {100L, 1000L, 5000L, 20000L, 50000L, 100000L};
        return index < list.size() ? list.get(index) : defaults[index];
    }

    public static double growthFactor(int index) {
        List<? extends Double> list = GROWTH_RANK_FACTORS.get();
        double[] defaults = {1.0, 0.6, 0.35, 0.2, 0.1, 0.05};
        return index < list.size() ? list.get(index) : defaults[index];
    }
}
