package com.reborndw.ability;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameLogic;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The innate ring kits by flame, plus small helpers they share. */
public final class Kits {
    private static final Map<Flame, FlameKit> KITS = new EnumMap<>(Flame.class);

    static {
        KITS.put(Flame.SKY, SkyAbilities.KIT);
        KITS.put(Flame.STORM, new StormKit());
        KITS.put(Flame.RAIN, new RainKit());
        KITS.put(Flame.SUN, new SunKit());
        KITS.put(Flame.LIGHTNING, new LightningKit());
        KITS.put(Flame.CLOUD, new CloudKit());
        KITS.put(Flame.MIST, new MistKit());
    }

    private Kits() {}

    public static FlameKit get(Flame flame) {
        return flame == null ? null : KITS.get(flame);
    }

    /** Spends a share of the active flame's capacity, telling the player if the pool is too low. */
    public static boolean spend(ServerPlayer player, double fraction) {
        if (FlameLogic.spendFraction(player, fraction)) return true;
        player.displayClientMessage(Component.translatable("message.reborn_dying_will.empty_pool"), true);
        return false;
    }

    /** Upkeep costs double while Rain's calm is on you. */
    public static double upkeepFactor(LivingEntity e) {
        return e.hasEffect(ModRegistry.CALMED) ? 2.0 : 1.0;
    }

    public static List<LivingEntity> enemiesAround(ServerPlayer player, Vec3 center, double radius) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= radius * radius
                        && !SkyAbilities.isAlly(player, e));
    }

    public static List<LivingEntity> alliesAround(ServerPlayer player, double radius) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius && SkyAbilities.isAlly(player, e));
    }

    /** Particles on a horizontal circle. */
    public static void circle(ServerLevel level, Vec3 c, double r, ParticleOptions p, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(p, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
        }
    }
}
