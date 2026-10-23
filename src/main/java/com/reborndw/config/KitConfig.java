package com.reborndw.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Tuning for the Sky-set ring kits (Storm, Rain, Sun, Lightning, Cloud, Mist). Every number is a placeholder.
 * Costs are shares of the active flame's capacity. "Per rank" means per rank above E unless stated.
 * Saved as serverconfig/reborn_dying_will-kits.toml.
 */
public final class KitConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue RANK_DAMAGE_SCALING;

    public static final ModConfigSpec.DoubleValue STORM_PIERCE_BASE, STORM_PIERCE_PER_RANK;
    public static final ModConfigSpec.DoubleValue STORM_BOLT_COST, STORM_BOLT_DAMAGE, STORM_BOLT_RANGE;
    public static final ModConfigSpec.DoubleValue STORM_BURST_COST, STORM_BURST_RADIUS, STORM_BURST_DAMAGE;

    public static final ModConfigSpec.IntValue TRANQUILITY_BASE_CAP;
    public static final ModConfigSpec.DoubleValue CALM_FIELD_COST, CALM_FIELD_UPKEEP, CALM_FIELD_RADIUS;
    public static final ModConfigSpec.DoubleValue PARRY_COST;
    public static final ModConfigSpec.IntValue PARRY_TICKS;

    public static final ModConfigSpec.IntValue SUN_REGEN_INTERVAL_E, SUN_REGEN_INTERVAL_PER_RANK;
    public static final ModConfigSpec.DoubleValue SUN_CLEANSE_BASE, SUN_CLEANSE_PER_RANK;
    public static final ModConfigSpec.DoubleValue PULSE_COST, PULSE_HEAL, PULSE_HEAL_PER_RANK, PULSE_GROW_RADIUS;
    public static final ModConfigSpec.DoubleValue RALLY_COST, RALLY_RADIUS, RALLY_HEAL, RALLY_HEAL_PER_RANK;

    public static final ModConfigSpec.DoubleValue LIGHTNING_TOUGHNESS_BASE, LIGHTNING_TOUGHNESS_PER_RANK;
    public static final ModConfigSpec.DoubleValue LIGHTNING_KB_BASE, LIGHTNING_KB_PER_RANK, LIGHTNING_DURABILITY_SAVING;
    public static final ModConfigSpec.DoubleValue CHAIN_FRACTION, CHAIN_RANGE;
    public static final ModConfigSpec.DoubleValue HARDEN_COST, HARDEN_REDUCTION;
    public static final ModConfigSpec.IntValue HARDEN_SECONDS;
    public static final ModConfigSpec.DoubleValue DISCHARGE_COST, DISCHARGE_RADIUS, DISCHARGE_DAMAGE;
    public static final ModConfigSpec.IntValue STUN_TICKS;

    public static final ModConfigSpec.DoubleValue ECHO_BASE, ECHO_PER_RANK;
    public static final ModConfigSpec.DoubleValue SPREAD_COST, NEEDLE_COST, NEEDLE_DAMAGE;
    public static final ModConfigSpec.IntValue NEEDLE_COUNT, SPREAD_ARMED_SECONDS;

    public static final ModConfigSpec.DoubleValue MIST_VISIBILITY_BASE, MIST_VISIBILITY_PER_RANK, CONFUSE_BASE, CONFUSE_PER_RANK;
    public static final ModConfigSpec.DoubleValue DECOY_COST, TERRAIN_COST, TERRAIN_COST_PER_BLOCK;
    public static final ModConfigSpec.IntValue DECOY_SECONDS, TERRAIN_SECONDS, TERRAIN_MAX_BLOCKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RANK_DAMAGE_SCALING = b.comment("Ability damage grows by this share per effective rank above E.")
                .defineInRange("rankDamageScaling", 0.25, 0.0, 10.0);

        b.push("storm");
        STORM_PIERCE_BASE = b.comment("Share of the target's armor Storm damage ignores at E.").defineInRange("armorPierceBase", 0.10, 0.0, 1.0);
        STORM_PIERCE_PER_RANK = b.defineInRange("armorPiercePerRank", 0.05, 0.0, 1.0);
        STORM_BOLT_COST = b.defineInRange("stormBoltCost", 0.03, 0.0, 1.0);
        STORM_BOLT_DAMAGE = b.defineInRange("stormBoltDamage", 6.0, 0.0, 1000.0);
        STORM_BOLT_RANGE = b.defineInRange("stormBoltRange", 16.0, 1.0, 128.0);
        STORM_BURST_COST = b.defineInRange("stormBurstCost", 0.05, 0.0, 1.0);
        STORM_BURST_RADIUS = b.defineInRange("stormBurstRadius", 3.0, 0.5, 32.0);
        STORM_BURST_DAMAGE = b.defineInRange("stormBurstDamage", 2.0, 0.0, 1000.0);
        b.pop();

        b.push("rain");
        TRANQUILITY_BASE_CAP = b.comment("Tranquility stack cap at E; +1 per rank of the attacker.").defineInRange("tranquilityBaseCap", 3, 1, 50);
        CALM_FIELD_COST = b.defineInRange("calmFieldCost", 0.05, 0.0, 1.0);
        CALM_FIELD_UPKEEP = b.comment("Per second while held.").defineInRange("calmFieldUpkeep", 0.01, 0.0, 1.0);
        CALM_FIELD_RADIUS = b.defineInRange("calmFieldRadius", 4.0, 0.5, 32.0);
        PARRY_COST = b.defineInRange("flameParryCost", 0.04, 0.0, 1.0);
        PARRY_TICKS = b.comment("Length of the parry window in ticks.").defineInRange("flameParryTicks", 10, 1, 200);
        b.pop();

        b.push("sun");
        SUN_REGEN_INTERVAL_E = b.comment("Ticks between extra half-hearts of regeneration at E.").defineInRange("regenIntervalE", 80, 1, 12000);
        SUN_REGEN_INTERVAL_PER_RANK = b.comment("Ticks taken off that interval per rank.").defineInRange("regenIntervalPerRank", 8, 0, 1000);
        SUN_CLEANSE_BASE = b.comment("How much faster harmful effects wear off at E (0.25 = 25%).").defineInRange("cleanseBase", 0.25, 0.0, 10.0);
        SUN_CLEANSE_PER_RANK = b.defineInRange("cleansePerRank", 0.05, 0.0, 10.0);
        PULSE_COST = b.defineInRange("activationPulseCost", 0.04, 0.0, 1.0);
        PULSE_HEAL = b.comment("Health healed over 5 seconds at D.").defineInRange("activationPulseHeal", 4.0, 0.0, 1000.0);
        PULSE_HEAL_PER_RANK = b.comment("Extra health per rank above D.").defineInRange("activationPulseHealPerRank", 2.0, 0.0, 1000.0);
        PULSE_GROW_RADIUS = b.defineInRange("activationPulseGrowRadius", 3.0, 0.0, 16.0);
        RALLY_COST = b.defineInRange("rallyCost", 0.06, 0.0, 1.0);
        RALLY_RADIUS = b.defineInRange("rallyRadius", 8.0, 0.5, 64.0);
        RALLY_HEAL = b.comment("Health healed at C.").defineInRange("rallyHeal", 6.0, 0.0, 1000.0);
        RALLY_HEAL_PER_RANK = b.comment("Extra health per rank above C.").defineInRange("rallyHealPerRank", 2.0, 0.0, 1000.0);
        b.pop();

        b.push("lightning");
        LIGHTNING_TOUGHNESS_BASE = b.defineInRange("toughnessBase", 1.0, 0.0, 100.0);
        LIGHTNING_TOUGHNESS_PER_RANK = b.defineInRange("toughnessPerRank", 0.5, 0.0, 100.0);
        LIGHTNING_KB_BASE = b.defineInRange("knockbackResistanceBase", 0.10, 0.0, 1.0);
        LIGHTNING_KB_PER_RANK = b.defineInRange("knockbackResistancePerRank", 0.05, 0.0, 1.0);
        LIGHTNING_DURABILITY_SAVING = b.comment("Share of armor and shield durability loss prevented.").defineInRange("durabilitySaving", 0.25, 0.0, 1.0);
        CHAIN_FRACTION = b.comment("Chain damage as a share of the original hit.").defineInRange("chainFraction", 0.40, 0.0, 10.0);
        CHAIN_RANGE = b.defineInRange("chainRange", 4.0, 0.5, 32.0);
        HARDEN_COST = b.defineInRange("hardenCost", 0.04, 0.0, 1.0);
        HARDEN_SECONDS = b.defineInRange("hardenSeconds", 5, 1, 600);
        HARDEN_REDUCTION = b.defineInRange("hardenDamageReduction", 0.25, 0.0, 1.0);
        DISCHARGE_COST = b.defineInRange("staticDischargeCost", 0.05, 0.0, 1.0);
        DISCHARGE_RADIUS = b.defineInRange("staticDischargeRadius", 4.0, 0.5, 32.0);
        DISCHARGE_DAMAGE = b.defineInRange("staticDischargeDamage", 5.0, 0.0, 1000.0);
        STUN_TICKS = b.defineInRange("stunTicks", 20, 1, 1200);
        b.pop();

        b.push("cloud");
        ECHO_BASE = b.comment("Chance a Cloud hit echoes at E.").defineInRange("echoChanceBase", 0.10, 0.0, 1.0);
        ECHO_PER_RANK = b.defineInRange("echoChancePerRank", 0.05, 0.0, 1.0);
        SPREAD_COST = b.defineInRange("cloudSpreadCost", 0.04, 0.0, 1.0);
        SPREAD_ARMED_SECONDS = b.comment("How long Cloud Spread waits for your next projectile.").defineInRange("cloudSpreadArmedSeconds", 30, 1, 3600);
        NEEDLE_COST = b.defineInRange("needleBurstCost", 0.06, 0.0, 1.0);
        NEEDLE_COUNT = b.defineInRange("needleBurstCount", 16, 1, 64);
        NEEDLE_DAMAGE = b.defineInRange("needleBurstDamage", 3.0, 0.0, 1000.0);
        b.pop();

        b.push("mist");
        MIST_VISIBILITY_BASE = b.comment("How much closer mobs must be to notice you at E.").defineInRange("detectionReductionBase", 0.15, 0.0, 1.0);
        MIST_VISIBILITY_PER_RANK = b.defineInRange("detectionReductionPerRank", 0.05, 0.0, 1.0);
        CONFUSE_BASE = b.defineInRange("confuseChanceBase", 0.10, 0.0, 1.0);
        CONFUSE_PER_RANK = b.defineInRange("confuseChancePerRank", 0.05, 0.0, 1.0);
        DECOY_COST = b.defineInRange("decoyCost", 0.05, 0.0, 1.0);
        DECOY_SECONDS = b.defineInRange("decoySeconds", 8, 1, 600);
        TERRAIN_COST = b.defineInRange("phantomTerrainCost", 0.05, 0.0, 1.0);
        TERRAIN_COST_PER_BLOCK = b.defineInRange("phantomTerrainCostPerBlock", 0.005, 0.0, 1.0);
        TERRAIN_SECONDS = b.defineInRange("phantomTerrainSeconds", 10, 1, 600);
        TERRAIN_MAX_BLOCKS = b.defineInRange("phantomTerrainMaxBlocks", 8, 1, 64);
        b.pop();

        SPEC = b.build();
    }

    private KitConfig() {}
}
