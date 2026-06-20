package net.fxnt.fxntstorage.simple_storage;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public abstract class AbstractSimpleStorageBoxScreen<M extends AbstractContainerMenu & ISimpleStorageBoxMenu>
        extends AbstractContainerScreen<M> {

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FXNTStorage.MOD_ID, "textures/gui/container/simple_storage_box_screen.png");
    private static final List<ResourceLocation> EMPTY_SLOT_UTILITY_UPGRADES = List.of(
            modLoc("item/void_template"),
            modLoc("item/compacting_template")
    );
    private static final int GUI_TEXTURE_WIDTH = 176;
    private static final int GUI_TEXTURE_HEIGHT = 176;

    private final CyclingSlotBackground utilityIcon = new CyclingSlotBackground(0);

    protected AbstractSimpleStorageBoxScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_TEXTURE_WIDTH;
        imageHeight = GUI_TEXTURE_HEIGHT;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.utilityIcon.tick(EMPTY_SLOT_UTILITY_UPGRADES);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        int filterX = leftPos + 30;
        int filterY = topPos + 20;
        renderFilterItem(graphics, filterX, filterY);
        this.renderTooltip(graphics, mouseX, mouseY);
        ItemStack filterItem = menu.getDisplayedItem();
        if (!filterItem.isEmpty() && mouseX >= filterX && mouseX < filterX + 32 && mouseY >= filterY && mouseY < filterY + 32) {
            graphics.renderTooltip(font, filterItem, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
        this.utilityIcon.render(this.menu, graphics, delta, this.leftPos, this.topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 7, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 7, 93 - 11, 0x404040, false);

        String storedText = Component.translatable("container.fxntstorage.simple_storage_box.stored").append(": ").getString();
        String capacityText = Component.translatable("container.fxntstorage.simple_storage_box.capacity").append(": ").getString();
        String voidText = Component.translatable("container.fxntstorage.simple_storage_box.upgrade").append(": ").getString();

        graphics.drawString(font, storedText + menu.getDisplayedStoredAmount(), 66, 20, 0x404040, false);
        graphics.drawString(font, capacityText + menu.getDisplayedMaxCapacity(), 66, 32, 0x404040, false);
        graphics.drawString(font, voidText + (
                menu.getSlot(0).getItem().is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE) ? Component.translatable("container.fxntstorage.simple_storage_box.compacting").getString()
                        : menu.getSlot(0).getItem().is(ModItems.STORAGE_BOX_VOID_UPGRADE)
                          ? Component.translatable("container.fxntstorage.simple_storage_box.void").getString()
                          : Component.translatable("container.fxntstorage.simple_storage_box.none").getString()
        ), 66, 44, 0x404040, false);
    }

    private void renderFilterItem(GuiGraphics graphics, int x, int y) {
        ItemStack itemStack = menu.getDisplayedItem();
        if (!itemStack.isEmpty()) {
            renderFilterItemStack(graphics, itemStack, x + 16f, y + 16f);
            renderFilterItemDecoration(graphics, font, itemStack, x, y, menu.getDisplayedStoredAmount());
        }
    }

    private void renderFilterItemStack(GuiGraphics graphics, ItemStack stack, float x, float y) {
        PoseStack poseStack = graphics.pose();
        MultiBufferSource buffer = graphics.bufferSource();

        BakedModel bakedModel = this.minecraft.getItemRenderer().getModel(stack, null, null, 0);
        poseStack.pushPose();
        poseStack.translate(x, y, 150);

        poseStack.mulPose((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
        poseStack.scale(24.0F, 24.0F, 24.0F);
        boolean bl = !bakedModel.usesBlockLight();
        if (bl) {
            Lighting.setupForFlatItems();
        }

        this.minecraft.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
        graphics.flush();
        if (bl) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
    }

    private void renderFilterItemDecoration(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, int amount) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        if (!stack.isEmpty() && stack.isBarVisible()) {
            int i = stack.getBarWidth();
            int j = stack.getBarColor();
            int xOffset = x + 3;
            int yOffset = y + 28;
            int width = 26;
            int height = 2;
            graphics.fill(RenderType.guiOverlay(), xOffset, yOffset, xOffset + width, yOffset + height, -16777216);
            graphics.fill(RenderType.guiOverlay(), xOffset, yOffset, xOffset + (i * 2), yOffset + (height / 2), j | -16777216);
        }

        String string = Util.formatNumber(amount);
        poseStack.translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(font, string, x + 33 - font.width(string), y + 25, 16777215, true);

        poseStack.popPose();
    }
}
