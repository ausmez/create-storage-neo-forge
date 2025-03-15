package net.fxnt.fxntstorage.backpacks.main;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class BackpackClientTooltip implements ClientTooltipComponent {
    private static int width = 0;
    private static int height = 0;

    private static final int ICONS_PER_ROW = 9;
    private static final int ICON_SIZE = 18;

    private final BackpackTooltip component;

    public BackpackClientTooltip(BackpackTooltip tooltip) {
        this.component = tooltip;
        this.calculateHeight();
        this.calculateWidth();
    }

    private void calculateWidth() {
        int upgradesWidth = component.upgrades.size() * ICON_SIZE;
        int contentsWidth = this.calculateContentsWidth();
        int tooltipContentsWidth = component.tooltipText.stream().map(this::getTooltipWidth).max(Comparator.naturalOrder()).orElse(0);
        width = Math.max(Math.max(upgradesWidth, contentsWidth), tooltipContentsWidth);
    }

    private int getTooltipWidth(Component component) {
        return Minecraft.getInstance().font.width(component.getVisualOrderText());
    }

    private int calculateContentsWidth() {
        Font fontRenderer = Minecraft.getInstance().font;
        int contentsWidth = 0;

        for (int i = 0; i < component.storage.size() && i < ICONS_PER_ROW; i++) {
            int countWidth = this.getStackCountWidth(fontRenderer, component.storage.get(i));
            contentsWidth += Math.max(countWidth, ICON_SIZE);
        }

        return contentsWidth;
    }

    private void calculateHeight() {
        int upgradesHeight = component.upgrades.isEmpty() ? 0 : 32;
        int inventoryHeight = component.storage.isEmpty() ? 0 : 12 + (1 + (component.storage.size() - 1) / ICONS_PER_ROW) * 20;
        int totalHeight = upgradesHeight + inventoryHeight + component.tooltipText.size() * 10;
        height = totalHeight > 0 ? totalHeight : 12;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return width;
    }

    @Override
    public void renderImage(@NotNull Font pFont, int pX, int pY, @NotNull GuiGraphics pGuiGraphics) {
        this.renderTooltip(pFont, pX, pY, pGuiGraphics);
    }

    private void renderTooltip(Font font, int leftX, int topY, GuiGraphics guiGraphics) {
        for (Component tooltipLine : component.tooltipText) {
            topY = this.renderTooltipText(guiGraphics, leftX, topY, font, tooltipLine);
        }
        this.renderContentsTooltip(font, leftX, topY, guiGraphics);
    }

    private void renderContentsTooltip(Font font, int leftX, int topY, GuiGraphics guiGraphics) {
        if (!component.upgrades.isEmpty()) {
            topY = this.renderTooltipText(guiGraphics, leftX, topY, font, Component.literal("Upgrades:").withStyle(ChatFormatting.YELLOW));
            topY = this.renderUpgrades(guiGraphics, leftX, topY);
        }
        if (!component.storage.isEmpty()) {
            topY = this.renderTooltipText(guiGraphics, leftX, topY, font, Component.literal("Inventory:").withStyle(ChatFormatting.YELLOW));
            this.renderContents(font, leftX, topY, guiGraphics);
        }
    }

    private int renderUpgrades(GuiGraphics guiGraphics, int leftX, int topY) {
        int x = leftX;

        for (ItemStack upgrade : component.upgrades) {
            guiGraphics.renderItem(upgrade, x, topY);
            x += ICON_SIZE;
        }

        topY += 20;
        return topY;
    }

    private void renderContents(Font font, int leftX, int topY, GuiGraphics guiGraphics) {
        int x = leftX;

        for (int i = 0; i < component.storage.size(); i++) {
            int y = topY + i / ICONS_PER_ROW * 20;
            if (i % ICONS_PER_ROW == 0) x = leftX;

            ItemStack stack = component.storage.get(i);
            int stackWidth = Math.max(this.getStackCountWidth(font, stack), ICON_SIZE);
            int xOffset = stackWidth - ICON_SIZE;
            guiGraphics.renderItem(stack, x + xOffset, y);
            guiGraphics.renderItemDecorations(font, stack, x + xOffset, y, (stack.getCount() > 1) ? formatNumber(stack.getCount()) : null);
            x += stackWidth;
        }
    }

    private int getStackCountWidth(Font font, ItemStack stack) {
        return font.width(formatNumber(stack.getCount())) + 2;
    }

    private int renderTooltipText(GuiGraphics guiGraphics, int leftX, int topY, Font font, Component tooltip) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, (double) 200.0F);
        MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        font.drawInBatch(tooltip, (float) leftX, (float) topY, 16777215, true, poseStack.last().pose(), renderTypeBuffer, Font.DisplayMode.NORMAL, 0, 15728880);
        renderTypeBuffer.endBatch();
        poseStack.translate(0.0F, 0.0F, (double) -200.0F);
        poseStack.popPose();
        return topY + 10;
    }

    private String formatNumber(int number) {
        if (number < 1000) return String.valueOf(number); // No suffix, just the number

        // Format for thousands (k)
        if (number < 1000000) {
            double value = number / 1000.0;
            return String.format("%.1fk", value); // 1 decimal place for thousands
        }

        // Format for millions (M)
        double value = number / 1000000.0;
        return String.format("%.2fM", value); // 2 decimal places for millions
    }

}
