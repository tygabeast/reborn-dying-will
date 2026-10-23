package com.reborndw.client;

import com.reborndw.RebornDW;
import com.reborndw.menu.RingForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RingForgeScreen extends AbstractContainerScreen<RingForgeMenu> {
    private static final ResourceLocation BG = RebornDW.id("textures/gui/ring_forge.png");

    public RingForgeScreen(RingForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("container.reborn_dying_will.ring_forge.metal"), 8, 69, 0x606060, false);
        g.drawString(font, Component.translatable("container.reborn_dying_will.ring_forge.chain"), 96, 25, 0x606060, false);
    }
}
