package com.reborndw.client.look;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Untextured, unlit, two-sided render types for flames: {@link #BODY} blends normally and gives a flame its colour,
 * {@link #GLOW} adds light on top. Neither writes depth, so overlapping layers of one flame never cut each other.
 */
public final class FlameRenderTypes {
    private FlameRenderTypes() {}

    // drawn in the order written (no sorting): each flame paints its layers back to front itself
    private static RenderType make(String name, RenderStateShard.TransparencyStateShard blend) {
        return RenderType.create("reborn_dying_will_" + name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTransparencyState(blend)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                        .createCompositeState(false));
    }

    public static final RenderType BODY = make("flame_body", RenderStateShard.TRANSLUCENT_TRANSPARENCY);
    public static final RenderType GLOW = make("flame_glow", RenderStateShard.LIGHTNING_TRANSPARENCY);

    /** The Earth set's script bands: textured, full-bright, unlit, two-sided, normal blending. */
    public static final RenderType SCRIPT = RenderType.create("reborn_dying_will_script", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS, 4096, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(com.reborndw.RebornDW.id("textures/entity/script_ring.png"), false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false));

    // ---- shader-pack path: vanilla entity render types, which shader packs know how to draw (glow = the "eyes" program)
    private static final net.minecraft.resources.ResourceLocation WHITE = com.reborndw.RebornDW.id("textures/entity/flame_white.png");
    public static final RenderType BODY_COMPAT = RenderType.entityTranslucentEmissive(WHITE);
    public static final RenderType GLOW_COMPAT = RenderType.EYES.apply(WHITE, RenderStateShard.LIGHTNING_TRANSPARENCY);
    public static final RenderType SCRIPT_COMPAT = RenderType.entityTranslucentEmissive(com.reborndw.RebornDW.id("textures/entity/script_ring.png"));

    private static java.lang.invoke.MethodHandle irisInUse, irisPackName;
    private static boolean irisLooked, cached;
    private static long cachedAt;

    /**
     * True when the shader-pack path should be used: always, never, or (auto) while an Iris shader pack is on whose name
     * is not in the exceptions list (packs that look better on the normal path). Re-checked at most once a second.
     */
    public static boolean compat() {
        switch (com.reborndw.config.ClientConfig.SHADER_MODE.get()) {
            case ALWAYS: return true;
            case NEVER: return false;
            default:
        }
        long now = System.currentTimeMillis();
        if (now - cachedAt < 1000L) return cached;
        cachedAt = now;
        cached = autoCompat();
        return cached;
    }

    private static boolean autoCompat() {
        if (!irisLooked) {
            irisLooked = true;
            var lookup = java.lang.invoke.MethodHandles.publicLookup();
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object inst = api.getMethod("getInstance").invoke(null);
                irisInUse = lookup.findVirtual(api, "isShaderPackInUse", java.lang.invoke.MethodType.methodType(boolean.class)).bindTo(inst);
            } catch (Throwable ignored) { irisInUse = null; } // no Iris (or a different API): stay on the normal path
            try { // the pack's name is not in Iris's public API; if this ever moves, exceptions just stop applying
                irisPackName = lookup.findStatic(Class.forName("net.irisshaders.iris.Iris"), "getCurrentPackName",
                        java.lang.invoke.MethodType.methodType(String.class));
            } catch (Throwable ignored) { irisPackName = null; }
        }
        if (irisInUse == null) return false;
        try {
            if (!(boolean) irisInUse.invokeExact()) return false;
            if (irisPackName != null) {
                String name = ((String) irisPackName.invokeExact());
                if (name != null) {
                    name = name.toLowerCase(java.util.Locale.ROOT);
                    for (String ex : com.reborndw.config.ClientConfig.SHADER_EXCEPTIONS.get())
                        if (!ex.isBlank() && name.contains(ex.toLowerCase(java.util.Locale.ROOT))) return false;
                }
            }
            return true;
        } catch (Throwable t) { return false; }
    }
}
