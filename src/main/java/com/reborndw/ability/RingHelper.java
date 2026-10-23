package com.reborndw.ability;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/** Finds the active ring: off-hand, a Curios ring slot or the necklace slot. The highest grade wins. */
public final class RingHelper {
    private RingHelper() {}

    public static RingData bestRing(Player player, Flame flame) {
        RingData best = consider(null, player.getOffhandItem(), flame);
        var inv = CuriosApi.getCuriosInventory(player);
        if (inv.isPresent()) {
            for (SlotResult r : inv.get().findCurios(s -> s.has(ModRegistry.RING.get()))) {
                best = consider(best, r.stack(), flame);
            }
        }
        return best;
    }

    private static RingData consider(RingData best, ItemStack stack, Flame flame) {
        RingData d = stack.get(ModRegistry.RING.get());
        if (d == null || d.flame() != flame) return best;
        if (best == null || d.grade().ordinal() > best.grade().ordinal()) return d;
        return best;
    }

    /** The lower of the player's rank and their ring's grade, or E with no ring (the innate kit). */
    public static Rank effectiveRank(Player player) {
        FlameData d = FlameLogic.get(player);
        if (d.active == null) return Rank.E;
        RingData ring = bestRing(player, d.active);
        if (ring == null) return Rank.E;
        return Rank.min(FlameLogic.rank(d, d.active), ring.grade());
    }

    public static boolean hasRing(Player player) {
        FlameData d = FlameLogic.get(player);
        return d.active != null && bestRing(player, d.active) != null;
    }
}
