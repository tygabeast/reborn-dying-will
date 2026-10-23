package com.reborndw.infusion;

import com.reborndw.ability.RingHelper;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Works out whether a held weapon is carrying the player's flame right now, and how strongly.
 * An anvil infusion counts while its flame is the player's active flame (rank: the lower of the player's rank and the
 * weapon's grade). The Infuse toggle lends the active flame to any weapon in hand while a ring is worn (rank: the
 * ring's effective rank), at reduced strength and a small cost per hit.
 */
public final class InfusionHelper {
    public static final double TOGGLE_STRENGTH = 0.7;

    private InfusionHelper() {}

    public record Source(Flame flame, Rank eff, WeaponCategory category, boolean toggle, InteractionHand hand, ItemStack stack) {
        /** Damage factor for moves and bonus damage: the toggle is weaker than a real infusion. */
        public double strength() { return toggle ? TOGGLE_STRENGTH : 1.0; }
    }

    /** The main hand's weapon; with nothing infused there (or bare fists), an infused off-hand shield. */
    @Nullable
    public static Source source(Player player) {
        Source main = source(player, InteractionHand.MAIN_HAND);
        if (main != null && main.category() != WeaponCategory.FISTS) return main;
        Source off = source(player, InteractionHand.OFF_HAND);
        if (off != null && off.category() == WeaponCategory.SHIELD) return off;
        return main;
    }

    @Nullable
    public static Source source(Player player, InteractionHand hand) {
        FlameData d = FlameLogic.get(player);
        if (!d.awakened || d.active == null) return null;
        ItemStack stack = player.getItemInHand(hand);
        WeaponCategory cat = WeaponCategory.of(stack);
        if (cat == null) return null;
        if (hand == InteractionHand.OFF_HAND && cat == WeaponCategory.FISTS) return null;
        Infusion inf = stack.get(ModRegistry.INFUSION.get());
        if (inf != null && inf.flame() == d.active)
            return new Source(inf.flame(), Rank.min(FlameLogic.rank(d, d.active), inf.grade()), cat, false, hand, stack);
        if (d.infuseToggle && hand == InteractionHand.MAIN_HAND && RingHelper.hasRing(player))
            return new Source(d.active, RingHelper.effectiveRank(player), cat, true, hand, stack);
        return null;
    }

    /** Client-side version for the ability panel, using what the sync packet told us. */
    @Nullable
    public static Source clientSource(Player player, FlameData d) {
        if (!d.awakened || d.active == null) return null;
        ItemStack stack = player.getMainHandItem();
        WeaponCategory cat = WeaponCategory.of(stack);
        if (cat == null) return null;
        Infusion inf = stack.get(ModRegistry.INFUSION.get());
        if (inf != null && inf.flame() == d.active)
            return new Source(inf.flame(), Rank.min(d.syncedRank, inf.grade()), cat, false, InteractionHand.MAIN_HAND, stack);
        if (d.infuseToggle && d.syncedHasRing)
            return new Source(d.active, d.syncedEffRank, cat, true, InteractionHand.MAIN_HAND, stack);
        return null;
    }
}
