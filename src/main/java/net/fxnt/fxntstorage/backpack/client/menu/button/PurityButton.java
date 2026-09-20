package net.fxnt.fxntstorage.backpack.client.menu.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

@OnlyIn(Dist.CLIENT)
public class PurityButton extends Button {

    private static final GuiIcon[] PURITY_ICONS = {
            GuiIcon.PURITY_0, GuiIcon.PURITY_1, GuiIcon.PURITY_2, GuiIcon.PURITY_3
    };

    private final IntFunction<Component> tooltipResolver;

    private int purity;

    public PurityButton(int x, int y, int width, int height,
                        int initialPurity,
                        IntFunction<Component> tooltipResolver,
                        OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.tooltipResolver = tooltipResolver;
        this.purity = initialPurity;
        this.setTooltip(Tooltip.create(tooltipResolver.apply(initialPurity)));
    }

    public void updateState(int newPurity) {
        if (this.purity == newPurity) return;
        this.purity = newPurity;
        this.setTooltip(Tooltip.create(tooltipResolver.apply(newPurity)));
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int level = Mth.clamp(purity, 0, PURITY_ICONS.length - 1);

        GuiIconSprites.renderButton(guiGraphics, PURITY_ICONS[level], this.active, this.isHovered(),
                getX(), getY(), this.width, this.height);

        // Purity 0 means "any purity", so mark the empty droplet as a wildcard
        if (level == 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            GuiIcon.TILDE.render(guiGraphics, getX(), getY(), this.width, this.height);
            RenderSystem.disableBlend();
        }
    }
}
