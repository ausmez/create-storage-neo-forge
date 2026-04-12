package net.fxnt.fxntstorage.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Locale;

public abstract class AbstractStorageBoxScreen<M extends AbstractContainerMenu & ISortableStorageBox>
        extends AbstractContainerScreen<M> {

    private record TextureConfig(ResourceLocation texture, int height, int minSlots, int rows) {
        static TextureConfig of(String name, int height, int minSlots, int rows) {
            return new TextureConfig(
                    ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/gui/container/" + name),
                    height, minSlots, rows);
        }
    }

    // Ordered largest-first so selectTexture() returns the best fit on first match
    private static final TextureConfig[] TEXTURE_CONFIGS = {
            TextureConfig.of("storage_box_screen_11.png", 313, 132, 11),
            TextureConfig.of("storage_box_screen_9.png", 277, 108, 9),
            TextureConfig.of("storage_box_screen_7.png", 241, 84, 7),
            TextureConfig.of("storage_box_screen_5.png", 205, 60, 5),
            TextureConfig.of("storage_box_screen_4.png", 187, 0, 4), // smallest / fallback
    };

    // Layout constants (pixel offsets within the GUI texture; never change)
    private static final int TEXTURE_WIDTH = 282;
    private static final int CONTAINER_MIN_X = 29;
    private static final int CONTAINER_MIN_Y = 17;
    private static final int SCROLL_BAR_MIN_X = CONTAINER_MIN_X + (Util.SLOT_SIZE * Util.SLOTS_PER_ROW) + 4;
    private static final int SCROLL_BAR_MAX_X = SCROLL_BAR_MIN_X + 14;
    private static final int INVENTORY_MIN_X = 60;
    private static final int INVENTORY_GAP = 15;  // gap between container slots and player inventory
    private static final int HOTBAR_GAP = 4;      // gap between player inventory and hotbar
    private static final int SCROLL_THUMB_TEX_X = 270;
    private static final int SCROLL_THUMB_TEX_Y = 0;
    private static final int SCROLL_THUMB_WIDTH = 12;
    private static final int SCROLL_THUMB_HEIGHT = 15;
    private static final int INVENTORY_LABEL_OFFSET = 11;

    private final int CONTAINER_SLOTS = menu.getContainerSize();
    private final int totalRows = (int) Math.ceil((double) CONTAINER_SLOTS / Util.SLOTS_PER_ROW);
    private int CONTAINER_ROWS;

    // Y-axis layout (recalculated on resize)
    private int containerSlotsMaxY;
    private int containerSlotsHeight;
    private int inventorySlotsMinY;
    private int hotbarSlotsMinY;

    // Exclusion zones for JEI/EMI compatibility
    private Rect2i containerExclusionZone;
    private Rect2i inventoryExclusionZone;

    private ResourceLocation guiTexture;
    private int textureHeight;

    private int scrollThumbY = 0;
    private int topVisibleRow;
    private int scrollYOffset;
    private boolean isDragging;
    private SortOrder currentSortOrder;

    protected AbstractStorageBoxScreen(M menu, Inventory playerInventory, Component title) {
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
        width = winWidth;
        height = winHeight;
        imageWidth = TEXTURE_WIDTH - SCROLL_THUMB_WIDTH;

        TextureConfig config = selectTexture(winHeight);
        guiTexture = config.texture();
        textureHeight = config.height();
        CONTAINER_ROWS = config.rows();
        imageHeight = textureHeight;

        containerSlotsMaxY = CONTAINER_MIN_Y + (Util.SLOT_SIZE * CONTAINER_ROWS);
        containerSlotsHeight = containerSlotsMaxY - CONTAINER_MIN_Y;
        inventorySlotsMinY = containerSlotsMaxY + INVENTORY_GAP;
        hotbarSlotsMinY = inventorySlotsMinY + (Util.SLOT_SIZE * 3) + HOTBAR_GAP;

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        containerExclusionZone = new Rect2i(
                leftPos + 22,
                topPos,
                TEXTURE_WIDTH - SCROLL_THUMB_WIDTH - 22,
                containerSlotsMaxY + 12);

        inventoryExclusionZone = new Rect2i(
                leftPos + INVENTORY_MIN_X - 6,
                topPos + inventorySlotsMinY - 6,
                Util.SLOT_SIZE * 9 + 12,
                Util.SLOT_SIZE * 4 + HOTBAR_GAP + 12);

        initializeSlots();
    }

    private TextureConfig selectTexture(int winHeight) {
        for (TextureConfig config : TEXTURE_CONFIGS) {
            if (winHeight >= config.height() && CONTAINER_SLOTS >= config.minSlots()) {
                return config;
            }
        }
        return TEXTURE_CONFIGS[TEXTURE_CONFIGS.length - 1];
    }

    @Override
    protected void init() {
        super.init();
        isDragging = false;

        currentSortOrder = menu.getSortOrder();
        Button sortOrder = Button.builder(currentSortOrder.getDisplayName(), button -> {
                    currentSortOrder = currentSortOrder.next();
                    button.setMessage(currentSortOrder.getDisplayName());
                    button.setTooltip(createSortTooltip());
                    menu.setSortOrder(currentSortOrder);
                })
                .tooltip(createSortTooltip())
                .size(16, 12)
                .pos(leftPos + imageWidth - 42, topPos + 4)
                .build();
        addRenderableWidget(sortOrder);
    }

    private Tooltip createSortTooltip() {
        return Tooltip.create(
                Component.translatable("tooltip.fxntstorage.sortBy")
                        .append(Component.literal(" "))
                        .append(currentSortOrder.name().toUpperCase(Locale.ROOT))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("tooltip.fxntstorage.sortBy.text").withStyle(ChatFormatting.DARK_GRAY))
        );
    }

    private void initializeSlots() {
        int index = 0;
        for (int y = 0; y < totalRows; y++) {
            int yOffset = CONTAINER_MIN_Y + (y * Util.SLOT_SIZE) + 1;
            int scrollSlotY = y >= CONTAINER_ROWS ? -2000 : yOffset;

            for (int x = 0; x < Util.SLOTS_PER_ROW; x++) {
                int xOffset = CONTAINER_MIN_X + (x * Util.SLOT_SIZE) + 1;
                Slot slot = menu.getSlot(index);
                slot.x = xOffset;
                slot.y = scrollSlotY;
                index++;
                if (index == CONTAINER_SLOTS) break;
            }
        }

        index = 0;
        for (int y = 0; y < 3; y++) {
            int yOffset = inventorySlotsMinY + (y * Util.SLOT_SIZE) + 1;
            for (int x = 0; x < 9; x++) {
                Slot slot = menu.getPlayerSlot(index);
                slot.x = INVENTORY_MIN_X + (x * Util.SLOT_SIZE) + 1;
                slot.y = yOffset;
                index++;
            }
        }

        for (int x = 0; x < 9; x++) {
            Slot slot = menu.getHotbarSlot(x);
            slot.x = INVENTORY_MIN_X + (x * Util.SLOT_SIZE) + 1;
            slot.y = hotbarSlotsMinY + 1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(guiTexture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, textureHeight);

        int thumbTexY = isScrollbarRequired() ? SCROLL_THUMB_TEX_Y : SCROLL_THUMB_TEX_Y + SCROLL_THUMB_HEIGHT;
        graphics.blit(guiTexture, leftPos + SCROLL_BAR_MIN_X + 1, getScrollThumbY(),
                SCROLL_THUMB_TEX_X, thumbTexY, SCROLL_THUMB_WIDTH, SCROLL_THUMB_HEIGHT, TEXTURE_WIDTH, textureHeight);
    }

    private boolean isScrollbarRequired() {
        return CONTAINER_ROWS < totalRows;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 30, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, INVENTORY_MIN_X, inventorySlotsMinY - INVENTORY_LABEL_OFFSET, 0x404040, false);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (isInsideZone(mouseX, mouseY, containerExclusionZone) || isInsideZone(mouseX, mouseY, inventoryExclusionZone)) {
            return super.hasClickedOutside(mouseX, mouseY, left, top, button);
        }
        return true;
    }

    private static boolean isInsideZone(double mouseX, double mouseY, Rect2i zone) {
        return mouseX > zone.getX() && mouseX < zone.getX() + zone.getWidth()
                && mouseY > zone.getY() && mouseY < zone.getY() + zone.getHeight();
    }

    private int getScrollThumbY() {
        return topPos + CONTAINER_MIN_Y + 1 + scrollThumbY;
    }

    private void updateThumbPosition(double adjustedMouseY) {
        scrollThumbY = (int) Math.min(Math.max(adjustedMouseY, 0), containerSlotsHeight - SCROLL_THUMB_HEIGHT - 2);
        int row = (int) Math.round(((double) scrollThumbY) / (containerSlotsHeight - SCROLL_THUMB_HEIGHT - 2) * (totalRows - CONTAINER_ROWS));
        this.setTopRow(topVisibleRow, row);
    }

    private void snapThumbToGradation() {
        scrollThumbY = (int) (((double) topVisibleRow / (totalRows - CONTAINER_ROWS)) * (CONTAINER_ROWS * Util.SLOT_SIZE - 2 - SCROLL_THUMB_HEIGHT));
    }

    private void setTopRow(int oldTopRow, int newTopRow) {
        if (oldTopRow == newTopRow) return;

        topVisibleRow = newTopRow;
        boolean atBottom = newTopRow + CONTAINER_ROWS >= totalRows;

        int numSlotsToUpdate = Util.SLOTS_PER_ROW * CONTAINER_ROWS;
        int oldStartIndex = oldTopRow * Util.SLOTS_PER_ROW;
        int newStartIndex = newTopRow * Util.SLOTS_PER_ROW;

        for (int index = oldStartIndex; index < oldStartIndex + numSlotsToUpdate; index++) {
            menu.slots.get(index).y = -2000;
        }

        int newRangeEnd = newStartIndex + numSlotsToUpdate;
        if (atBottom) {
            newRangeEnd = Math.min(newRangeEnd, CONTAINER_SLOTS);
        }

        for (int index = newStartIndex; index < newRangeEnd; index++) {
            int row = (index / Util.SLOTS_PER_ROW) - newTopRow;
            menu.slots.get(index).y = CONTAINER_MIN_Y + 1 + row * Util.SLOT_SIZE;
        }
    }

    private void setTopRowAndMoveThumb(int oldTopRow, int newTopRow) {
        this.setTopRow(oldTopRow, newTopRow);
        this.snapThumbToGradation();
    }

    private boolean isMouseOverScrollArea(double mouseX, double mouseY) {
        return mouseX >= leftPos + CONTAINER_MIN_X && mouseX <= leftPos + SCROLL_BAR_MAX_X
                && mouseY >= topPos + CONTAINER_MIN_Y && mouseY <= topPos + containerSlotsMaxY;
    }

    private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLL_BAR_MIN_X && mouseX <= leftPos + SCROLL_BAR_MAX_X
                && mouseY >= topPos + CONTAINER_MIN_Y && mouseY <= topPos + containerSlotsMaxY;
    }

    private boolean isMouseOverScrollThumb(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLL_BAR_MIN_X && mouseX <= leftPos + SCROLL_BAR_MAX_X
                && mouseY >= getScrollThumbY() && mouseY <= getScrollThumbY() + SCROLL_THUMB_HEIGHT;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!isScrollbarRequired()) return false;

        if (this.isMouseOverScrollArea(mouseX, mouseY)) {
            int newTop;
            if (scrollY < 0) {
                newTop = Math.min(topVisibleRow + (hasShiftDown() ? CONTAINER_ROWS : 1), totalRows - CONTAINER_ROWS);
            } else {
                newTop = Math.max(topVisibleRow - (hasShiftDown() ? CONTAINER_ROWS : 1), 0);
            }
            this.setTopRowAndMoveThumb(topVisibleRow, newTop);
            return true;
        }
        return false;
    }

    @ApiStatus.OverrideOnly
    public List<Rect2i> getExclusionZones() {
        return List.of(containerExclusionZone, inventoryExclusionZone);
    }
}