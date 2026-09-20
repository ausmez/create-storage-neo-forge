package net.fxnt.fxntstorage.backpack.client.menu.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiIconSprites(GuiIcon enabled, GuiIcon disabled) {

    public GuiIconSprites(GuiIcon icon) {
        this(icon, icon);
    }

    public GuiIcon get(boolean isActive) {
        return isActive ? enabled : disabled;
    }

    public static void renderButton(GuiGraphics graphics, GuiIconSprites sprites,
                                    boolean active, boolean hovered,
                                    int x, int y, int width, int height) {
        renderButton(graphics, sprites.get(active), active, hovered, x, y, width, height);
    }

    public static void renderButton(GuiGraphics graphics, GuiIcon icon,
                                    boolean active, boolean hovered,
                                    int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        icon.render(graphics, x, y, width, height);
        if (active && hovered) {
            GuiIcon.HIGHLIGHT.render(graphics, x, y, width, height);
        }
        RenderSystem.disableBlend();
    }
}
