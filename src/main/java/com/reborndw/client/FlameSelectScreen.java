package com.reborndw.client;

import com.reborndw.flame.Flame;
import com.reborndw.flame.FlameData;
import com.reborndw.flame.Form;
import com.reborndw.network.SelectFlamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pick the active flame from the ones you've unlocked, grouped by family. */
public class FlameSelectScreen extends Screen {
    private static final int BW = 120, BH = 20, GAP = 4, COL_GAP = 12;
    private final List<Button> flameButtons = new ArrayList<>();
    private final Map<Button, Flame> buttonFlames = new java.util.HashMap<>();
    private final Map<Flame.Family, int[]> headers = new EnumMap<>(Flame.Family.class);
    private Flame builtActive;
    private int statusY;

    public FlameSelectScreen() {
        super(Component.translatable("screen.reborn_dying_will.select_flame"));
    }

    private static FlameData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : ClientFlameCache.get(mc.player.getId());
    }

    @Override
    protected void init() {
        flameButtons.clear();
        buttonFlames.clear();
        headers.clear();
        FlameData d = data();
        if (d == null) return;
        builtActive = d.active;

        Map<Flame.Family, List<Flame>> byFamily = new EnumMap<>(Flame.Family.class);
        for (Flame f : Flame.values()) if (d.unlocked.contains(f)) byFamily.computeIfAbsent(f.family, k -> new ArrayList<>()).add(f);
        int cols = Math.max(1, byFamily.size());
        int rows = byFamily.values().stream().mapToInt(List::size).max().orElse(1);
        int totalW = cols * BW + (cols - 1) * COL_GAP;
        int totalH = 12 + rows * (BH + GAP);
        int left = (width - totalW) / 2;
        int top = Math.max(30, (height - totalH) / 2 - 10);

        int col = 0;
        for (var entry : byFamily.entrySet()) {
            int x = left + col * (BW + COL_GAP);
            headers.put(entry.getKey(), new int[]{x + BW / 2, top});
            int y = top + 12;
            for (Flame f : entry.getValue()) {
                Component name = Component.translatable(f.translationKey()).withColor(f.color)
                        .append(Component.literal("  " + FlameHud.rankLetter(d.capacity(f))).withColor(0xC0C0C0));
                Component label = f == d.active ? Component.translatable("screen.reborn_dying_will.active", name) : name;
                Button b = Button.builder(label, btn -> choose(f)).bounds(x, y, BW, BH).build();
                addRenderableWidget(b);
                flameButtons.add(b);
                buttonFlames.put(b, f);
                y += BH + GAP;
            }
            col++;
        }
        statusY = top + totalH + 6;
        updateButtons();
    }

    private void choose(Flame f) {
        PacketDistributor.sendToServer(new SelectFlamePayload(f));
        onClose();
    }

    private void updateButtons() {
        FlameData d = data();
        if (d == null) return;
        boolean canSwitch = d.form == Form.NONE && d.switchCooldownTicks <= 0;
        for (Button b : flameButtons) b.active = canSwitch && buttonFlames.get(b) != d.active;
    }

    @Override
    public void tick() {
        FlameData d = data();
        if (d == null || !d.awakened) { onClose(); return; }
        if (d.active != builtActive) rebuildWidgets();
        else updateButtons();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        FlameData d = data();
        if (d == null) return;
        int titleY = headers.values().stream().mapToInt(h -> h[1]).min().orElse(40) - 18;
        g.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);
        for (var h : headers.entrySet())
            g.drawCenteredString(font, Component.translatable("screen.reborn_dying_will.family." + h.getKey().name().toLowerCase()),
                    h.getValue()[0], h.getValue()[1], 0xA0A0A0);

        Component status;
        int color = 0xFFB0B0B0;
        if (d.unlocked.size() < 2) status = Component.translatable("screen.reborn_dying_will.only_one");
        else if (d.form != Form.NONE) { status = Component.translatable("screen.reborn_dying_will.in_form"); color = 0xFFFF8060; }
        else if (d.switchCooldownTicks > 0) { status = Component.translatable("screen.reborn_dying_will.cooldown", d.switchCooldownTicks / 20 + 1); color = 0xFFFFD060; }
        else status = Component.translatable("screen.reborn_dying_will.empties_pool");
        g.drawCenteredString(font, status, width / 2, statusY, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // the same key that opened the screen closes it
        if (ClientSetup.SWITCH_FLAME.matches(keyCode, scanCode)) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
