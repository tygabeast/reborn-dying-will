package com.reborndw.registry;

import com.reborndw.RebornDW;
import com.reborndw.block.ResonatorBlock;
import com.reborndw.block.ResonatorBlockEntity;
import com.reborndw.effect.HarmonizedEffect;
import com.reborndw.entity.DyingWillBulletEntity;
import com.reborndw.flame.FlameData;
import com.reborndw.item.*;
import com.reborndw.recipe.RankedShapedRecipe;
import com.reborndw.block.PhantomBlock;
import com.reborndw.block.RingForgeBlock;
import com.reborndw.effect.CalmedEffect;
import com.reborndw.effect.SimpleEffect;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.entity.MistDecoyEntity;
import com.reborndw.flame.Flame;
import com.reborndw.menu.RingForgeMenu;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import java.util.EnumMap;
import java.util.Map;
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
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, M);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, M);

    // attachments
    public static final Supplier<AttachmentType<FlameData>> FLAME_DATA = ATTACHMENTS.register("flame_data",
            () -> AttachmentType.serializable(FlameData::new).copyOnDeath().build());

    // data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GemData>> GEM = COMPONENTS.registerComponentType("gem",
            b -> b.persistent(GemData.CODEC).networkSynchronized(GemData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.reborndw.infusion.Infusion>> INFUSION = COMPONENTS.registerComponentType("infusion",
            b -> b.persistent(com.reborndw.infusion.Infusion.CODEC).networkSynchronized(com.reborndw.infusion.Infusion.STREAM_CODEC));
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

    public static final DeferredBlock<RingForgeBlock> RING_FORGE = BLOCKS.register("ring_forge",
            () -> new RingForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.ANVIL)));
    public static final DeferredBlock<com.reborndw.block.InfusionAnvilBlock> INFUSION_ANVIL = BLOCKS.register("infusion_anvil",
            () -> new com.reborndw.block.InfusionAnvilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.ANVIL).noOcclusion()));
    public static final DeferredBlock<PhantomBlock> PHANTOM_BLOCK = BLOCKS.register("phantom_block",
            () -> new PhantomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F, 1.0F).sound(SoundType.AMETHYST).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    public static final Supplier<BlockEntityType<ResonatorBlockEntity>> RESONATOR_BE = BLOCK_ENTITIES.register("resonator",
            () -> BlockEntityType.Builder.of(ResonatorBlockEntity::new, BASIC_RESONATOR.get()).build(null));

    // items
    public static final DeferredItem<BlockItem> FLAME_GEM_ORE_ITEM = ITEMS.registerSimpleBlockItem(FLAME_GEM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_FLAME_GEM_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_FLAME_GEM_ORE);
    public static final DeferredItem<BlockItem> BASIC_RESONATOR_ITEM = ITEMS.registerSimpleBlockItem(BASIC_RESONATOR);
    public static final DeferredItem<BlockItem> RING_FORGE_ITEM = ITEMS.registerSimpleBlockItem(RING_FORGE);
    public static final DeferredItem<BlockItem> INFUSION_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(INFUSION_ANVIL);
    public static final DeferredItem<FlameGemItem> FLAME_GEM = ITEMS.register("flame_gem",
            () -> new FlameGemItem(new Item.Properties().component(GEM.get(), GemData.EMPTY)));
    public static final DeferredItem<DyingWillPillItem> DYING_WILL_PILL = ITEMS.register("dying_will_pill",
            () -> new DyingWillPillItem(new Item.Properties()));
    public static final DeferredItem<Item> DYING_WILL_BULLET = ITEMS.register("dying_will_bullet",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<DyingWillPistolItem> DYING_WILL_PISTOL = ITEMS.register("dying_will_pistol",
            () -> new DyingWillPistolItem(new Item.Properties().stacksTo(1).durability(250)));
    /** Legacy from 0.1: the Ring Forge now makes rings from iron, but old blanks still work there. */
    public static final DeferredItem<Item> BLANK_RING = ITEMS.register("blank_ring",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.reborn_dying_will.blank_ring").withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
    public static final DeferredItem<FlameRingItem> FLAME_RING = ITEMS.register("flame_ring",
            () -> new FlameRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), false));
    public static final DeferredItem<FlameRingItem> CHAINED_FLAME_RING = ITEMS.register("chained_flame_ring",
            () -> new FlameRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), true));

    // one flame pill per Sky-set flame: they awaken another flame at low health
    public static final Map<Flame, DeferredItem<FlamePillItem>> FLAME_PILLS = new EnumMap<>(Flame.class);
    static {
        for (Flame f : Flame.SKY_SET)
            FLAME_PILLS.put(f, ITEMS.register(f.id() + "_flame_pill", () -> new FlamePillItem(new Item.Properties(), f)));
    }

    // entities
    public static final Supplier<EntityType<DyingWillBulletEntity>> DYING_WILL_BULLET_ENTITY = ENTITIES.register("dying_will_bullet",
            () -> EntityType.Builder.<DyingWillBulletEntity>of(DyingWillBulletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build(M + ":dying_will_bullet"));

    public static final Supplier<EntityType<FlameShotEntity>> FLAME_SHOT = ENTITIES.register("flame_shot",
            () -> EntityType.Builder.<FlameShotEntity>of(FlameShotEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(1).noSave().build(M + ":flame_shot"));
    public static final Supplier<EntityType<MistDecoyEntity>> MIST_DECOY = ENTITIES.register("mist_decoy",
            () -> EntityType.Builder.<MistDecoyEntity>of(MistDecoyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).noSave().build(M + ":mist_decoy"));

    // effects
    public static final DeferredHolder<MobEffect, MobEffect> HARMONIZED = EFFECTS.register("harmonized", HarmonizedEffect::create);
    public static final DeferredHolder<MobEffect, MobEffect> DYING_WILL = EFFECTS.register("dying_will", () -> new com.reborndw.effect.FormEffect(0xFF7A1A));
    public static final DeferredHolder<MobEffect, MobEffect> HYPER_DYING_WILL = EFFECTS.register("hyper_dying_will", () -> new com.reborndw.effect.FormEffect(0xFFB030));
    public static final DeferredHolder<MobEffect, MobEffect> TRANQUILITY = EFFECTS.register("tranquility",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, Flame.RAIN.color)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, RebornDW.id("tranquility_speed"), -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, RebornDW.id("tranquility_attack_speed"), -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> CALMED = EFFECTS.register("calmed", () -> new CalmedEffect(0x6FB7FF));
    public static final DeferredHolder<MobEffect, MobEffect> HARDENED = EFFECTS.register("hardened",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, Flame.LIGHTNING.color)
                    .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, RebornDW.id("hardened_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = EFFECTS.register("stunned",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xE8E04A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, RebornDW.id("stunned_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, RebornDW.id("stunned_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> CONFUSED = EFFECTS.register("confused",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, Flame.MIST.color));
    public static final DeferredHolder<MobEffect, MobEffect> ROOTED = EFFECTS.register("rooted",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x5A8A3A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, RebornDW.id("rooted_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, RebornDW.id("rooted_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> DECAY = EFFECTS.register("decay", () -> new com.reborndw.effect.DecayEffect(Flame.STORM.color));
    public static final DeferredHolder<MobEffect, MobEffect> ULTIMATE_DYING_WILL = EFFECTS.register("ultimate_dying_will", () -> new com.reborndw.effect.FormEffect(0xFFF0A0));

    // recipes
    public static final Supplier<RecipeSerializer<RankedShapedRecipe>> RANKED_SHAPED = RECIPES.register("ranked_shaped",
            RankedShapedRecipe.Serializer::new);

    // menus
    public static final Supplier<MenuType<RingForgeMenu>> RING_FORGE_MENU = MENUS.register("ring_forge",
            () -> new MenuType<>(RingForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<com.reborndw.menu.InfusionAnvilMenu>> INFUSION_ANVIL_MENU = MENUS.register("infusion_anvil",
            () -> new MenuType<>(com.reborndw.menu.InfusionAnvilMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // creative tab
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.reborn_dying_will"))
            .icon(() -> new ItemStack(DYING_WILL_PILL.get()))
            .displayItems((params, out) -> {
                out.accept(FLAME_GEM_ORE_ITEM.get());
                out.accept(DEEPSLATE_FLAME_GEM_ORE_ITEM.get());
                out.accept(FLAME_GEM.get());
                out.accept(BASIC_RESONATOR_ITEM.get());
                out.accept(RING_FORGE_ITEM.get());
                out.accept(INFUSION_ANVIL_ITEM.get());
                out.accept(DYING_WILL_PILL.get());
                for (Flame f : Flame.SKY_SET) out.accept(FLAME_PILLS.get(f).get());
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
        MENUS.register(bus);
        TABS.register(bus);
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(MIST_DECOY.get(), MistDecoyEntity.attributes().build());
    }

    private ModRegistry() {}
}
