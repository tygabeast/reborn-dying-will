package com.reborndw.client;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import com.reborndw.registry.ModRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** The flame meter, to the left of the hotbar. */
public class FlameHud implements LayeredDraw.Layer {
    private static final int W = 82, H = 5;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        FlameData d = ClientFlameCache.get(mc.player.getId());
        if (d == null || !d.awakened || d.active == null) return;
        Flame f = d.active;
        double cap = Math.max(1, d.usableCapacity(f));
        double frac = Math.max(0, Math.min(1, d.pool / cap));

        int x = g.guiWidth() / 2 - 91 - 8 - W;
        int y = g.guiHeight() - 12;
        int color = 0xFF000000 | f.color;

        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xC0000000);
        g.fill(x, y, x + (int) Math.round(W * frac), y + H, color);
        if (d.form == Form.ULTIMATE) g.fill(x, y, x + W, y + 1, 0xFFFFFFFF);
        if (d.scar[f.ordinal()] > 0) {
            int scarW = (int) Math.round(W * Math.min(1, d.scar[f.ordinal()] / Math.max(1, d.capacity(f))));
            g.fill(x + W - scarW, y + H - 1, x + W, y + H, 0xFF7A1010);
        }

        String rank = rankLetter(d.capacity(f));
        Component label = Component.translatable(f.translationKey()).append(" " + rank + "  " + Math.round(d.pool) + "/" + Math.round(cap));
        g.drawString(mc.font, label, x, y - 10, color, true);

        // timers come from the status effects themselves, which the game keeps in sync
        String state = switch (d.form) {
            case DYING_WILL -> "DW " + effectTime(mc, ModRegistry.DYING_WILL);
            case HYPER -> "HYPER";
            case ULTIMATE -> "ULTIMATE " + effectTime(mc, ModRegistry.ULTIMATE_DYING_WILL);
            case NONE -> d.ownWeakness && mc.player.hasEffect(MobEffects.WEAKNESS) ? "Weak " + effectTime(mc, MobEffects.WEAKNESS) : "";
        };
        if (d.shieldActive) state += (state.isEmpty() ? "" : "  ") + "Shield " + Math.round(d.shieldHp / 2.0) + "❤";
        if (!state.isEmpty()) {
            int sc = d.form == Form.NONE ? 0xFFB0B0B0 : 0xFFFFE070;
            g.drawString(mc.font, state, x, y - 20, sc, true);
        }
    }

    private static String effectTime(Minecraft mc, Holder<MobEffect> effect) {
        MobEffectInstance e = mc.player.getEffect(effect);
        return e == null ? "0:00" : time(e.getDuration());
    }

    private static String time(int ticks) {
        int s = Math.max(0, ticks / 20);
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    /** Display only; the server owns the real thresholds, so this uses the default bands. */
    static String rankLetter(double capacity) {
        long[] t = {100, 1000, 5000, 20000, 50000, 100000};
        String[] r = {"E", "D", "C", "B", "A", "S"};
        String out = "E";
        for (int i = 0; i < t.length; i++) if (capacity >= t[i]) out = r[i];
        return out;
    }
}
