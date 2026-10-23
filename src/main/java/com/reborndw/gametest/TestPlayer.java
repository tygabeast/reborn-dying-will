package com.reborndw.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

/** A packet-free stand-in player for game tests that can be hurt and fires the death event. */
class TestPlayer extends FakePlayer {
    TestPlayer(ServerLevel level) {
        super(level, new GameProfile(UUID.randomUUID(), "reborn-test"));
        try { // drop the 3-second spawn protection so damage tests land (dev runtime uses Mojang names)
            var f = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(this, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        CommonHooks.onLivingDeath(this, source);
    }
}
