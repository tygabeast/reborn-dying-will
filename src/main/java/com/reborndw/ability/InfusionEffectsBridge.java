package com.reborndw.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Small shared helpers between infusion hits and weapon moves. */
final class InfusionEffectsBridge {
    private InfusionEffectsBridge() {}

    static void knock(Player player, LivingEntity target, double strength) {
        Vec3 push = target.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(strength);
        target.push(push.x, 0.15, push.z);
        target.hurtMarked = true;
    }
}
