package com.reborndw.client;

import com.reborndw.RebornDW;
import com.reborndw.menu.InfusionAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class InfusionAnvilScreen extends AbstractContainerScreen<InfusionAnvilMenu> {
    private static final ResourceLocation BG = RebornDW.id("textures/gui/infusion_anvil.png");

    public InfusionAnvilScreen(InfusionAnvilMenu menu, Inventory inv, Component title) {
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
        g.drawString(font, Component.translatable("container.reborn_dying_will.infusion_anvil.item"), 8, 69, 0x606060, false);
        g.drawString(font, Component.translatable("container.reborn_dying_will.infusion_anvil.gem"), 66, 69, 0x606060, false);
    }
}
