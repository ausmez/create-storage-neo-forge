package net.fxnt.fxntstorage.backpack.main;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.KeyPressedPacket;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.fxnt.fxntstorage.util.KeybindHandler.TOGGLE_BACKPACK_KEY;

@ParametersAreNonnullByDefault
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int CONTAINER_COLUMNS = 12;
    private static final int TOOL_SLOT_COLUMNS = 12;
    private static final int TOOL_SLOT_ROWS = 2;
    private static final int UPGRADE_SLOT_COLUMNS = 1;
    private static final int UPGRADE_SLOT_ROWS = 6;

    private final int itemSlots = menu.ITEM_SLOT_COUNT;
    private final int toolSlots = menu.TOOL_SLOT_COUNT;
    private final int totalSlots = menu.TOTAL_SLOT_COUNT;
    private final int totalRows = (int) Math.ceil((double) itemSlots / CONTAINER_COLUMNS);

    private int containerRows = 5;

    private static final int containerSlotsMinX = 29;
    private static final int containerSlotsMaxX = containerSlotsMinX + (Util.SLOT_SIZE * CONTAINER_COLUMNS);
    private static final int containerSlotsMinZ = 17;

    private int containerSlotsMaxZ = containerSlotsMinX + (Util.SLOT_SIZE * containerRows);
    private int containerSlotsHeight = containerSlotsMaxZ - containerSlotsMinZ;
    private static final int scrollBarMinX = containerSlotsMaxX + 4;
    private static final int scrollBarMaxX = scrollBarMinX + 14;
    private static final int scrollBarMinZ = containerSlotsMinZ;
    private int scrollBarMaxZ = containerSlotsMaxZ;

    private static final int upgradeSlotsMinX = 7;
    private static final int upgradeSlotsMinZ = containerSlotsMinZ;
    private static final int toolSlotsMinX = containerSlotsMinX;
    private int toolSlotsMinZ = containerSlotsMaxZ + 4;
    private int toolSlotsMaxZ = toolSlotsMinZ + (Util.SLOT_SIZE * TOOL_SLOT_ROWS);
    private final int inventorySlotsMinX = 60;
    private final int inventorySlotsMaxX = inventorySlotsMinX + (Util.SLOT_SIZE * 9);
    private int inventorySlotsMinZ = toolSlotsMaxZ + 15;
    private int inventorySlotsMaxZ = inventorySlotsMinZ + (Util.SLOT_SIZE * 3);
    private final int hotbarSlotsMinX = inventorySlotsMinX;
    private int hotbarSlotsMinZ = inventorySlotsMaxZ + 4;
    private int hotbarSlotsMaxZ = hotbarSlotsMinZ + Util.SLOT_SIZE;

    private static final int scrollThumbMinX = 270;
    private static final int scrollThumbMaxX = scrollThumbMinX + 12;
    private static final int scrollThumbMinZ = 0;
    private static final int scrollThumbMaxZ = 15;
    private static final int scrollThumbWidth = scrollThumbMaxX - scrollThumbMinX;
    private static final int scrollThumbHeight = scrollThumbMaxZ - scrollThumbMinZ;

    private boolean isDragging;
    private int scrollThumbY = 0;
    private int topVisibleRow, scrollYOffset;
    private int inventoryTextOffset = 11;

    private int containerExclusionZoneMinX, containerExclusionZoneMaxX, containerExclusionZoneMinZ, containerExclusionZoneMaxZ, containerExclusionZoneWidth, containerExclusionZoneHeight;
    private int inventoryExclusionZoneMinX, inventoryExclusionZoneMaxX, inventoryExclusionZoneMinZ, inventoryExclusionZoneMaxZ, inventoryExclusionZoneWidth, inventoryExclusionZoneHeight;

    private static final ResourceLocation GUI_TEXTURE_4 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/backpack_screen_4.png");
    private static final ResourceLocation GUI_TEXTURE_7 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/backpack_screen_7.png");
    private static final ResourceLocation GUI_TEXTURE_9 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/backpack_screen_9.png");
    private static final int GUI_TEXTURE_4_HEIGHT = 225;
    private static final int GUI_TEXTURE_4_ROWS = 4;
    private static final int GUI_TEXTURE_7_HEIGHT = 281;
    private static final int GUI_TEXTURE_7_ROWS = 7;
    private static final int GUI_TEXTURE_9_HEIGHT = 317;
    private static final int GUI_TEXTURE_9_ROWS = 9;
    private static final int TEXTURE_WIDTH = 282;

    private ResourceLocation guiTexture = GUI_TEXTURE_4;
    private int textureHeight = GUI_TEXTURE_4_HEIGHT;

    private SortOrder currentSortOrder;

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        updateGuiTextureSize(width, height);
    }

    @Override
    public void resize(Minecraft minecraft, int winWidth, int winHeight) {
        updateGuiTextureSize(winWidth, winHeight);
        setTopRowAndMoveThumb(topVisibleRow, 0);
        super.resize(minecraft, winWidth, winHeight);
    }

    private void updateGuiTextureSize(int winWidth, int winHeight) {
        // If screen height too small, then provide smaller screen layout (fewer rows)
        width = winWidth;
        height = winHeight;
        imageWidth = TEXTURE_WIDTH - scrollThumbWidth;
        if (winHeight >= GUI_TEXTURE_9_HEIGHT) {
            guiTexture = GUI_TEXTURE_9;
            textureHeight = GUI_TEXTURE_9_HEIGHT;
            containerRows = GUI_TEXTURE_9_ROWS;
        } else if (winHeight >= GUI_TEXTURE_7_HEIGHT) {
            guiTexture = GUI_TEXTURE_7;
            textureHeight = GUI_TEXTURE_7_HEIGHT;
            containerRows = GUI_TEXTURE_7_ROWS;
        } else {
            guiTexture = GUI_TEXTURE_4;
            textureHeight = GUI_TEXTURE_4_HEIGHT;
            containerRows = GUI_TEXTURE_4_ROWS;
        }
        imageHeight = textureHeight;
        containerSlotsMaxZ = containerSlotsMinZ + (Util.SLOT_SIZE * containerRows);
        scrollBarMaxZ = containerSlotsMaxZ;
        containerSlotsHeight = containerSlotsMaxZ - containerSlotsMinX;

        if (guiTexture == GUI_TEXTURE_4) {
            toolSlotsMinZ = containerSlotsMaxZ + 2;
        } else {
            toolSlotsMinZ = containerSlotsMaxZ + 4;
        }
        toolSlotsMaxZ = toolSlotsMinZ + (Util.SLOT_SIZE * TOOL_SLOT_ROWS);
        inventorySlotsMinZ = toolSlotsMaxZ + 15;
        inventorySlotsMaxZ = inventorySlotsMinZ + (Util.SLOT_SIZE * 3);
        hotbarSlotsMinZ = inventorySlotsMaxZ + 4;
        hotbarSlotsMaxZ = hotbarSlotsMinZ + Util.SLOT_SIZE;
        inventoryTextOffset = 11;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        containerExclusionZoneMinX = leftPos;
        containerExclusionZoneMinZ = topPos;
        containerExclusionZoneWidth = TEXTURE_WIDTH - scrollThumbWidth;
        containerExclusionZoneHeight = toolSlotsMaxZ + 12;

        containerExclusionZoneMaxX = containerExclusionZoneMinX + containerExclusionZoneWidth;
        containerExclusionZoneMaxZ = containerExclusionZoneMinZ + containerExclusionZoneHeight;

        inventoryExclusionZoneMinX = leftPos + inventorySlotsMinX - 6;
        inventoryExclusionZoneMinZ = topPos + inventorySlotsMinZ - 6;
        inventoryExclusionZoneWidth = inventorySlotsMaxX - inventorySlotsMinX + 12;
        inventoryExclusionZoneHeight = hotbarSlotsMaxZ - inventorySlotsMinZ + 12;

        inventoryExclusionZoneMaxX = inventoryExclusionZoneMinX + inventoryExclusionZoneWidth;
        inventoryExclusionZoneMaxZ = inventoryExclusionZoneMinZ + inventoryExclusionZoneHeight;
        initializeSlots();
    }

    @Override
    protected void init() {
        super.init();
        isDragging = false;

        currentSortOrder = menu.getSortOrder();
        Button sortOrder = Button.builder(currentSortOrder.getDisplayName(), button -> {
                    currentSortOrder = currentSortOrder.next();
                    button.setMessage(currentSortOrder.getDisplayName());
                    button.setTooltip(Tooltip.create(Component.literal("Sort by ").append(currentSortOrder.name().toUpperCase(Locale.ROOT))));
                    menu.setSortOrder(currentSortOrder);
                })
                .tooltip(Tooltip.create(Component.literal("Sort by ").append(currentSortOrder.name().toUpperCase(Locale.ROOT))))
                .size(16, 12)
                .pos(leftPos + imageWidth - 42, topPos + 4)
                .build();
        addRenderableWidget(sortOrder);
    }

    private void initializeSlots() {
        int index = 0;
        for (int y = 0; y < totalRows; y++) {
            int yOffset = containerSlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            int scrollSlotYOffset = y >= containerRows ? -2000 : yOffset;

            for (int x = 0; x < CONTAINER_COLUMNS; x++) {
                int xOffset = containerSlotsMinX + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getSlot(index);
                slot.x = xOffset;
                slot.y = scrollSlotYOffset;
                index++;
                if (index == itemSlots) break;
            }
        }

        for (int y = 0; y < TOOL_SLOT_ROWS; y++) {
            int yOffset = toolSlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            for (int x = 0; x < TOOL_SLOT_COLUMNS; x++) {
                int xOffset = toolSlotsMinX + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getSlot(index);
                slot.x = xOffset;
                slot.y = yOffset;
                index++;
                if (index == itemSlots + toolSlots) break;
            }
        }

        for (int y = 0; y < UPGRADE_SLOT_ROWS; y++) {
            int yOffset = upgradeSlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            for (int x = 0; x < UPGRADE_SLOT_COLUMNS; x++) {
                int xOffset = upgradeSlotsMinX + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getSlot(index);
                slot.x = xOffset;
                slot.y = yOffset;
                index++;
                if (index == totalSlots) break;
            }
        }

        index = 0;
        for (int y = 0; y < 3; y++) {
            int yOffset = inventorySlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            for (int x = 0; x < 9; x++) {
                int xOffset = inventorySlotsMinX + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getPlayerSlot(index);
                slot.x = xOffset;
                slot.y = yOffset;
                index++;
            }
        }

        index = 0;
        for (int x = 0; x < 9; x++) {
            int xOffset = hotbarSlotsMinX + (x * Util.SLOT_SIZE) + 1;
            Slot slot = menu.getHotbarSlot(index);
            slot.x = xOffset;
            slot.y = hotbarSlotsMinZ + 1;
            index++;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(guiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, textureHeight);

        // Render the scrollbar thumb as either "enabled" if scrolling is required, or "disabled" if not
        int thumbYOffset = (isScrollbarRequired()) ? 0 : 15;
        graphics.blit(guiTexture, leftPos + scrollBarMinX + 1, getScrollThumbY(), scrollThumbMinX, scrollThumbMinZ + thumbYOffset, scrollThumbWidth, scrollThumbHeight, TEXTURE_WIDTH, textureHeight);
    }

    private boolean isScrollbarRequired() {
        return containerRows < GUI_TEXTURE_9_ROWS;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventorySlotsMinX, inventorySlotsMinZ - inventoryTextOffset, 0x404040, false);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        boolean clickedInside = false;
        double containerLeft = containerExclusionZoneMinX;
        double containerRight = containerExclusionZoneMaxX;
        double containerTop = containerExclusionZoneMinZ;
        double containerBottom = containerExclusionZoneMaxZ;

        if (mouseX > containerLeft && mouseX < containerRight && mouseY > containerTop && mouseY < containerBottom) {
            clickedInside = true;
        }

        double inventoryLeft = inventoryExclusionZoneMinX;
        double inventoryRight = inventoryExclusionZoneMaxX;
        double inventoryTop = inventoryExclusionZoneMinZ;
        double inventoryBottom = inventoryExclusionZoneMaxZ;

        if (mouseX > inventoryLeft && mouseX < inventoryRight && mouseY > inventoryTop && mouseY < inventoryBottom) {
            clickedInside = true;
        }

        if (!clickedInside) {
            return true;
        }

        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    private int getScrollThumbY() {
        return topPos + scrollBarMinZ + 1 + scrollThumbY;
    }

    private void updateThumbPosition(double adjustedMouseY) {
        scrollThumbY = (int) Math.min(Math.max(adjustedMouseY, 0), containerSlotsHeight - scrollThumbHeight - 2);
        int row = (int) Math.round(((double) scrollThumbY) / (containerSlotsHeight - scrollThumbHeight - 2) * (totalRows - containerRows));
        this.setTopRow(topVisibleRow, row);
    }

    private void snapThumbToGradation() {
        scrollThumbY = (int) (((double) topVisibleRow / (totalRows - containerRows)) * (containerRows * Util.SLOT_SIZE - 2 - scrollThumbHeight));
    }

    private void setTopRow(int oldTopRow, int newTopRow) {
        if (oldTopRow == newTopRow) return;  // No change in row

        topVisibleRow = newTopRow;  // Update the current top visible row
        boolean atBottom = newTopRow + containerRows >= totalRows; // Check if the new position is at or extends beyond the bottom

        // Base offset for y coordinate
        int yOffsetBase = containerSlotsMinZ + 1;

        // Determine the number of slots to update
        int numSlotsToUpdate = CONTAINER_COLUMNS * containerRows;

        // Calculate starting indexes for slots
        int oldStartIndex = oldTopRow * CONTAINER_COLUMNS;
        int newStartIndex = newTopRow * CONTAINER_COLUMNS;

        // Clear old visible range
        for (int index = oldStartIndex; index < oldStartIndex + numSlotsToUpdate; index++) {
            menu.slots.get(index).y = -2000;
        }

        // New range to set slots
        int newRangeEnd = newStartIndex + numSlotsToUpdate;
        if (atBottom) {
            // Adjust end index if at the bottom to not exceed total slots
            newRangeEnd = Math.min(newRangeEnd, itemSlots);
        }

        // Apply new y-offset to the new range of slots
        for (int index = newStartIndex; index < newRangeEnd; index++) {
            int row = (index / CONTAINER_COLUMNS) - newTopRow;  // Calculate row relative to the new top row
            // Calculate the y-offset for each slot
            menu.slots.get(index).y = yOffsetBase + row * Util.SLOT_SIZE;
        }

    }

    private void setTopRowAndMoveThumb(int oldTopRow, int newTopRow) {
        this.setTopRow(oldTopRow, newTopRow);
        this.snapThumbToGradation();
    }

    private boolean isMouseOverScrollArea(double mouseX, double mouseY) {
        return mouseX >= leftPos + containerSlotsMinX && mouseX <= leftPos + scrollBarMaxX && mouseY >= topPos + containerSlotsMinZ && mouseY <= topPos + scrollBarMaxZ;
    }

    private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
        return mouseX >= leftPos + scrollBarMinX && mouseX <= leftPos + scrollBarMaxX && mouseY >= topPos + scrollBarMinZ && mouseY <= topPos + scrollBarMaxZ;
    }

    private boolean isMouseOverScrollThumb(double mouseX, double mouseY) {
        return mouseX >= leftPos + scrollBarMinX && mouseX <= leftPos + scrollBarMaxX && mouseY >= getScrollThumbY() && mouseY <= getScrollThumbY() + scrollThumbHeight;
    }

    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            ModNetwork.sendToServer(new KeyPressedPacket(Util.BACKPACK_MENU_CTRL, true));
        }
        if (this.handleKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            ModNetwork.sendToServer(new KeyPressedPacket(Util.BACKPACK_MENU_CTRL, false));
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (topVisibleRow != totalRows - containerRows) {
                if (hasShiftDown()) {
                    this.setTopRowAndMoveThumb(topVisibleRow, Math.min(topVisibleRow + containerRows, totalRows - containerRows));
                } else {
                    this.setTopRowAndMoveThumb(topVisibleRow, topVisibleRow + 1);
                }
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            if (topVisibleRow != 0) {
                if (hasShiftDown()) {
                    this.setTopRowAndMoveThumb(topVisibleRow, Math.max(topVisibleRow - containerRows, 0));
                } else {
                    this.setTopRowAndMoveThumb(topVisibleRow, topVisibleRow - 1);
                }
            }
            return true;
        } else if (TOGGLE_BACKPACK_KEY.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            KeybindHandler.handleOpenCloseBackpack();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOverScrollThumb(mouseX, mouseY) && button == 0 && isScrollbarRequired()) {
            scrollYOffset = (int) mouseY - scrollThumbY;
            isDragging = true;
        } else if (this.isMouseOverScrollBar(mouseX, mouseY) && button == 0 && isScrollbarRequired()) {
            this.updateThumbPosition(mouseY - Util.CONTAINER_HEADER_HEIGHT - 1 - topPos);
            this.snapThumbToGradation();
        }

        menu.container.setPlayerInteraction(true); // Needed for QUICK_CRAFT on BlockEntity
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            this.updateThumbPosition(mouseY - scrollYOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            this.snapThumbToGradation();
            return true;
        }
        menu.container.setPlayerInteraction(false); // Needed for QUICK_CRAFT on BlockEntity
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaY)) return true;

        // Return false if scrollbar is not required
        if (!isScrollbarRequired()) return false;

        if (this.isMouseOverScrollArea(mouseX, mouseY)) {
            int newTop;
            if (deltaY < 0) {
                newTop = Math.min(topVisibleRow + (hasShiftDown() ? containerRows : 1), totalRows - containerRows);
            } else {
                newTop = Math.max(topVisibleRow - (hasShiftDown() ? containerRows : 1), 0);
            }
            this.setTopRowAndMoveThumb(topVisibleRow, newTop);
            return true;
        }
        return false;
    }

    @NotNull
    @ApiStatus.OverrideOnly
    public List<Rect2i> getExclusionZones() {
        return Arrays.asList(
                new Rect2i(containerExclusionZoneMinX, containerExclusionZoneMinZ, containerExclusionZoneWidth, containerExclusionZoneHeight),
                new Rect2i(inventoryExclusionZoneMinX, inventoryExclusionZoneMinZ, inventoryExclusionZoneWidth, inventoryExclusionZoneHeight)
        );
    }

    @Override
    protected void renderSlot(GuiGraphics pGuiGraphics, Slot pSlot) {
        if (pSlot.index > Util.ITEM_SLOT_END_RANGE) {
            super.renderSlot(pGuiGraphics, pSlot);
        } else {
            int x = pSlot.x;
            int y = pSlot.y;
            ItemStack itemStack = pSlot.getItem();
            pGuiGraphics.renderItem(itemStack, x, y, pSlot.x + pSlot.y * this.imageWidth);
            if (!itemStack.isEmpty()) {
                if (itemStack.getCount() != 1) {
                    String countText = String.valueOf(itemStack.getCount());

                    PoseStack poseStack = pGuiGraphics.pose();
                    poseStack.pushPose();
                    poseStack.translate(0.0D, 0.0D, 200.0F);
                    float scale = Math.min(1.0F, 16.0F / font.width(countText));
                    if (scale < 1.0F) {
                        poseStack.scale(scale, scale, 1.0F);
                    }
                    pGuiGraphics.drawString(font, countText, (x + 19 - 2 - (font.width(countText) * scale)) / scale, (y + 6 + 3 + (1 / (scale * scale) - 1)) / scale, 16777215, true);
                    poseStack.popPose();
                }
                // Render item status bar
                if (itemStack.isBarVisible()) {
                    int l = itemStack.getBarWidth();
                    int i = itemStack.getBarColor();
                    int j = x + 2;
                    int k = y + 13;
                    pGuiGraphics.fill(RenderType.guiOverlay(), j, k, j + 13, k + 2, -16777216);
                    pGuiGraphics.fill(RenderType.guiOverlay(), j, k, j + l, k + 1, i | -16777216);
                }
            }
        }

    }

}
