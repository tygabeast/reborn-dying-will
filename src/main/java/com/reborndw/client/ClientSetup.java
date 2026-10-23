package com.reborndw.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.reborndw.RebornDW;
import com.reborndw.registry.ModRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import com.reborndw.registry.GemData;
import com.reborndw.registry.RingData;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RebornDW.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private static final String CATEGORY = "key.categories.reborn_dying_will";
    public static final KeyMapping PRIMARY = key("primary", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SECONDARY = key("secondary", GLFW.GLFW_KEY_X);
    public static final KeyMapping SIGNATURE = key("signature", GLFW.GLFW_KEY_C);
    public static final KeyMapping HYPER = key("hyper", GLFW.GLFW_KEY_H);
    public static final KeyMapping SWITCH_FLAME = key("switch_flame", GLFW.GLFW_KEY_Y);

    private ClientSetup() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.reborn_dying_will." + name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(PRIMARY);
        event.register(SECONDARY);
        event.register(SIGNATURE);
        event.register(HYPER);
        event.register(SWITCH_FLAME);
    }

    @SubscribeEvent
    public static void gui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, RebornDW.id("flame_meter"), new FlameHud());
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.DYING_WILL_BULLET_ENTITY.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.RESONATOR_BE.get(), ResonatorRenderer::new);
    }

    /** Gems and the gems set in rings take their flame's colour. */
    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            if (layer != 0) return -1;
            GemData g = stack.getOrDefault(ModRegistry.GEM.get(), GemData.EMPTY);
            return g.flame().map(f -> 0xFF000000 | f.color).orElse(0xFFD8D8D8);
        }, ModRegistry.FLAME_GEM.get());
        event.register((stack, layer) -> {
            if (layer != 1) return -1;
            RingData r = stack.get(ModRegistry.RING.get());
            return r == null ? 0xFFD8D8D8 : 0xFF000000 | r.flame().color;
        }, ModRegistry.FLAME_RING.get(), ModRegistry.CHAINED_FLAME_RING.get());
    }
}
