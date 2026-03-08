package net.fxnt.fxntstorage.backpack.client.menu.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class SpriteButton<T> extends Button {

    private final Function<T, WidgetSprites> spriteResolver;
    private final Function<T, Component> tooltipResolver;

    private final int textureWidth;
    private final int textureHeight;

    private WidgetSprites currentSprites;
    private T lastState;

    public SpriteButton(int x, int y, int width, int height,
                        T initialState,
                        Function<T, WidgetSprites> spriteResolver,
                        Function<T, Component> tooltipResolver,
                        OnPress onPress) {
        this(x, y, width, height, width, height, initialState, spriteResolver, tooltipResolver, onPress);
    }

    public SpriteButton(int x, int y, int width, int height,
                        int textureWidth, int textureHeight,
                        T initialState,
                        Function<T, WidgetSprites> spriteResolver,
                        Function<T, Component> tooltipResolver,
                        OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.spriteResolver = spriteResolver;
        this.tooltipResolver = tooltipResolver;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;

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
        if (currentSprites == null) return;

        boolean focused = this.isHovered();
        ResourceLocation texture = currentSprites.get(this.active, focused);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blit(texture,
                this.getX(), this.getY(),
                0, 0,
                this.width, this.height,
                this.textureWidth, this.textureHeight
        );

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
