package com.reborndw.client;

import com.reborndw.client.look.FlameEmitter;
import com.reborndw.client.look.FlameLooks;
import com.reborndw.entity.FlameShotEntity;
import net.minecraft.client.Minecraft;

/** Client-only hook called from a flame shot's tick. */
public final class ShotTrail {
    private ShotTrail() {}

    public static void tick(FlameShotEntity e) {
        FlameEmitter.shot(Minecraft.getInstance(), e, FlameLooks.of(e.flame()));
    }
}
