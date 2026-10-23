package com.reborndw.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reborndw.flame.Rank;
import com.reborndw.registry.GemData;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe whose Flame Gems must have reached a minimum rank (gem stations: Ring Forge E, Infusion Anvil D,
 * Box Workbench C). JSON: a normal shaped recipe plus "min_gem_rank".
 */
public class RankedShapedRecipe implements CraftingRecipe {
    final ShapedRecipe inner;
    final Rank minRank;

    public RankedShapedRecipe(ShapedRecipe inner, Rank minRank) {
        this.inner = inner;
        this.minRank = minRank;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!inner.matches(input, level)) return false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (!s.is(ModRegistry.FLAME_GEM.get())) continue;
            GemData g = s.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
            Rank r = g.rank();
            if (g.flame().isEmpty() || r == null || !r.atLeast(minRank)) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return inner.assemble(input, registries); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return inner.canCraftInDimensions(width, height); }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return inner.getResultItem(registries); }

    @Override
    public NonNullList<Ingredient> getIngredients() { return inner.getIngredients(); }

    @Override
    public CraftingBookCategory category() { return inner.category(); }

    @Override
    public String getGroup() { return inner.getGroup(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRegistry.RANKED_SHAPED.get(); }

    public static class Serializer implements RecipeSerializer<RankedShapedRecipe> {
        public static final MapCodec<RankedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ShapedRecipe.Serializer.CODEC.forGetter(r -> r.inner),
                Rank.CODEC.fieldOf("min_gem_rank").forGetter(r -> r.minRank)
        ).apply(i, RankedShapedRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, RankedShapedRecipe> STREAM_CODEC = StreamCodec.composite(
                ShapedRecipe.Serializer.STREAM_CODEC, r -> r.inner,
                Rank.STREAM_CODEC, r -> r.minRank,
                RankedShapedRecipe::new);

        @Override public MapCodec<RankedShapedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RankedShapedRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
