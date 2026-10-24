package com.reborndw.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.reborndw.RebornDW;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.infusion.Infusion;
import com.reborndw.infusion.WeaponCategory;
import com.reborndw.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Infused items shimmer in their flame's colour while the infusion is working for whoever holds them (the flame is
 * their active flame, or the Infuse toggle is lending it), and not at all while it isn't, so the glint itself says
 * whether the weapon is live. Hooked into the item renderer by {@code ItemRendererMixin}. Render thread only.
 */
public final class InfusionGlint {
    private static final RenderType[] TYPES = new RenderType[Flame.values().length];
    private static ItemStack current;
    private static LivingEntity holder;
    private static int depth;

    private InfusionGlint() {}

    /**
     * Glints must draw after the item under them (they test for equal depth), so like vanilla's they get fixed buffers,
     * which are drawn after the item sheets.
     */
    public static void registerBuffers(net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent event) {
        for (Flame f : Flame.values()) event.registerRenderBuffer(type(f));
    }

    private static RenderType type(Flame f) {
        RenderType t = TYPES[f.ordinal()];
        if (t == null) {
            t = RenderType.create("reborn_dying_will_glint_" + f.id(), DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536,
                    RenderType.CompositeState.builder()
                            .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(RebornDW.id("textures/misc/glint_" + f.id() + ".png"), true, false))
                            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                            .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                            .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                            .createCompositeState(false));
            TYPES[f.ordinal()] = t;
        }
        return t;
    }

    public static void begin(ItemStack stack) { if (depth++ == 0) current = stack; }

    public static void end() { if (--depth <= 0) { depth = 0; current = null; } }

    public static void holder(@Nullable LivingEntity e) { holder = e; }

    /** The flame an item is carrying right now for this holder (or the local player), or null. */
    @Nullable
    public static Flame live(ItemStack stack, @Nullable LivingEntity who) {
        Player p = who instanceof Player pl ? pl : Minecraft.getInstance().player;
        if (p == null) return null;
        FlameData d = ClientFlameCache.get(p.getId());
        if (d == null || !d.awakened || d.active == null) return null;
        Infusion inf = stack.get(ModRegistry.INFUSION.get());
        if (inf != null && inf.flame() == d.active) return inf.flame();
        if (d.infuseToggle && d.syncedHasRing && stack == p.getMainHandItem() && WeaponCategory.of(stack) != null) return d.active;
        return null;
    }

    /**
     * The buffer to draw an item into, or null to leave it to vanilla. A live infusion gets its flame's glint (in place of
     * the enchantment glint); an infused item that isn't live gets no glint unless it is also enchanted.
     */
    @Nullable
    public static VertexConsumer buffer(MultiBufferSource src, RenderType type) {
        ItemStack stack = current;
        if (stack == null || stack.isEmpty()) return null;
        boolean infused = stack.has(ModRegistry.INFUSION.get());
        Flame f = live(stack, holder);
        if (f != null) return VertexMultiConsumer.create(src.getBuffer(type(f)), src.getBuffer(type));
        if (infused && !stack.isEnchanted()) return src.getBuffer(type);
        return null;
    }
}
