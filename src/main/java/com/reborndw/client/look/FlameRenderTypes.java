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
}
