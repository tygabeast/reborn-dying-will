package com.reborndw.client.anim;

import com.reborndw.anim.MoveAnim;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Which players are mid-move on this client, and how far along. Entries end on their own; nothing else is stored. */
public final class MoveAnims {
    public record Playing(MoveAnim anim, boolean offHand, long start) {}

    private static final Int2ObjectOpenHashMap<Playing> PLAYING = new Int2ObjectOpenHashMap<>();
    /** Dev screenshots only: a fixed progress for every animation (negative = off). */
    public static float frozen = -1F;

    private MoveAnims() {}

    public static void play(int entityId, MoveAnim anim, boolean offHand) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        PLAYING.put(entityId, new Playing(anim, offHand, level.getGameTime()));
    }

    public static void stop(int entityId) { PLAYING.remove(entityId); }

    @Nullable
    public static Playing get(Player p) { return PLAYING.isEmpty() ? null : PLAYING.get(p.getId()); }

    /** 0..1 through the animation, or a negative number once it is over. */
    public static float progress(Playing pl, Player p, float partial) {
        if (frozen >= 0F) return frozen;
        float t = (p.level().getGameTime() - pl.start() + partial) / pl.anim().ticks;
        return t > 1F ? -1F : Math.max(0F, t);
    }

    /** Which arm makes the move. */
    public static HumanoidArm arm(Playing pl, Player p) {
        HumanoidArm main = p.getMainArm();
        return pl.offHand() ? main.getOpposite() : main;
    }

    public static InteractionHand hand(Playing pl) { return pl.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }

    public static void tick() {
        if (PLAYING.isEmpty() || frozen >= 0F) return;
        var level = Minecraft.getInstance().level;
        if (level == null) { PLAYING.clear(); return; }
        long now = level.getGameTime();
        PLAYING.values().removeIf(pl -> now - pl.start() > pl.anim().ticks + 1 || now < pl.start());
    }

    public static void clear() { PLAYING.clear(); }
}
