package com.reborndw.dev;

import com.mojang.authlib.GameProfile;
import com.reborndw.client.ClientFlameCache;
import com.reborndw.entity.FlameShotEntity;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import com.reborndw.registry.ModRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only: reads run-dev/devshots.txt and screenshots a dummy player with each flame, then quits. Lines:
 * {@code head <FLAME> <FORM> <yawDeg> <elevDeg> <dist> [helmet] [night]}, {@code shot <FLAME> <yawDeg> <elevDeg> <dist>},
 * {@code config <key> <value>} (flame height/forward), {@code wait <ticks>}.
 */
public final class DevShots {
    private record Step(String[] a) {}

    private static final List<Step> PLAN = new ArrayList<>();
    private static int index = -1, ticks, settle = 30, startDelay = 80;
    private static RemotePlayer dummy;
    private static FlameShotEntity shot;
    private static Vec3 base;
    private static boolean ready;
    private static int waitLog;

    private DevShots() {}

    public static void init() {
        try {
            for (String line : Files.readAllLines(Path.of("devshots.txt"))) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) PLAN.add(new Step(line.split("\\s+")));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        NeoForge.EVENT_BUS.addListener(DevShots::tick);
    }

    private static void server(Minecraft mc, String... cmds) {
        MinecraftServer s = mc.getSingleplayerServer();
        if (s == null) return;
        s.execute(() -> { for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); });
    }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (++waitLog % 100 == 0) com.reborndw.RebornDW.LOGGER.info("DEVSHOTS waiting, screen={}", mc.screen == null ? null : mc.screen.getClass().getName());
            return;
        }
        if (!ready) {
            if (--startDelay > 0) return;
            ready = true;
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(40);
            mc.options.bobView().set(false);
            server(mc, "gamemode spectator Dev", "time set 6000", "gamerule doDaylightCycle false", "weather clear",
                    "gamerule doMobSpawning false", "kill @e[type=!player]");
            base = new Vec3(0.5, mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0), 0.5);
            dummy = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"), "Dummy"));
            dummy.setId(424242);
            mc.level.addEntity(dummy);
            next(mc);
            return;
        }
        if (index >= PLAN.size()) return;
        String[] a = PLAN.get(index).a();
        hold(mc, a);
        if (++ticks >= settle) {
            if (a[0].equals("head") || a[0].equals("shot")) {
                String name = String.join("_", a).replace('.', 'p') + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {});
            }
            next(mc);
        }
    }

    private static void place(Entity en, Vec3 p, float yaw, float pitch) {
        en.setPos(p.x, p.y, p.z);
        en.xo = en.xOld = p.x; en.yo = en.yOld = p.y; en.zo = en.zOld = p.z;
        en.setYRot(yaw); en.yRotO = yaw; en.setXRot(pitch); en.xRotO = pitch;
        en.setYHeadRot(yaw);
        if (en instanceof net.minecraft.world.entity.LivingEntity l) { l.yHeadRotO = yaw; l.yBodyRot = yaw; l.yBodyRotO = yaw; }
        en.setDeltaMovement(en instanceof FlameShotEntity ? en.getDeltaMovement() : Vec3.ZERO);
    }

    private static void hold(Minecraft mc, String[] a) {
        if (!a[0].equals("head") && !a[0].equals("shot")) return;
        boolean isShot = a[0].equals("shot");
        int o = isShot ? 2 : 3;
        float yaw = Float.parseFloat(a[o]), elev = Float.parseFloat(a[o + 1]), dist = Float.parseFloat(a[o + 2]);
        float headPitch = 0;
        for (String s : a) if (s.startsWith("pitch=")) headPitch = Float.parseFloat(s.substring(6));
        place(dummy, base, 0F, headPitch);
        Vec3 target = isShot ? base.add(0, 1.6, 1.5) : base.add(0, 1.8, 0.2);
        if (isShot) shot.setPos(target.x, target.y, target.z);
        double yr = Math.toRadians(yaw), er = Math.toRadians(elev);
        // the dummy faces +z; yaw 0 puts the camera in front of its face
        Vec3 cam = target.add(Math.sin(yr) * Math.cos(er) * dist, Math.sin(er) * dist, Math.cos(yr) * Math.cos(er) * dist);
        Vec3 d = target.subtract(cam);
        float cy = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float cp = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        place(mc.player, cam.subtract(0, mc.player.getEyeHeight(), 0), cy, cp);
        mc.player.getAbilities().flying = true;
    }

    private static void next(Minecraft mc) {
        ticks = 0;
        settle = 30;
        if (shot != null) { shot.discard(); mc.level.removeEntity(shot.getId(), Entity.RemovalReason.DISCARDED); shot = null; }
        index++;
        if (index >= PLAN.size()) { mc.stop(); return; }
        String[] a = PLAN.get(index).a();
        switch (a[0]) {
            case "wait" -> settle = Integer.parseInt(a[1]);
            case "time" -> server(mc, "time set " + a[1]);
            case "config" -> {
                double v = Double.parseDouble(a[2]);
                switch (a[1]) {
                    case "raise" -> com.reborndw.config.ClientConfig.FLAME_RAISE.set(v);
                    case "forward" -> com.reborndw.config.ClientConfig.FLAME_FORWARD.set(v);
                    case "scale" -> com.reborndw.config.ClientConfig.FLAME_SCALE.set(v);
                    default -> {}
                }
                settle = 1;
            }
            case "head" -> {
                FlameData fd = new FlameData();
                fd.active = Flame.valueOf(a[1]);
                fd.form = Form.valueOf(a[2]);
                ClientFlameCache.put(dummy.getId(), fd);
                boolean helmet = List.of(a).contains("helmet");
                dummy.setItemSlot(EquipmentSlot.HEAD, helmet ? new ItemStack(Items.IRON_HELMET) : ItemStack.EMPTY);
                if (List.of(a).contains("night")) server(mc, "time set 18000"); else server(mc, "time set 6000");
            }
            case "shot" -> {
                ClientFlameCache.put(dummy.getId(), new FlameData());
                dummy.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                shot = new FlameShotEntity(ModRegistry.FLAME_SHOT.get(), mc.level);
                shot.setFlame(Flame.valueOf(a[1]));
                shot.setId(424243);
                shot.setDeltaMovement(0.6, 0, 0);
                shot.setPos(base.x, base.y + 1.6, base.z + 1.5);
                mc.level.addEntity(shot);
            }
            default -> settle = 1;
        }
    }
}
