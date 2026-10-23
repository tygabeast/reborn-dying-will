package com.reborndw.registry;

import com.reborndw.RebornDW;
import com.reborndw.block.ResonatorBlock;
import com.reborndw.block.ResonatorBlockEntity;
import com.reborndw.effect.HarmonizedEffect;
import com.reborndw.entity.DyingWillBulletEntity;
import com.reborndw.flame.FlameData;
import com.reborndw.item.*;
import com.reborndw.recipe.RingCraftingRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModRegistry {
    public static final String M = RebornDW.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(M);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(M);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, M);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, M);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, M);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(M);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, M);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, M);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, M);

    // attachments
    public static final Supplier<AttachmentType<FlameData>> FLAME_DATA = ATTACHMENTS.register("flame_data",
            () -> AttachmentType.serializable(FlameData::new).copyOnDeath().build());

    // data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GemData>> GEM = COMPONENTS.registerComponentType("gem",
            b -> b.persistent(GemData.CODEC).networkSynchronized(GemData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RingData>> RING = COMPONENTS.registerComponentType("ring",
            b -> b.persistent(RingData.CODEC).networkSynchronized(RingData.STREAM_CODEC));

    // blocks
    public static final DeferredBlock<Block> FLAME_GEM_ORE = BLOCKS.register("flame_gem_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops().strength(3.0F, 3.0F).lightLevel(s -> 3)));
    public static final DeferredBlock<Block> DEEPSLATE_FLAME_GEM_ORE = BLOCKS.register("deepslate_flame_gem_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE).lightLevel(s -> 3)));
    public static final DeferredBlock<ResonatorBlock> BASIC_RESONATOR = BLOCKS.register("basic_resonator",
            () -> new ResonatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(ResonatorBlock.LIT) ? 9 : 0)));

    public static final Supplier<BlockEntityType<ResonatorBlockEntity>> RESONATOR_BE = BLOCK_ENTITIES.register("resonator",
            () -> BlockEntityType.Builder.of(ResonatorBlockEntity::new, BASIC_RESONATOR.get()).build(null));

    // items
    public static final DeferredItem<BlockItem> FLAME_GEM_ORE_ITEM = ITEMS.registerSimpleBlockItem(FLAME_GEM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_FLAME_GEM_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_FLAME_GEM_ORE);
    public static final DeferredItem<BlockItem> BASIC_RESONATOR_ITEM = ITEMS.registerSimpleBlockItem(BASIC_RESONATOR);
    public static final DeferredItem<FlameGemItem> FLAME_GEM = ITEMS.register("flame_gem",
            () -> new FlameGemItem(new Item.Properties().component(GEM.get(), GemData.EMPTY)));
    public static final DeferredItem<DyingWillPillItem> DYING_WILL_PILL = ITEMS.register("dying_will_pill",
            () -> new DyingWillPillItem(new Item.Properties()));
    public static final DeferredItem<Item> DYING_WILL_BULLET = ITEMS.register("dying_will_bullet",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<DyingWillPistolItem> DYING_WILL_PISTOL = ITEMS.register("dying_will_pistol",
            () -> new DyingWillPistolItem(new Item.Properties().stacksTo(1).durability(250)));
    public static final DeferredItem<Item> BLANK_RING = ITEMS.register("blank_ring",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<FlameRingItem> FLAME_RING = ITEMS.register("flame_ring",
            () -> new FlameRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), false));
    public static final DeferredItem<FlameRingItem> CHAINED_FLAME_RING = ITEMS.register("chained_flame_ring",
            () -> new FlameRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), true));

    // entities
    public static final Supplier<EntityType<DyingWillBulletEntity>> DYING_WILL_BULLET_ENTITY = ENTITIES.register("dying_will_bullet",
            () -> EntityType.Builder.<DyingWillBulletEntity>of(DyingWillBulletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build(M + ":dying_will_bullet"));

    // effects
    public static final DeferredHolder<MobEffect, MobEffect> HARMONIZED = EFFECTS.register("harmonized", HarmonizedEffect::create);
    public static final DeferredHolder<MobEffect, MobEffect> DYING_WILL = EFFECTS.register("dying_will", () -> new com.reborndw.effect.FormEffect(0xFF7A1A));
    public static final DeferredHolder<MobEffect, MobEffect> HYPER_DYING_WILL = EFFECTS.register("hyper_dying_will", () -> new com.reborndw.effect.FormEffect(0xFFB030));
    public static final DeferredHolder<MobEffect, MobEffect> ULTIMATE_DYING_WILL = EFFECTS.register("ultimate_dying_will", () -> new com.reborndw.effect.FormEffect(0xFFF0A0));

    // recipes
    public static final Supplier<RecipeSerializer<RingCraftingRecipe>> RING_CRAFTING = RECIPES.register("ring_crafting",
            () -> new SimpleCraftingRecipeSerializer<>(RingCraftingRecipe::new));

    // creative tab
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.reborn_dying_will"))
            .icon(() -> new ItemStack(DYING_WILL_PILL.get()))
            .displayItems((params, out) -> {
                out.accept(FLAME_GEM_ORE_ITEM.get());
                out.accept(DEEPSLATE_FLAME_GEM_ORE_ITEM.get());
                out.accept(FLAME_GEM.get());
                out.accept(BASIC_RESONATOR_ITEM.get());
                out.accept(BLANK_RING.get());
                out.accept(DYING_WILL_PILL.get());
                out.accept(DYING_WILL_BULLET.get());
                out.accept(DYING_WILL_PISTOL.get());
            }).build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        RECIPES.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
