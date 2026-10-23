package com.reborndw.recipe;

import com.reborndw.flame.Rank;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import com.reborndw.registry.RingData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Temporary stand-in for the Ring Forge until it is built:
 * a blank ring plus a ranked, attuned Flame Gem makes a flame ring of that flame and grade;
 * a flame ring plus a chain makes the chained ring for the necklace slot.
 */
public class RingCraftingRecipe extends CustomRecipe {
    public RingCraftingRecipe(CraftingBookCategory category) { super(category); }

    private ItemStack compute(CraftingInput input) {
        ItemStack blank = ItemStack.EMPTY, gem = ItemStack.EMPTY, ring = ItemStack.EMPTY, chain = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ModRegistry.BLANK_RING.get()) && blank.isEmpty()) blank = s;
            else if (s.is(ModRegistry.FLAME_GEM.get()) && gem.isEmpty()) gem = s;
            else if (s.is(ModRegistry.FLAME_RING.get()) && ring.isEmpty()) ring = s;
            else if (s.is(Items.CHAIN) && chain.isEmpty()) chain = s;
            else return ItemStack.EMPTY;
        }
        if (!blank.isEmpty() && !gem.isEmpty() && ring.isEmpty() && chain.isEmpty()) {
            GemData g = gem.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
            Rank r = g.rank();
            if (g.flame().isEmpty() || r == null) return ItemStack.EMPTY;
            ItemStack out = new ItemStack(ModRegistry.FLAME_RING.get());
            out.set(ModRegistry.RING.get(), new RingData(g.flame().get(), r));
            return out;
        }
        if (!ring.isEmpty() && !chain.isEmpty() && blank.isEmpty() && gem.isEmpty()) {
            RingData data = ring.get(ModRegistry.RING.get());
            if (data == null) return ItemStack.EMPTY;
            ItemStack out = new ItemStack(ModRegistry.CHAINED_FLAME_RING.get());
            out.set(ModRegistry.RING.get(), data);
            return out;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) { return !compute(input).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return compute(input); }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return w * h >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRegistry.RING_CRAFTING.get(); }
}
