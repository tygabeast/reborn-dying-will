package com.reborndw.client;

import com.reborndw.config.ClientConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import com.reborndw.registry.ModRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/** The flame meter. Position, orientation and size come from the client config. */
public class FlameHud implements LayeredDraw.Layer {
    private static final int LINE = 10;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        FlameData d = ClientFlameCache.get(mc.player.getId());
        if (d == null || !d.awakened || d.active == null) return;
        Flame f = d.active;
        double cap = Math.max(1, d.usableCapacity(f));
        double frac = Math.max(0, Math.min(1, d.pool / cap));
        int color = 0xFF000000 | f.color;

        // label lines: name, rank and pool; then the form timer, weakness or shield
        List<Component> lines = new ArrayList<>();
        boolean labels = ClientConfig.METER_LABELS.get();
        if (labels) {
            lines.add(Component.translatable(f.translationKey()).append(" " + d.syncedRank.name() + "  " + Math.round(d.pool) + "/" + Math.round(cap)));
            String state = switch (d.form) {
                case DYING_WILL -> "DW " + effectTime(mc, ModRegistry.DYING_WILL);
                case HYPER -> "HYPER";
                case ULTIMATE -> "ULTIMATE " + effectTime(mc, ModRegistry.ULTIMATE_DYING_WILL);
                case NONE -> d.ownWeakness && mc.player.hasEffect(MobEffects.WEAKNESS) ? "Weak " + effectTime(mc, MobEffects.WEAKNESS) : "";
            };
            if (d.shieldActive) state += (state.isEmpty() ? "" : "  ") + "Shield " + Math.round(d.shieldHp / 2.0) + "❤";
            if (!state.isEmpty()) lines.add(Component.literal(state));
        }
        int textW = 0;
        for (Component c : lines) textW = Math.max(textW, mc.font.width(c));

        ClientConfig.Anchor anchor = ClientConfig.METER_ANCHOR.get();
        boolean vertical = ClientConfig.METER_ORIENTATION.get() == ClientConfig.Orientation.VERTICAL;
        int len = ClientConfig.METER_LENGTH.get(), thick = ClientConfig.METER_THICKNESS.get();
        int barW = vertical ? thick : len, barH = vertical ? len : thick;
        int boxW = vertical ? barW + (textW > 0 ? 4 + textW : 0) : Math.max(barW, textW);
        int boxH = vertical ? Math.max(barH, lines.size() * LINE) : barH + 2 * LINE + 1;
        int[] pos = HudPlacement.place(anchor, ClientConfig.METER_X.get(), ClientConfig.METER_Y.get(), boxW, boxH, g.guiWidth(), g.guiHeight());
        boolean right = HudPlacement.rightSide(anchor) || anchor == ClientConfig.Anchor.HOTBAR_LEFT && vertical;

        int bx, by;
        if (vertical) {
            bx = right ? pos[0] + boxW - barW : pos[0];
            by = pos[1] + boxH - barH;
        } else {
            bx = pos[0];
            by = pos[1] + boxH - barH;
        }
        drawBar(g, d, f, bx, by, barW, barH, frac, vertical, color);

        // labels: above a horizontal bar, beside a vertical one
        for (int i = 0; i < lines.size(); i++) {
            Component c = lines.get(i);
            int tc = i == 0 ? color : (d.form == Form.NONE ? 0xFFB0B0B0 : 0xFFFFE070);
            int tx, ty;
            if (vertical) {
                tx = right ? bx - 4 - mc.font.width(c) : bx + barW + 4;
                ty = by + i * LINE;
            } else {
                tx = HudPlacement.rightSide(anchor) ? bx + barW - mc.font.width(c) : bx;
                ty = by - (lines.size() - i) * LINE;
            }
            g.drawString(mc.font, c, tx, ty, tc, true);
        }
    }

    private static void drawBar(GuiGraphics g, FlameData d, Flame f, int x, int y, int w, int h, double frac, boolean vertical, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
        if (vertical) {
            int filled = (int) Math.round(h * frac);
            g.fill(x, y + h - filled, x + w, y + h, color);
            if (d.form == Form.ULTIMATE) g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        } else {
            g.fill(x, y, x + (int) Math.round(w * frac), y + h, color);
            if (d.form == Form.ULTIMATE) g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        }
        double scar = d.scar[f.ordinal()];
        if (scar > 0) {
            double s = Math.min(1, scar / Math.max(1, d.capacity(f)));
            if (vertical) {
                int sh = (int) Math.round(h * s);
                g.fill(x + w - 1, y, x + w, y + sh, 0xFF7A1010);
            } else {
                int sw = (int) Math.round(w * s);
                g.fill(x + w - sw, y + h - 1, x + w, y + h, 0xFF7A1010);
            }
        }
    }

    private static String effectTime(Minecraft mc, Holder<MobEffect> effect) {
        MobEffectInstance e = mc.player.getEffect(effect);
        return e == null ? "0:00" : time(e.getDuration());
    }

    static String time(int ticks) {
        int s = Math.max(0, ticks / 20);
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    /** Display only, from the client's view of capacity; prefer FlameData.syncedRank. */
    static String rankLetter(double capacity) {
        long[] t = {100, 1000, 5000, 20000, 50000, 100000};
        String[] r = {"E", "D", "C", "B", "A", "S"};
        String out = "E";
        for (int i = 0; i < t.length; i++) if (capacity >= t[i]) out = r[i];
        return out;
    }
}
