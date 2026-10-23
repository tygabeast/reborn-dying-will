package com.reborndw.ability;

import com.reborndw.flame.Flame;
import com.reborndw.flame.Rank;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** What each ring kit offers, for display (the ability panel). The kits themselves hold the behaviour. */
public final class KitInfo {
    public enum Slot { PASSIVE, PRIMARY, SECONDARY }

    /** {@code style}: "" for a tap, "hold" or "toggle". */
    public record Entry(Slot slot, String id, Rank minRank, String style) {
        public String nameKey() { return "ability.reborn_dying_will." + id; }
    }

    private static final Map<Flame, List<Entry>> KITS = new EnumMap<>(Flame.class);

    static {
        kit(Flame.SKY, "harmony", "flame_burst", "", "sky_shield", "toggle");
        kit(Flame.STORM, "disintegration", "storm_bolt", "", "storm_burst", "");
        kit(Flame.RAIN, "tranquility", "calm_field", "hold", "flame_parry", "");
        kit(Flame.SUN, "activation", "activation_pulse", "", "rally", "");
        kit(Flame.LIGHTNING, "hardening", "harden", "", "static_discharge", "");
        kit(Flame.CLOUD, "propagation", "cloud_spread", "", "needle_burst", "");
        kit(Flame.MIST, "construction", "decoy", "", "phantom_terrain", "");
    }

    private KitInfo() {}

    private static void kit(Flame f, String passive, String primary, String primaryStyle, String secondary, String secondaryStyle) {
        KITS.put(f, List.of(new Entry(Slot.PASSIVE, passive, Rank.E, ""),
                new Entry(Slot.PRIMARY, primary, Rank.D, primaryStyle),
                new Entry(Slot.SECONDARY, secondary, Rank.C, secondaryStyle)));
    }

    /** The kit for a flame, or an empty list if it has none yet. */
    public static List<Entry> of(Flame flame) {
        return flame == null ? List.of() : KITS.getOrDefault(flame, List.of());
    }
}
