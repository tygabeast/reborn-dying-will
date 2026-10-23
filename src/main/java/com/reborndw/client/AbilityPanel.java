package com.reborndw.client;

import com.reborndw.ability.KitInfo;
import com.reborndw.config.ClientConfig;
import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the active flame's abilities with their keys. Abilities you can't use yet (no ring, or rank too low)
 * are greyed out with the reason. Toggle with its key or the client config.
 */
public class AbilityPanel implements LayeredDraw.Layer {
    private static final int LIT = 0xFFFFFFFF, DIM = 0xFF6E6E6E, KEY = 0xFFFFD86A, LINE = 10, PAD = 4;

    private record Line(Component text, int color) {}

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.PANEL_SHOW.get() || mc.player == null || mc.options.hideGui || mc.player.isSpectator()
                || mc.getDebugOverlay().showDebugScreen()) return;
        FlameData d = ClientFlameCache.get(mc.player.getId());
        if (d == null || !d.awakened || d.active == null) return;
        Flame f = d.active;

        List<Line> lines = new ArrayList<>();
        MutableComponent title = Component.empty().append(Component.translatable(f.translationKey()).withColor(f.color))
                .append(Component.literal("  " + d.syncedRank.name()).withStyle(ChatFormatting.WHITE));
        title.append(d.syncedHasRing
                ? Component.translatable("hud.reborn_dying_will.panel.effective", d.syncedEffRank.name()).withStyle(ChatFormatting.GRAY)
                : Component.translatable("hud.reborn_dying_will.panel.no_ring").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(new Line(title, LIT));

        List<KitInfo.Entry> kit = KitInfo.of(f);
        if (kit.isEmpty()) {
            lines.add(new Line(Component.translatable("hud.reborn_dying_will.panel.no_kit"), DIM));
        }
        for (KitInfo.Entry e : kit) {
            switch (e.slot()) {
                case PASSIVE -> lines.add(new Line(Component.translatable("hud.reborn_dying_will.panel.passive")
                        .append(Component.translatable(e.nameKey())), LIT));
                case PRIMARY -> lines.add(abilityLine(ClientSetup.PRIMARY, e, d));
                case SECONDARY -> lines.add(abilityLine(ClientSetup.SECONDARY, e, d));
            }
        }
        lines.add(new Line(keyed(ClientSetup.SIGNATURE, Component.translatable("hud.reborn_dying_will.panel.signature"), true)
                .append(Component.translatable("hud.reborn_dying_will.panel.needs_gear")), DIM));
        MutableComponent hyper = keyed(ClientSetup.HYPER, Component.translatable(d.form == Form.HYPER
                ? "hud.reborn_dying_will.panel.hyper_on" : "hud.reborn_dying_will.panel.hyper"), !d.hyperUnlocked);
        if (!d.hyperUnlocked) hyper.append(Component.translatable("hud.reborn_dying_will.panel.locked"));
        lines.add(new Line(hyper, d.hyperUnlocked ? LIT : DIM));
        if (d.form != Form.NONE)
            lines.add(new Line(keyed(ClientSetup.RELEASE, Component.translatable("hud.reborn_dying_will.panel.release"), false), LIT));
        if (d.unlocked.size() > 1)
            lines.add(new Line(keyed(ClientSetup.SWITCH_FLAME, Component.translatable("hud.reborn_dying_will.panel.switch"), false), LIT));

        float scale = (float) (double) ClientConfig.PANEL_SCALE.get();
        int w = 0;
        for (Line l : lines) w = Math.max(w, mc.font.width(l.text()));
        int boxW = w + PAD * 2, boxH = lines.size() * LINE + PAD * 2 - 2;
        int sw = (int) (g.guiWidth() / scale), sh = (int) (g.guiHeight() / scale);
        int[] pos = HudPlacement.place(ClientConfig.PANEL_ANCHOR.get(), ClientConfig.PANEL_X.get(), ClientConfig.PANEL_Y.get(), boxW, boxH, sw, sh);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1);
        if (ClientConfig.PANEL_BACKGROUND.get()) g.fill(pos[0], pos[1], pos[0] + boxW, pos[1] + boxH, 0x90000000);
        for (int i = 0; i < lines.size(); i++)
            g.drawString(mc.font, lines.get(i).text(), pos[0] + PAD, pos[1] + PAD + i * LINE, lines.get(i).color(), false);
        g.pose().popPose();
    }

    private static Line abilityLine(KeyMapping key, KitInfo.Entry e, FlameData d) {
        boolean usable = d.syncedHasRing && d.syncedEffRank.atLeast(e.minRank());
        MutableComponent text = keyed(key, Component.translatable(e.nameKey()), !usable);
        if (!e.style().isEmpty()) text.append(Component.translatable("hud.reborn_dying_will.panel." + e.style()));
        if (!d.syncedHasRing) text.append(Component.translatable("hud.reborn_dying_will.panel.needs_ring"));
        else if (!usable) text.append(Component.translatable("hud.reborn_dying_will.panel.needs_rank", e.minRank().name()));
        return new Line(text, usable ? LIT : DIM);
    }

    /** "[Z] Name": the key part in gold (grey when dimmed); the rest takes the line colour. */
    private static MutableComponent keyed(KeyMapping key, Component name, boolean dim) {
        Component keyName = key.isUnbound() ? Component.literal("-") : key.getTranslatedKeyMessage();
        MutableComponent keyPart = Component.literal("[").append(keyName.copy()).append("] ").withColor((dim ? DIM : KEY) & 0xFFFFFF);
        return Component.empty().append(keyPart).append(name.copy());
    }
}
