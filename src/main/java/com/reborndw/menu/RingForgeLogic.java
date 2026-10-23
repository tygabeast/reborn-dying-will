package com.reborndw.menu;

import com.reborndw.flame.Rank;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the Ring Forge makes from its three inputs (metal, gem, chain):
 * - 4 iron ingots (or a legacy Blank Ring) + a ranked, attuned gem = a Flame Ring of that flame and grade;
 *   add a chain for a Chained Flame Ring (necklace slot).
 * - a Flame Ring in the metal slot + a chain = the same ring on a chain.
 */
public final class RingForgeLogic {
    public static final int IRON_PER_RING = 4;

    private RingForgeLogic() {}

    /** How many of each input one craft uses: {metal, gem, chain}. */
    public record Result(ItemStack output, int metal, int gem, int chain) {
        static final Result NONE = new Result(ItemStack.EMPTY, 0, 0, 0);
    }

    public static boolean isIron(ItemStack s) {
        return s.is(Items.IRON_INGOT) || s.is(net.neoforged.neoforge.common.Tags.Items.INGOTS_IRON);
    }

    public static Result result(ItemStack metal, ItemStack gem, ItemStack chain) {
        boolean hasChain = chain.is(Items.CHAIN);
        if (!chain.isEmpty() && !hasChain) return Result.NONE;

        // re-chain a finished ring
        if (metal.is(ModRegistry.FLAME_RING.get()) && gem.isEmpty() && hasChain) {
            RingData data = metal.get(ModRegistry.RING.get());
            if (data == null) return Result.NONE;
            ItemStack out = new ItemStack(ModRegistry.CHAINED_FLAME_RING.get());
            out.set(ModRegistry.RING.get(), data);
            return new Result(out, 1, 0, 1);
        }

        // forge a new ring around a gem
        if (!gem.is(ModRegistry.FLAME_GEM.get())) return Result.NONE;
        GemData g = gem.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
        Rank rank = g.rank();
        if (g.flame().isEmpty() || rank == null) return Result.NONE;
        int metalUse;
        if (isIron(metal) && metal.getCount() >= IRON_PER_RING) metalUse = IRON_PER_RING;
        else if (metal.is(ModRegistry.BLANK_RING.get())) metalUse = 1;
        else return Result.NONE;
        ItemStack out = new ItemStack(hasChain ? ModRegistry.CHAINED_FLAME_RING.get() : ModRegistry.FLAME_RING.get());
        out.set(ModRegistry.RING.get(), new RingData(g.flame().get(), rank));
        return new Result(out, metalUse, 1, hasChain ? 1 : 0);
    }
}
