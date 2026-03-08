package net.fxnt.fxntstorage.backpack.client.menu.button;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradePanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class ItemSpriteButton<T> extends Button {

    private static final WidgetSprites BACKGROUND = UpgradePanel.createWidgetSprites("background");

    private final Function<T, ResourceLocation> overlayResolver;
    private final Function<T, Component> tooltipResolver;
    private final Function<T, ItemStack> itemResolver;

    private T lastState;
    private ResourceLocation currentOverlay;
    private ItemStack currentItem;

    private final int itemRenderSize;

    public ItemSpriteButton(int x, int y, int width, int height,
                            T initialState,
                            Function<T, ResourceLocation> overlayResolver,
                            Function<T, Component> tooltipResolver,
                            Function<T, ItemStack> itemResolver,
                            int itemRenderSize,
                            OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);

        this.overlayResolver = overlayResolver;
        this.tooltipResolver = tooltipResolver;
        this.itemResolver = itemResolver;
        this.itemRenderSize = itemRenderSize;

        this.lastState = initialState;
        this.currentOverlay = overlayResolver.apply(initialState);
        this.currentItem = itemResolver.apply(initialState);

        this.setTooltip(Tooltip.create(tooltipResolver.apply(initialState)));
    }

    public void updateState(T newState) {
        if (!Objects.equals(this.lastState, newState)) {
            this.lastState = newState;
            this.currentOverlay = overlayResolver.apply(newState);
            this.currentItem = itemResolver.apply(newState);
            this.setTooltip(Tooltip.create(tooltipResolver.apply(newState)));
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();

        // Background
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blitSprite(BACKGROUND.get(this.active, this.isHovered()), x, y, this.width, this.height);

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ItemStack
        if (!currentItem.isEmpty()) {
            renderScaledItem(guiGraphics, currentItem, x, y, this.width, this.height, itemRenderSize);
        }

        // Overlay icon rendered above the item
        if (currentOverlay != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);

            guiGraphics.blitSprite(currentOverlay, x, y, this.width, this.height);
            
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack stack,
                                  int buttonX, int buttonY, int buttonWidth, int buttonHeight, int targetSize) {
        float scale = targetSize / 16f;
        int x = buttonX + (buttonWidth - targetSize) / 2;
        int y = buttonY + (buttonHeight - targetSize) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }
}