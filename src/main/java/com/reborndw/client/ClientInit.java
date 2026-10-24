package com.reborndw.client;

import com.reborndw.config.ClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup that needs the mod container: the client config and its in-game config screen. */
public final class ClientInit {
    private ClientInit() {}

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // dev-only screenshot harness (src/dev, never in the released jar)
        if (System.getProperty("rdw.devshots") != null) {
            try { Class.forName("com.reborndw.dev.DevShots").getMethod("init").invoke(null); }
            catch (ReflectiveOperationException e) { com.reborndw.RebornDW.LOGGER.warn("dev shots unavailable", e); }
        }
    }
}
