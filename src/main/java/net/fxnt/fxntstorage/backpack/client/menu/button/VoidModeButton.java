package net.fxnt.fxntstorage.backpack.client.menu.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

@OnlyIn(Dist.CLIENT)
public class VoidModeButton extends Button {

    private static final GuiIcon[] MODE_ICONS = {GuiIcon.VOID_ALWAYS, GuiIcon.VOID_SLOT, GuiIcon.VOID_STORAGE};

    private final IntFunction<Component> tooltipResolver;

    private int mode;

    public VoidModeButton(int x, int y, int width, int height,
                          int initialMode,
                          IntFunction<Component> tooltipResolver,
                          OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.tooltipResolver = tooltipResolver;
        this.mode = initialMode;
        this.setTooltip(Tooltip.create(tooltipResolver.apply(initialMode)));
    }

    public void updateState(int newMode) {
        if (this.mode == newMode) return;
        this.mode = newMode;
        this.setTooltip(Tooltip.create(tooltipResolver.apply(newMode)));
    }

    public void refreshTooltip() {
        this.setTooltip(Tooltip.create(tooltipResolver.apply(mode)));
    }

    private static GuiIcon iconFor(int mode) {
        return mode >= 0 && mode < MODE_ICONS.length ? MODE_ICONS[mode] : MODE_ICONS[0];
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GuiIcon.BACKGROUND.render(guiGraphics, getX(), getY(), this.width, this.height);
        RenderSystem.disableBlend();

        GuiIconSprites.renderButton(guiGraphics, iconFor(mode), this.active, this.isHovered(),
                getX(), getY(), this.width, this.height);
    }
}
