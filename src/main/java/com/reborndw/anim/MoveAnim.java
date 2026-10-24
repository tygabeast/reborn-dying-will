package com.reborndw.anim;

import com.reborndw.network.MoveAnimPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How a player's body moves for each weapon move. The server picks one when a move fires and tells the player and
 * everyone tracking them; clients pose the model (third person) and the held item (first person) over {@link #ticks}.
 */
public enum MoveAnim {
    SLASH(9), PUNCH(7), COUNTER(20), THRUST(9), SPIN(11), FLURRY(9), DASH(9), LASH(9), BIND(11),
    BOW(11), GUN(5), BRACE(40), BASH(9), SLAM(11), CHARGE_HEAVY(60), CHARGE_FIST(60), SMASH(8), HEAVY_PUNCH(8);

    public final int ticks;

    MoveAnim(int ticks) { this.ticks = ticks; }

    public static void play(ServerPlayer p, MoveAnim anim, InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new MoveAnimPayload(p.getId(), anim.ordinal(), hand == InteractionHand.OFF_HAND));
    }
}
