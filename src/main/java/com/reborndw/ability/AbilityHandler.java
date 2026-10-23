package com.reborndw.ability;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.FlameLogic;
import com.reborndw.flame.Rank;
import com.reborndw.network.ActionPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Routes hotkeys. Each key fires from the first source with an ability: held weapon, gloves, then the ring's innate kit. */
public final class AbilityHandler {
    private AbilityHandler() {}

    public static void handle(ServerPlayer player, ActionPayload.Action action) {
        handle(player, action, true);
    }

    public static void handle(ServerPlayer player, ActionPayload.Action action, boolean pressed) {
        FlameData d = FlameLogic.get(player);
        if (!d.awakened || d.active == null || player.isSpectator()) return;
        FlameKit kit = Kits.get(d.active);
        if (!pressed) {
            if (kit != null) kit.onRelease(player, d, action);
            if (d.heldAction == action) d.heldAction = null;
            return;
        }
        switch (action) {
            case HYPER -> FlameLogic.toggleHyper(player);
            case RELEASE -> FlameLogic.release(player);
            case SIGNATURE -> player.displayClientMessage(Component.translatable("message.reborn_dying_will.signature_needs_gear"), true);
            case PRIMARY, SECONDARY -> {
                if (kit == null) {
                    player.displayClientMessage(Component.translatable("message.reborn_dying_will.kit_not_ready",
                            Component.translatable(d.active.translationKey()).withColor(d.active.color)), true);
                    return;
                }
                if (kit.beforeGate(player, d, action)) return;
                Rank need = action == ActionPayload.Action.PRIMARY ? kit.primaryRank() : kit.secondaryRank();
                Rank eff = RingHelper.effectiveRank(player);
                d.cachedRank = eff;
                if (!RingHelper.hasRing(player)) {
                    player.displayClientMessage(Component.translatable("message.reborn_dying_will.needs_ring",
                            Component.translatable(d.active.translationKey()).withColor(d.active.color)), true);
                    return;
                }
                if (!eff.atLeast(need)) {
                    player.displayClientMessage(Component.translatable("message.reborn_dying_will.needs_rank", need.name()), true);
                    return;
                }
                if (action == ActionPayload.Action.PRIMARY) kit.primary(player, d, eff);
                else kit.secondary(player, d, eff);
            }
        }
    }

    /** Per-tick upkeep for the active flame's kit; also tells the previous kit when the flame changed. */
    public static void tick(ServerPlayer player, FlameData d) {
        if (d.kitFlame != d.active) {
            FlameKit old = d.kitFlame == null ? null : Kits.get(d.kitFlame);
            if (old != null) old.onDeactivate(player, d);
            d.heldAction = null;
            d.kitFlame = d.active;
            d.rankRefresh = 0;
        }
        if (!d.awakened || d.active == null) return;
        if (--d.rankRefresh <= 0) {
            Rank eff = RingHelper.effectiveRank(player);
            boolean ring = RingHelper.hasRing(player);
            if (eff != d.cachedRank || ring != d.cachedHasRing) d.markDirty(); // the HUD's ability panel shows these
            d.cachedRank = eff;
            d.cachedHasRing = ring;
            d.rankRefresh = 10;
        }
        FlameKit kit = Kits.get(d.active);
        if (kit != null) kit.tick(player, d, d.cachedRank);
    }

    public static boolean has(Flame flame) {
        return Kits.get(flame) != null;
    }
}
