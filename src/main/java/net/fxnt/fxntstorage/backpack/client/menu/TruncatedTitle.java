package net.fxnt.fxntstorage.backpack.client.menu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.Optional;

public final class TruncatedTitle {
    private boolean truncated;
    private int drawnWidth;
    private int x, y;
    private Component fullTitle;

    // Call from renderLabels (inside the leftPos/topPos-translated pose)
    public void draw(GuiGraphics graphics, Font font, Component title, int x, int y, int maxWidth, int color) {
        this.x = x;
        this.y = y;
        this.fullTitle = title;

        FormattedCharSequence drawn;
        if (font.width(title) > maxWidth) {
            int ellipsisWidth = font.width("…");
            FormattedText trimmed = font.substrByWidth(title, maxWidth - ellipsisWidth);
            FormattedText ellipsis = FormattedText.of("…", trailingStyle(trimmed));
            drawn = Language.getInstance().getVisualOrder(FormattedText.composite(trimmed, ellipsis));
            truncated = true;
        } else {
            drawn = title.getVisualOrderText();
            truncated = false;
        }
        drawnWidth = font.width(drawn);
        graphics.drawString(font, drawn, x, y, color, false);
    }

    // Call from render (raw screen coords); shows the full title when hovering a truncated one
    public void renderTooltipIfHovered(GuiGraphics g, Font font, int leftPos, int topPos, int mouseX, int mouseY) {
        if (!truncated) return;
        int x0 = leftPos + x;
        int y0 = topPos + y;
        if (mouseX >= x0 && mouseX < x0 + drawnWidth
                && mouseY >= y0 && mouseY < y0 + font.lineHeight) {
            g.renderTooltip(font, fullTitle, mouseX, mouseY);
        }
    }

    // Style of the last non-empty run, so the ellipsis matches the color/format at the cut point
    private static Style trailingStyle(FormattedText text) {
        Style[] last = { Style.EMPTY };
        text.visit((style, content) -> {
            if (!content.isEmpty()) last[0] = style;
            return Optional.empty();
        }, Style.EMPTY);
        return last[0];
    }
}
