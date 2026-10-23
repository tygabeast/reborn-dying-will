package com.reborndw.client;

import com.reborndw.RebornDW;
import com.reborndw.ability.SkyAbilities;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import com.reborndw.network.ActionPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RebornDW.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        send(ClientSetup.PRIMARY, ActionPayload.Action.PRIMARY);
        send(ClientSetup.SECONDARY, ActionPayload.Action.SECONDARY);
        send(ClientSetup.SIGNATURE, ActionPayload.Action.SIGNATURE);
        send(ClientSetup.HYPER, ActionPayload.Action.HYPER);
        send(ClientSetup.SWITCH_FLAME, ActionPayload.Action.SWITCH_FLAME);

        // the forehead flame, seen by everyone, in the active flame's colour
        if (mc.isPaused()) return;
        for (Player p : mc.level.players()) {
            FlameData d = ClientFlameCache.get(p.getId());
            if (d == null || d.active == null || d.form == Form.NONE || p.isInvisible()) continue;
            if (p == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) continue;
            var c = SkyAbilities.rgb(d.active.color);
            float size = d.form == Form.ULTIMATE ? 1.6F : d.form == Form.HYPER ? 1.2F : 0.9F;
            double y = p.getEyeY() + 0.35;
            double fx = -Math.sin(Math.toRadians(p.getYHeadRot())) * 0.2, fz = Math.cos(Math.toRadians(p.getYHeadRot())) * 0.2;
            for (int i = 0; i < 2; i++) {
                mc.level.addParticle(new DustParticleOptions(c, size),
                        p.getX() + fx + (p.getRandom().nextDouble() - 0.5) * 0.15, y + p.getRandom().nextDouble() * 0.2,
                        p.getZ() + fz + (p.getRandom().nextDouble() - 0.5) * 0.15, 0, 0.04, 0);
            }
            if (d.form != Form.DYING_WILL && p.tickCount % 3 == 0)
                mc.level.addParticle(ParticleTypes.SMALL_FLAME, p.getX() + fx, y + 0.1, p.getZ() + fz, 0, 0.02, 0);
        }
    }

    private static void send(KeyMapping key, ActionPayload.Action action) {
        while (key.consumeClick()) PacketDistributor.sendToServer(new ActionPayload(action));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFlameCache.clear();
    }
}
