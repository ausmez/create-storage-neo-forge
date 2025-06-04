package net.fxnt.fxntstorage.backpack.tooltip;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BackpackClientTooltip implements ClientTooltipComponent {
    private static final int ICONS_PER_ROW = 9;
    private static final int ICON_SIZE = 18;

    private final BackpackTooltip component;
    private final int width;
    private final int height;

    public BackpackClientTooltip(BackpackTooltip tooltip) {
        this.component = tooltip;
        this.width = calculateWidth();
        this.height = calculateHeight();
    }

    private int calculateWidth() {
        Minecraft mc = Minecraft.getInstance();
        int upgradesWidth = component.upgrades.size() * ICON_SIZE;
        int contentsWidth = component.storage.stream()
                .limit(ICONS_PER_ROW)
                .mapToInt(stack -> Math.max(getStackCountWidth(mc.font, stack), ICON_SIZE))
                .sum();
        int tooltipTextWidth = component.tooltipText.stream()
                .mapToInt(text -> mc.font.width(text.getVisualOrderText()))
                .max()
                .orElse(0);
        return Math.max(Math.max(upgradesWidth, contentsWidth), tooltipTextWidth);
    }

    private int calculateHeight() {
        int upgradesHeight = component.upgrades.isEmpty() ? 0 : 32;
        int storageRows = (component.storage.size() + ICONS_PER_ROW - 1) / ICONS_PER_ROW;
        int inventoryHeight = component.storage.isEmpty() ? 0 : 12 + storageRows * 20;
        int textHeight = component.tooltipText.size() * 10;
        return Math.max(upgradesHeight + inventoryHeight + textHeight, 12);
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
        for (Component line : component.tooltipText) {
            pY = renderTooltipText(pGuiGraphics, pX, pY, pFont, line);
        }
        renderContents(pFont, pX, pY, pGuiGraphics);
    }

    private void renderContents(Font pFont, int pX, int pY, GuiGraphics pGuiGraphics) {
        if (!component.upgrades.isEmpty()) {
            pY = renderTooltipText(pGuiGraphics, pX, pY, pFont, Component.literal("Upgrades:").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
            pY = renderItemList(pFont, pX, pY, pGuiGraphics, component.upgrades, false);
        }
        if (!component.storage.isEmpty()) {
            pY = renderTooltipText(pGuiGraphics, pX, pY, pFont, Component.literal("Inventory:").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
            renderItemList(pFont, pX, pY, pGuiGraphics, component.storage, true);
        }
    }

    private int renderItemList(Font font, int xStart, int yStart, GuiGraphics guiGraphics, java.util.List<ItemStack> items, boolean multiline) {
        int x = xStart;
        int y = yStart;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);

            if (multiline && i > 0 && i % ICONS_PER_ROW == 0) {
                x = xStart;
                y += 20;
            }

            int stackWidth = Math.max(getStackCountWidth(font, stack), ICON_SIZE);
            int xOffset = stackWidth - ICON_SIZE;

            guiGraphics.renderItem(stack, x + xOffset, y);
            if (stack.getCount() > 1) {
                guiGraphics.renderItemDecorations(font, stack, x + xOffset, y, formatNumber(stack.getCount()));
            }

            x += multiline ? stackWidth : ICON_SIZE;
        }

        return multiline ? y + 20 : y + (items.isEmpty() ? 0 : 20);
    }

    private int getStackCountWidth(Font font, ItemStack stack) {
        return font.width(formatNumber(stack.getCount())) + 2;
    }

    private int renderTooltipText(GuiGraphics guiGraphics, int x, int y, Font font, Component tooltip) {
        guiGraphics.drawString(font, tooltip, x, y, 0xFFFFFF, true);
        return y + 10;
    }

    private String formatNumber(int number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1_000_000) return String.format("%.1fk", number / 1000.0);
        return String.format("%.2fM", number / 1_000_000.0);
    }

}
