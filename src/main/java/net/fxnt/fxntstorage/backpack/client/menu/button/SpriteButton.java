package net.fxnt.fxntstorage.backpack.client.menu.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class SpriteButton<T> extends Button {

    private final Function<T, WidgetSprites> spriteResolver;
    private final Function<T, Component> tooltipResolver;

    private WidgetSprites currentSprites;
    private T lastState;

    public SpriteButton(int x, int y, int width, int height, T initialState,
                        Function<T, WidgetSprites> spriteResolver,
                        Function<T, Component> tooltipResolver,
                        OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.spriteResolver = spriteResolver;
        this.tooltipResolver = tooltipResolver;

        this.lastState = initialState;
        this.currentSprites = spriteResolver.apply(initialState);
        this.setTooltip(Tooltip.create(tooltipResolver.apply(initialState)));
    }

    public void updateState(T newState) {
        if (!Objects.equals(this.lastState, newState)) {
            this.lastState = newState;
            this.currentSprites = spriteResolver.apply(newState);
            this.setTooltip(Tooltip.create(tooltipResolver.apply(newState)));
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blitSprite(currentSprites.get(this.active, this.isHovered()), getX(), getY(), this.width, this.height);
    }
}