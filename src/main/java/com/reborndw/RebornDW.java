package com.reborndw;

import com.reborndw.config.ServerConfig;
import com.reborndw.network.ModNetwork;
import com.reborndw.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RebornDW.MODID)
public class RebornDW {
    public static final String MODID = "reborn_dying_will";

    public RebornDW(IEventBus bus, ModContainer container) {
        ModRegistry.register(bus);
        bus.addListener(ModNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
