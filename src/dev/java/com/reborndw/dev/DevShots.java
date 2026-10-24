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
            if (a[0].equals("head") || a[0].equals("shot") || a[0].equals("inv") || a[0].equals("fx")) {
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
        if (a[0].equals("inv")) return;
        boolean isFx = a[0].equals("fx");
        if (!a[0].equals("head") && !a[0].equals("shot") && !isFx) return;
        boolean isShot = a[0].equals("shot");
        int o = isShot ? 2 : 3;
        float yaw = isFx ? 25 : Float.parseFloat(a[o]), elev = isFx ? 12 : Float.parseFloat(a[o + 1]), dist = isFx ? 4.2F : Float.parseFloat(a[o + 2]);
        float headPitch = 0;
        for (String s : a) if (s.startsWith("pitch=")) headPitch = Float.parseFloat(s.substring(6));
        place(dummy, base, 0F, headPitch);
        Vec3 target = isFx ? base.add(0, 1.2, 1.2) : isShot ? base.add(0, 1.6, 1.5) : base.add(0, 1.8, 0.2);
        for (String opt : a) if (opt.startsWith("at=")) {
            String[] v = opt.substring(3).split(",");
            target = base.add(Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]));
        }
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
        if (mc.screen != null) { mc.setScreen(null); mc.options.hideGui = true; }
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
                    case "compat" -> com.reborndw.config.ClientConfig.SHADER_MODE.set(v > 0 ? com.reborndw.config.ClientConfig.ShaderMode.ALWAYS : com.reborndw.config.ClientConfig.ShaderMode.AUTO);
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
                fd.awakened = true;
                ItemStack held = ItemStack.EMPTY;
                for (String o : a) if (o.startsWith("item=")) {
                    held = new ItemStack(Items.DIAMOND_SWORD);
                    if (o.equals("item=ENCH")) held.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    else held.set(ModRegistry.INFUSION.get(), new com.reborndw.infusion.Infusion(Flame.valueOf(o.substring(5)), com.reborndw.flame.Rank.B));
                }
                dummy.setItemSlot(EquipmentSlot.MAINHAND, held);
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
            case "fx" -> {
                ClientFlameCache.put(dummy.getId(), new FlameData());
                Flame f = Flame.valueOf(a[2]);
                int kind = switch (a[1]) { case "HIT" -> 0; case "BOLT" -> 1; case "SLASH" -> 2; case "THRUST" -> 3; case "SLAM" -> 4;
                    case "TETHER" -> 5; case "CHARGE" -> 6; case "SHIELD" -> 7; case "BURST" -> 8; default -> 9; };
                MinecraftServer srv = mc.getSingleplayerServer();
                Vec3 c = base.add(0, 1.2, 1.2);
                srv.execute(() -> {
                    var lvl = srv.overworld();
                    switch (kind) {
                        case 1, 5 -> com.reborndw.fx.FlameFx.send(lvl, kind, f, c.add(-1.4, .2, 0), c.add(1.4, -.2, 0), 1F, 0);
                        case 3 -> com.reborndw.fx.FlameFx.send(lvl, kind, f, c.add(-1.6, 0, -.5), c.add(1.6, 0, .5), 1F, 0);
                        case 2 -> com.reborndw.fx.FlameFx.send(lvl, kind, f, c.add(0, -.2, -1.2), new Vec3(0, 0, 1), 2F, 110 | ((-25 + 180) << 10));
                        case 4 -> com.reborndw.fx.FlameFx.send(lvl, kind, f, base.add(0, 0, 1.2), Vec3.ZERO, 2.5F, 0);
                        case 7 -> com.reborndw.fx.FlameFx.send(lvl, kind, f, c, new Vec3(0, 0, 1), 1.2F, 0);
                        default -> com.reborndw.fx.FlameFx.send(lvl, kind, f, c, Vec3.ZERO, 1.2F, 0);
                    }
                });
                settle = a.length > 3 ? Integer.parseInt(a[3]) : 4;
            }
            case "cmd" -> { server(mc, String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length))); settle = 10; }
            case "inv" -> {
                if (!a[1].equals("real")) {
                    FlameData fd = new FlameData();
                    fd.awakened = true;
                    fd.active = Flame.valueOf(a[1]);
                    fd.form = Form.valueOf(a[2]);
                    ClientFlameCache.put(mc.player.getId(), fd);
                }
                mc.options.hideGui = false;
                server(mc, "gamemode adventure Dev");
                settle = 50;
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            }
            default -> settle = 1;
        }
    }
}
