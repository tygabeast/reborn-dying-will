package com.reborndw.ability;

import com.reborndw.flame.FlameData;
import com.reborndw.flame.Rank;
import com.reborndw.network.ActionPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * A flame's innate ring kit: a property passive from E, a Primary from D and a Secondary from C.
 * AbilityHandler checks the ring and rank before calling primary/secondary.
 */
public interface FlameKit {
    Rank primaryRank();

    Rank secondaryRank();

    /** Runs before the ring and rank check; return true to consume the key (e.g. switching a toggle off). */
    default boolean beforeGate(ServerPlayer player, FlameData d, ActionPayload.Action action) { return false; }

    void primary(ServerPlayer player, FlameData d, Rank eff);

    void secondary(ServerPlayer player, FlameData d, Rank eff);

    /** The key was let go (for hold abilities). */
    default void onRelease(ServerPlayer player, FlameData d, ActionPayload.Action action) {}

    /** Every tick while this flame is active. {@code eff} is the effective rank (E without a ring). */
    default void tick(ServerPlayer player, FlameData d, Rank eff) {}

    /** The player switched away from this flame (or died): end toggles and remove passive modifiers. */
    default void onDeactivate(ServerPlayer player, FlameData d) {}
}
