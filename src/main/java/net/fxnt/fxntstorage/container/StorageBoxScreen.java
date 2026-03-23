package net.fxnt.fxntstorage.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StorageBoxScreen extends AbstractContainerScreen<StorageBoxMenu> {
    private final int CONTAINER_SLOTS = menu.getContainerSize();
    private final int CONTAINER_COLS = Util.SLOTS_PER_ROW;
    private int CONTAINER_ROWS = 5;
    private final int totalRows = (int) Math.ceil((double) CONTAINER_SLOTS / CONTAINER_COLS);
    private final int containerSlotsMinX = 29;
    private final int containerSlotsMaxX = containerSlotsMinX + (Util.SLOT_SIZE * CONTAINER_COLS);
    private final int containerSlotsMinZ = 17;
    private int containerSlotsMaxZ = containerSlotsMinX + (Util.SLOT_SIZE * CONTAINER_ROWS);
    private int containerSlotsHeight = containerSlotsMaxZ - containerSlotsMinZ;
    private final int scrollBarMinX = containerSlotsMaxX + 4;
    private final int scrollBarMaxX = scrollBarMinX + 14;
    private final int scrollBarMinZ = containerSlotsMinZ;
    private int scrollBarMaxZ = containerSlotsMaxZ;

    private final int inventorySlotsMinX = 60;
    private final int inventorySlotsMaxX = inventorySlotsMinX + (Util.SLOT_SIZE * 9);
    private int inventorySlotsMinZ = containerSlotsMaxZ + 15;
    private int inventorySlotsMaxZ = inventorySlotsMinZ + (Util.SLOT_SIZE * 3);
    private final int hotbarSlotsMinX = inventorySlotsMinX;
    private int hotbarSlotsMinZ = inventorySlotsMaxZ + 4;
    private int hotbarSlotsMaxZ = hotbarSlotsMinZ + Util.SLOT_SIZE;
    private final int scrollThumbMinX = 270;
    private final int scrollThumbMaxX = scrollThumbMinX + 12;
    private final int scrollThumbMinZ = 0;
    private final int scrollThumbMaxZ = 15;
    private int scrollThumbY = 0;
    private int topVisibleRow, scrollYOffset;
    private final int scrollThumbWidth = scrollThumbMaxX - scrollThumbMinX;
    private final int scrollThumbHeight = scrollThumbMaxZ - scrollThumbMinZ;
    private boolean isDragging;
    private int inventoryTextOffset = 11;

    private int containerExclusionZoneMinX, containerExclusionZoneMaxX, containerExclusionZoneMinZ, containerExclusionZoneMaxZ, containerExclusionZoneWidth, containerExclusionZoneHeight;
    private int inventoryExclusionZoneMinX, inventoryExclusionZoneMaxX, inventoryExclusionZoneMinZ, inventoryExclusionZoneMaxZ, inventoryExclusionZoneWidth, inventoryExclusionZoneHeight;

    // Textures
    private static final ResourceLocation GUI_TEXTURE_4 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/storage_box_screen_4.png");
    private static final ResourceLocation GUI_TEXTURE_5 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/storage_box_screen_5.png");
    private static final ResourceLocation GUI_TEXTURE_7 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/storage_box_screen_7.png");
    private static final ResourceLocation GUI_TEXTURE_9 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/storage_box_screen_9.png");
    private static final ResourceLocation GUI_TEXTURE_11 = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/storage_box_screen_11.png");

    private final int guiTexture4Height = 187;
    private final int guiTexture4Rows = 4;
    private final int guiTexture5Height = 205;
    private final int guiTexture5Rows = 5;
    private final int guiTexture7Height = 241;
    private final int guiTexture7Rows = 7;
    private final int guiTexture9Height = 277;
    private final int guiTexture9Rows = 9;
    private final int guiTexture11Height = 313;
    private final int guiTexture11Rows = 11;
    private ResourceLocation guiTexture = GUI_TEXTURE_5;
    private int textureHeight = guiTexture5Height;
    private final int textureWidth = 282;

    private boolean atBottom = false;

    private SortOrder currentSortOrder;

    public static @NotNull StorageBoxScreen createScreen(StorageBoxMenu menu, Inventory playerInventory, Component title) {
        return new StorageBoxScreen(menu, playerInventory, title);
    }

    public StorageBoxScreen(StorageBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        updateGuiTextureSize(width, height);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int winWidth, int winHeight) {
        updateGuiTextureSize(winWidth, winHeight);
        setTopRowAndMoveThumb(topVisibleRow, 0);
        super.resize(minecraft, winWidth, winHeight);
    }

    private void updateGuiTextureSize(int winWidth, int winHeight) {
        // If screen height too small, then provide smaller screen layout (fewer rows)
        width = winWidth;
        height = winHeight;

        imageWidth = textureWidth - scrollThumbWidth;

        if (winHeight >= guiTexture11Height && this.CONTAINER_SLOTS >= 132) {
            guiTexture = GUI_TEXTURE_11;
            textureHeight = guiTexture11Height;
            CONTAINER_ROWS = guiTexture11Rows;
        } else if (winHeight >= guiTexture9Height && this.CONTAINER_SLOTS >= 108) {
            guiTexture = GUI_TEXTURE_9;
            textureHeight = guiTexture9Height;
            CONTAINER_ROWS = guiTexture9Rows;
        } else if (winHeight >= guiTexture7Height && this.CONTAINER_SLOTS >= 84) {
            guiTexture = GUI_TEXTURE_7;
            textureHeight = guiTexture7Height;
            CONTAINER_ROWS = guiTexture7Rows;
        } else if (winHeight >= guiTexture5Height && this.CONTAINER_SLOTS >= 60) {
            guiTexture = GUI_TEXTURE_5;
            textureHeight = guiTexture5Height;
            CONTAINER_ROWS = guiTexture5Rows;
        } else {
            guiTexture = GUI_TEXTURE_4;
            textureHeight = guiTexture4Height;
            CONTAINER_ROWS = guiTexture4Rows;
        }
        imageHeight = textureHeight;

        containerSlotsMaxZ = containerSlotsMinZ + (Util.SLOT_SIZE * CONTAINER_ROWS);
        scrollBarMaxZ = containerSlotsMaxZ;
        containerSlotsHeight = containerSlotsMaxZ - containerSlotsMinX;
        inventorySlotsMinZ = containerSlotsMaxZ + 15;
        inventorySlotsMaxZ = inventorySlotsMinZ + (Util.SLOT_SIZE * 3);
        hotbarSlotsMinZ = inventorySlotsMaxZ + 4;
        hotbarSlotsMaxZ = hotbarSlotsMinZ + Util.SLOT_SIZE;
        inventoryTextOffset = 11;

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        containerExclusionZoneMinX = leftPos + 22;
        containerExclusionZoneMinZ = topPos;
        containerExclusionZoneWidth = textureWidth - scrollThumbWidth - 22;
        containerExclusionZoneHeight = containerSlotsMaxZ + 12;

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
                    button.setTooltip(Tooltip.create(Component.translatable("tooltip.fxntstorage.sortBy").append(Component.literal(" ")).append(currentSortOrder.name().toUpperCase(Locale.ROOT))));
                    menu.setSortOrder(currentSortOrder);
                })
                .tooltip(Tooltip.create(Component.translatable("tooltip.fxntstorage.sortBy").append(Component.literal(" ")).append(currentSortOrder.name().toUpperCase(Locale.ROOT))))
                .size(16, 12)
                .pos(leftPos + imageWidth - 42, topPos + 4)
                .build();
        addRenderableWidget(sortOrder);
    }

    private void initializeSlots() {
        int index = 0;
        for (int y = 0; y < totalRows; y++) {
            int yOffset = containerSlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            int scrollSlotYOffset = y >= CONTAINER_ROWS ? -2000 : yOffset;

            for (int x = 0; x < CONTAINER_COLS; x++) {
                int xOffset = containerSlotsMinX + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getSlot(index);
                slot.x = xOffset;
                slot.y = scrollSlotYOffset;
                index++;
                if (index == CONTAINER_SLOTS) break;
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(guiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, textureWidth, textureHeight);

        // Render the scrollbar thumb as either "enabled" if scrolling is required, or "disabled" if not
        int thumbYOffset = isScrollbarRequired() ? 0 : 15;
        graphics.blit(guiTexture, leftPos + scrollBarMinX + 1, getScrollThumbY(), scrollThumbMinX, scrollThumbMinZ + thumbYOffset, scrollThumbWidth, scrollThumbHeight, textureWidth, textureHeight);

    }

    private boolean isScrollbarRequired() {
        return CONTAINER_ROWS < totalRows;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 30, 6, 0x404040, false);
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
        int row = (int) Math.round(((double) scrollThumbY) / (containerSlotsHeight - scrollThumbHeight - 2) * (totalRows - CONTAINER_ROWS));
        this.setTopRow(topVisibleRow, row);
    }

    private void snapThumbToGradation() {
        scrollThumbY = (int) (((double) topVisibleRow / (totalRows - CONTAINER_ROWS)) * (CONTAINER_ROWS * Util.SLOT_SIZE - 2 - scrollThumbHeight));
    }

    private void setTopRow(int oldTopRow, int newTopRow) {
        if (oldTopRow == newTopRow) return;  // No change in row

        topVisibleRow = newTopRow;  // Update the current top visible row
        atBottom = newTopRow + CONTAINER_ROWS >= totalRows; // Check if the new position is at or extends beyond the bottom

        // Base offset for y coordinate
        int yOffsetBase = containerSlotsMinZ + 1;

        // Determine the number of slots to update
        int numSlotsToUpdate = CONTAINER_COLS * CONTAINER_ROWS;

        // Calculate starting indexes for slots
        int oldStartIndex = oldTopRow * CONTAINER_COLS;
        int newStartIndex = newTopRow * CONTAINER_COLS;

        // Clear old visible range
        for (int index = oldStartIndex; index < oldStartIndex + numSlotsToUpdate; index++) {
            menu.slots.get(index).y = -2000;
        }

        // New range to set slots
        int newRangeEnd = newStartIndex + numSlotsToUpdate;
        if (atBottom) {
            // Adjust end index if at the bottom to not exceed total slots
            newRangeEnd = Math.min(newRangeEnd, CONTAINER_SLOTS);
        }

        // Apply new y-offset to the new range of slots
        for (int index = newStartIndex; index < newRangeEnd; index++) {
            int row = (index / CONTAINER_COLS) - newTopRow;  // Calculate row relative to the new top row
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOverScrollThumb(mouseX, mouseY) && button == 0 && isScrollbarRequired()) {
            scrollYOffset = (int) mouseY - scrollThumbY;
            isDragging = true;
        } else if (this.isMouseOverScrollBar(mouseX, mouseY) && button == 0 && isScrollbarRequired()) {
            this.updateThumbPosition(mouseY - Util.CONTAINER_HEADER_HEIGHT - 1 - topPos);
            this.snapThumbToGradation();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(@NotNull Slot pSlot, int pSlotId, int pMouseButton, @NotNull ClickType pType) {
        super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
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
                newTop = Math.min(topVisibleRow + (hasShiftDown() ? CONTAINER_ROWS : 1), totalRows - CONTAINER_ROWS);
            } else {
                newTop = Math.max(topVisibleRow - (hasShiftDown() ? CONTAINER_ROWS : 1), 0);
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
}
