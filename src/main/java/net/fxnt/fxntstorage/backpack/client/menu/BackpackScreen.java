package net.fxnt.fxntstorage.backpack.client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.slot.UpgradeSlot;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopRecipeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopUpgrade;
import net.fxnt.fxntstorage.compat.emi.EMICompat;
import net.fxnt.fxntstorage.compat.rei.REICompat;
import net.fxnt.fxntstorage.network.packet.KeyPressedPacket;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;
import static net.fxnt.fxntstorage.util.KeybindHandler.TOGGLE_BACKPACK_KEY;

@ParametersAreNonnullByDefault
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int CONTAINER_COLUMNS = 12;
    private static final int TOOL_SLOT_COLUMNS = 12;
    private static final int TOOL_SLOT_ROWS = 2;
    private static final int UPGRADE_SLOT_COLUMNS = 1;
    private static final int UPGRADE_SLOT_ROWS = 6;
    private static final int TEXTURE_WIDTH = 282;

    // Panel dimensions
    private static final int PANEL_EXPANDED_HEIGHT = 47;
    private static final int PANEL_COLLAPSED_HEIGHT = 32;
    private static final int PANEL_TAB_WIDTH = 18;
    private static final int PANEL_TAB_HEIGHT = 20;
    private static final int PANEL_TAB_SPACING = 4;

    // Slot positions
    private static final int CONTAINER_SLOTS_MIN_X = 29;
    private static final int CONTAINER_SLOTS_MIN_Z = 17;
    private static final int UPGRADE_SLOTS_MIN_X = 7;
    private static final int PLAYER_INVENTORY_SLOTS_MIN_X = 60;

    // Scrollbar dimensions
    private static final int SCROLL_THUMB_WIDTH = 12;
    private static final int SCROLL_THUMB_HEIGHT = 15;
    private static final int SCROLL_BAR_OFFSET_X = 4;
    private static final int SCROLL_BAR_WIDTH = 14;

    // Texture coordinates for scroll thumb
    private static final int SCROLL_THUMB_TEX_MIN_X = 270;
    private static final int SCROLL_THUMB_TEX_MIN_Z = 0;
    private static final int SCROLL_THUMB_TEX_HEIGHT_OFFSET = 15;

    // GUI Texture configurations
    private static final ResourceLocation GUI_TEXTURE_4 = modLoc("textures/gui/container/backpack_screen_4.png");
    private static final ResourceLocation GUI_TEXTURE_7 = modLoc("textures/gui/container/backpack_screen_7.png");
    private static final ResourceLocation GUI_TEXTURE_9 = modLoc("textures/gui/container/backpack_screen_9.png");
    private static final ResourceLocation PANEL_TEXTURE = modLoc("textures/gui/atlas.png");

    private static final GuiTextureConfig GUI_CONFIG_4 = new GuiTextureConfig(GUI_TEXTURE_4, 225, 4);
    private static final GuiTextureConfig GUI_CONFIG_7 = new GuiTextureConfig(GUI_TEXTURE_7, 281, 7);
    private static final GuiTextureConfig GUI_CONFIG_9 = new GuiTextureConfig(GUI_TEXTURE_9, 317, 9);

    // Menu-derived values
    private final int itemSlots;
    private final int toolSlots;
    private final int totalSlots;
    private final int totalRows;

    // Dynamic layout values (updated on resize)
    private int containerRows = 5;
    private GuiTextureConfig currentGuiConfig = GUI_CONFIG_4;

    // Calculated positions
    private final LayoutPositions layout = new LayoutPositions();

    // Scrolling state
    private boolean isDragging;
    private int scrollThumbY = 0;
    private int scrollYOffset;
    private int topVisibleRow = 0;

    // Panel state
    private final List<PanelState> upgradePanels = new ArrayList<>();

    // Sort order
    private SortOrder currentSortOrder;

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.itemSlots = menu.getItemSlotCount();
        this.toolSlots = menu.getToolSlotCount();
        this.totalSlots = menu.getTotalSlotCount();
        this.totalRows = (int) Math.ceil((double) itemSlots / CONTAINER_COLUMNS);

        Minecraft mc = Minecraft.getInstance();
        updateGuiTextureSize(mc.getWindow().getGuiScaledHeight());
    }

    @Override
    protected void init() {
        super.init();
        isDragging = false;
        currentSortOrder = menu.getSortOrder();

        menu.setUpgradeSlotListener(this::rebuildUpgradePanels);

        addSortOrderButton();
        rebuildUpgradePanels();
    }

    private static class IconButton extends Button {
        private final ItemStack stack;
        private final float scale;

        private IconButton(int x, int y, int width, int height, Component message,
                           OnPress onPress, ItemStack stack, float scale) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
            this.stack = stack;
            this.scale = scale;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(getX(), getY(), 0);
            pose.scale(scale, scale, 1.0f);
            guiGraphics.renderItem(stack,
                    (int) ((width / 2f - 8) / scale),
                    (int) ((height / 2f - 8) / scale));
            pose.popPose();
        }
    }

    private static class PanelState {
        final int index;
        final String id;
        final UpgradeType type;
        final UpgradePanel panel;
        IconButton tabButton;
        int tabY;
        int relativeTabY;
        boolean expanded;

        PanelState(int index, UpgradeType type, UpgradePanel panel) {
            this.index = index;
            this.type = type;
            this.id = type.getId();
            this.panel = panel;
            this.expanded = false;
        }
    }

    private void addSortOrderButton() {
        Button sortOrder = Button.builder(currentSortOrder.getDisplayName(), this::handleSortOrderClick)
                .tooltip(createSortTooltip())
                .size(16, 12)
                .pos(leftPos + imageWidth - 42, topPos + 4)
                .build();
        addRenderableWidget(sortOrder);
    }

    private void handleSortOrderClick(Button button) {
        currentSortOrder = currentSortOrder.next();
        button.setMessage(currentSortOrder.getDisplayName());
        button.setTooltip(createSortTooltip());
        menu.setSortOrder(currentSortOrder);
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

    protected void rebuildUpgradePanels() {
        for (PanelState panel : upgradePanels) {
            if (panel.expanded) removeWidgets(panel.panel.getWidgets());
            removeWidget(panel.tabButton);
        }
        upgradePanels.clear();

        for (UpgradeType upgradeType : UpgradeType.values()) {
            if (!upgradeType.hasPanel()) continue;
            if (!menu.container.isPanelExpanded(upgradeType)) continue;
            // Check if this upgrade is actually installed
            if (!menu.hasUpgrade(upgradeType)) {
                menu.container.clearPanelExpanded(upgradeType);
            }
        }

        UpgradeContext context = UpgradeContext.forMenu(
                menu, menu.player, menu.container.getItemHandler(), menu.getBackpackType(), menu.blockPos
        );

        int panelIndex = 0;
        for (int i : menu.layout.upgrades().range()) {
            ItemStack upgradeItem = menu.getSlot(i).getItem();
            UpgradeType upgradeType = UpgradeType.fromItem(upgradeItem.getItem());

            if (upgradeItem.isEmpty() || upgradeType == null || !upgradeType.hasPanel()
                    || upgradeType.isPlayerOnly() && menu.getBackpackType().equals(BackpackMenu.BackpackType.CONTRAPTION))
                continue;

            IUpgrade upgrade = UpgradeRegistry.get(upgradeType);
            if (upgrade == null) continue;

            UpgradePanel upgradePanel = upgrade.createPanel(context);
            if (upgradePanel == null) continue;

            PanelState state = new PanelState(panelIndex, upgradeType, upgradePanel);
            state.relativeTabY = 11 + (panelIndex * (PANEL_TAB_HEIGHT + PANEL_TAB_SPACING));
            state.tabY = topPos + state.relativeTabY;

            state.tabButton = new IconButton(
                    leftPos + imageWidth, state.tabY,
                    PANEL_TAB_WIDTH, PANEL_TAB_HEIGHT,
                    Component.translatable("tooltip.fxntstorage.upgrade_tab." + upgradeType.getId()),
                    button -> togglePanel(state),
                    upgradeType.getActiveStack(),
                    1.0f
            );

            upgradePanels.add(state);
            addRenderableWidget(state.tabButton);

            // Restore expanded state
            if (menu.isPanelExpanded(upgradeType)) {
                restoreExpandedPanel(state);
            }

            panelIndex++;
        }

        repositionTabs();
    }

    private void togglePanel(PanelState state) {
        if (state.expanded) {
            collapsePanel(state);
        } else {
            expandPanel(state);
        }
        menu.container.saveSettings();

        if (FXNTStorage.EMI_LOADED) {
            EMICompat.onCraftingPanelToggled();
        }
        if (FXNTStorage.REI_LOADED) {
            REICompat.onCraftingPanelToggled();
        }
    }

    private void expandPanel(PanelState state) {
        for (PanelState other : upgradePanels) {
            if (other != state && other.expanded) {
                collapsePanelVisual(other);
            }
        }
        restoreExpandedPanel(state);
        menu.togglePanelExpanded(state.type);
    }

    private void restoreExpandedPanel(PanelState state) {
        state.expanded = true;
        state.tabButton.setTooltip(null);

        repositionTabs();

        setPanelPosition(state);

        removeWidgets(state.panel.getWidgets());
        state.panel.clearWidgets();

        state.panel.createWidgets(widget -> {
            if (widget instanceof AbstractWidget w) {
                addRenderableWidget(w);
            }
        });

        state.panel.tick();

        updatePanelSlotPosition(state);
    }

    private void setPanelPosition(PanelState state) {
        state.panel.setPanelPosition(leftPos, imageWidth, state.tabY);
    }

    private void collapsePanel(PanelState state) {
        collapsePanelVisual(state);
        menu.togglePanelExpanded(state.type);
    }

    private void collapsePanelVisual(PanelState state) {
        state.expanded = false;
        state.tabButton.setTooltip(null);
        removeWidgets(state.panel.getWidgets());

        repositionTabs();
    }

    private void repositionTabs() {
        int visibleIndex = 0;

        for (PanelState panel : upgradePanels) {
            int finalTabY = topPos + 5 + (visibleIndex * (PANEL_TAB_HEIGHT + PANEL_TAB_SPACING));

            for (PanelState other : upgradePanels) {
                if (other.index < panel.index && other.expanded) {
                    finalTabY += 8 + other.panel.getExpandedHeight() - PANEL_COLLAPSED_HEIGHT;
                }
            }

            panel.tabButton.setPosition(leftPos + imageWidth, finalTabY);
            panel.tabButton.visible = true;
            panel.tabY = finalTabY;
            panel.relativeTabY = finalTabY - topPos;

            visibleIndex++;
            setPanelPosition(panel);
            updatePanelSlotPosition(panel);
        }
    }

    private void updatePanelSlotPosition(PanelState state) {
        List<Slot> slots = menu.getUpgradeSlots(state.type);
        if (state.expanded) {
            state.panel.layoutSlots(slots, imageWidth, state.relativeTabY);
        } else {
            for (Slot slot : slots) {
                slot.y = -2000;
            }
        }
    }

    private void removeWidgets(List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            removeWidget(widget);
        }
    }

    @Override
    public void resize(Minecraft minecraft, int winWidth, int winHeight) {
        updateGuiTextureSize(winHeight);
        setTopRowAndMoveThumb(topVisibleRow, 0);
        super.resize(minecraft, winWidth, winHeight);
    }

    private void updateGuiTextureSize(int winHeight) {
        currentGuiConfig = selectGuiConfig(winHeight);
        containerRows = currentGuiConfig.rows;

        imageWidth = TEXTURE_WIDTH - SCROLL_THUMB_WIDTH;
        imageHeight = currentGuiConfig.height;

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        layout.calculate(containerRows, currentGuiConfig == GUI_CONFIG_4);
        initializeSlots();
    }

    private GuiTextureConfig selectGuiConfig(int winHeight) {
        if (winHeight >= GUI_CONFIG_9.height) return GUI_CONFIG_9;
        if (winHeight >= GUI_CONFIG_7.height) return GUI_CONFIG_7;
        return GUI_CONFIG_4;
    }

    private void initializeSlots() {
        initializeItemSlots();
        initializeToolSlots();
        initializeUpgradeSlots();
        initializePlayerInventorySlots();
        initializeHotbarSlots();
    }

    private void initializeItemSlots() {
        int index = 0;
        for (int y = 0; y < totalRows && index < itemSlots; y++) {
            int yOffset = CONTAINER_SLOTS_MIN_Z + (y * Util.SLOT_SIZE) + 1;
            int slotYOffset = y >= containerRows ? -2000 : yOffset;

            for (int x = 0; x < CONTAINER_COLUMNS && index < itemSlots; x++) {
                Slot slot = menu.getSlot(index++);
                slot.x = CONTAINER_SLOTS_MIN_X + (x * Util.SLOT_SIZE) + 1;
                slot.y = slotYOffset;
            }
        }
    }

    private void initializeToolSlots() {
        int index = itemSlots;
        for (int y = 0; y < TOOL_SLOT_ROWS && index < itemSlots + toolSlots; y++) {
            int yOffset = layout.toolSlotsMinZ + (y * Util.SLOT_SIZE) + 1;

            for (int x = 0; x < TOOL_SLOT_COLUMNS && index < itemSlots + toolSlots; x++) {
                Slot slot = menu.getSlot(index++);
                slot.x = CONTAINER_SLOTS_MIN_X + (x * Util.SLOT_SIZE) + 1;
                slot.y = yOffset;
            }
        }
    }

    private void initializeUpgradeSlots() {
        int index = itemSlots + toolSlots;
        for (int y = 0; y < UPGRADE_SLOT_ROWS && index < totalSlots; y++) {
            int yOffset = CONTAINER_SLOTS_MIN_Z + (y * Util.SLOT_SIZE) + 1;

            for (int x = 0; x < UPGRADE_SLOT_COLUMNS && index < totalSlots; x++) {
                Slot slot = menu.getSlot(index++);
                slot.x = UPGRADE_SLOTS_MIN_X + (x * Util.SLOT_SIZE) + 1;
                slot.y = yOffset;
            }
        }
    }

    private void initializePlayerInventorySlots() {
        int index = 0;
        for (int y = 0; y < 3; y++) {
            int yOffset = layout.inventorySlotsMinZ + (y * Util.SLOT_SIZE) + 1;
            for (int x = 0; x < 9; x++) {
                Slot slot = menu.getPlayerSlot(index++);
                slot.x = PLAYER_INVENTORY_SLOTS_MIN_X + (x * Util.SLOT_SIZE) + 1;
                slot.y = yOffset;
            }
        }
    }

    private void initializeHotbarSlots() {
        for (int x = 0; x < 9; x++) {
            Slot slot = menu.getHotbarSlot(x);
            slot.x = PLAYER_INVENTORY_SLOTS_MIN_X + (x * Util.SLOT_SIZE) + 1;
            slot.y = layout.hotbarSlotsMinZ + 1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
        handlePanelRenderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        handlePanelRender(graphics, mouseX, mouseY); // Tabs/panels should render below backpack GUI

        graphics.blit(currentGuiConfig.texture, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, TEXTURE_WIDTH, currentGuiConfig.height);
        renderScrollbar(graphics);
        renderLockedHotbarSlotOverlay(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int thumbYOffset = isScrollbarRequired() ? 0 : SCROLL_THUMB_TEX_HEIGHT_OFFSET;
        int scrollBarX = leftPos + CONTAINER_SLOTS_MIN_X + (CONTAINER_COLUMNS * Util.SLOT_SIZE) + SCROLL_BAR_OFFSET_X + 1;

        graphics.blit(currentGuiConfig.texture, scrollBarX, getScrollThumbY(),
                SCROLL_THUMB_TEX_MIN_X, SCROLL_THUMB_TEX_MIN_Z + thumbYOffset,
                SCROLL_THUMB_WIDTH, SCROLL_THUMB_HEIGHT, TEXTURE_WIDTH, currentGuiConfig.height);
    }

    private void renderContraptionInactiveUpgradeOverlay(GuiGraphics graphics) {
        if (menu.getBackpackType() != BackpackMenu.BackpackType.CONTRAPTION) return;
        for (int i : menu.layout.upgrades().range()) {
            Slot slot = menu.getSlot(i);

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            UpgradeType upgradeType = UpgradeType.fromItem(stack.getItem());
            if (upgradeType == null || !upgradeType.isPlayerOnly()) continue;

            int slotX = slot.x;
            int slotY = slot.y;
            graphics.fill(RenderType.guiOverlay(), slotX, slotY, slotX + 16, slotY + 16, 0x88AA0000);
        }
    }

    private void renderWorkshopUnusedHeldSlotOverlay(GuiGraphics graphics) {
        List<Slot> slots = menu.getUpgradeSlots(UpgradeType.WORKSHOP);
        if (slots.size() <= WorkshopUpgrade.HELD_SLOT) return;

        Slot heldSlot = slots.get(WorkshopUpgrade.HELD_SLOT);
        if (heldSlot.y < -100) return; // panel collapsed / slot parked off-screen

        Slot machineSlot = slots.get(WorkshopUpgrade.MACHINE_SLOT);
        if (!WorkshopRecipeHelper.isPress(machineSlot.getItem())) return;

        int slotX = heldSlot.x;
        int slotY = heldSlot.y;
        graphics.fill(RenderType.guiOverlay(), slotX, slotY, slotX + 16, slotY + 16, 0x88AA0000);
    }

    private void renderLockedHotbarSlotOverlay(GuiGraphics graphics) {
        if (menu.getBackpackType() != BackpackMenu.BackpackType.ITEM) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int selectedSlot = mc.player.getInventory().selected;
        Slot hotbarSlot = menu.getHotbarSlot(selectedSlot);
        int slotX = leftPos + hotbarSlot.x;
        int slotY = topPos + hotbarSlot.y;
        graphics.fill(RenderType.guiOverlay(), slotX, slotY, slotX + 16, slotY + 16, 0x80AA0000);
    }

    private void handlePanelRender(GuiGraphics graphics, int mouseX, int mouseY) {
        for (PanelState panel : upgradePanels) {
            int panelX = leftPos + imageWidth;
            int panelY = panel.tabY - 2;

            if (panel.expanded) {
                // Render expanded panel background
                graphics.blit(PANEL_TEXTURE, panelX - 3, panelY,
                        panel.panel.getTextureU(), panel.panel.getTextureV(),
                        panel.panel.getExpandedWidth(), panel.panel.getExpandedHeight(), 256, 256);
                panel.panel.render(graphics, mouseX, mouseY);

            } else {
                // Render collapsed tab
                graphics.blit(PANEL_TEXTURE, panelX - 3, panelY,
                        0, PANEL_EXPANDED_HEIGHT, 24, 24, 256, 256);
            }
        }
    }

    private void handlePanelRenderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (PanelState panel : upgradePanels) {
            panel.panel.renderTooltip(font, graphics, mouseX, mouseY, hoveredSlot);

            if (panel.tabButton.isMouseOver(mouseX, mouseY)) {
                MutableComponent tooltip = Component.translatable("tooltip.fxntstorage.upgrade_tab." + panel.type.getId());
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle,
                PLAYER_INVENTORY_SLOTS_MIN_X, layout.inventorySlotsMinZ - 11, 0x404040, false);

        renderContraptionInactiveUpgradeOverlay(graphics);
        renderWorkshopUnusedHeldSlotOverlay(graphics);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        // Tick all expanded panels
        for (PanelState panel : upgradePanels) {
            if (panel.expanded) {
                panel.panel.tick();
            }
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        int x = slot.x;
        int y = slot.y;
        int s = x + y * imageWidth;

        if (slot.isFake()) {
            guiGraphics.renderFakeItem(itemstack, x, y, s);
        } else {
            guiGraphics.renderItem(itemstack, x, y, s);
        }

        if (!itemstack.isEmpty()) {
            renderItemBar(guiGraphics, itemstack, x, y);
            renderItemCount(guiGraphics, itemstack, x, y);
        }
    }

    private void renderItemCount(GuiGraphics guiGraphics, ItemStack itemstack, int x, int y) {
        if (itemstack.getCount() == 1) return;

        String countText = String.valueOf(itemstack.getCount());
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 200.0F);

        float scale = Math.min(1.0F, 16.0F / font.width(countText));
        if (scale < 1.0F) {
            poseStack.scale(scale, scale, 1.0F);
        }

        float textX = (x + 17 - (font.width(countText) * scale)) / scale;
        float textY = (y + 9 + (1 / (scale * scale) - 1)) / scale;
        guiGraphics.drawString(font, countText, textX, textY, 16777215, true);

        poseStack.popPose();
    }

    private void renderItemBar(GuiGraphics guiGraphics, ItemStack itemstack, int x, int y) {
        if (!itemstack.isBarVisible()) return;

        int barWidth = itemstack.getBarWidth();
        int barColor = itemstack.getBarColor();
        int barX = x + 2;
        int barY = y + 13;

        guiGraphics.fill(RenderType.guiOverlay(), barX, barY, barX + 13, barY + 2, -16777216);
        guiGraphics.fill(RenderType.guiOverlay(), barX, barY, barX + barWidth, barY + 1, barColor | -16777216);
    }

    private boolean isScrollbarRequired() {
        return totalRows > containerRows;
    }

    private int getScrollThumbY() {
        return topPos + CONTAINER_SLOTS_MIN_Z + 1 + scrollThumbY;
    }

    private void updateThumbPosition(double adjustedMouseY) {
        int maxScroll = layout.containerSlotsHeight - SCROLL_THUMB_HEIGHT - 2;
        scrollThumbY = (int) Math.clamp(adjustedMouseY, 0, maxScroll);

        int row = (int) Math.round(((double) scrollThumbY) / maxScroll * (totalRows - containerRows));
        setTopRow(topVisibleRow, row);
    }

    private void snapThumbToGradation() {
        int maxScroll = containerRows * Util.SLOT_SIZE - 2 - SCROLL_THUMB_HEIGHT;
        scrollThumbY = (int) (((double) topVisibleRow / (totalRows - containerRows)) * maxScroll);
    }

    private void setTopRow(int oldTopRow, int newTopRow) {
        if (oldTopRow == newTopRow) return;

        topVisibleRow = newTopRow;
        int yOffsetBase = CONTAINER_SLOTS_MIN_Z + 1;
        int slotsPerPage = CONTAINER_COLUMNS * containerRows;

        clearSlotRange(oldTopRow * CONTAINER_COLUMNS, slotsPerPage);

        int newStartIndex = newTopRow * CONTAINER_COLUMNS;
        int newEndIndex = Math.min(newStartIndex + slotsPerPage, itemSlots);

        for (int index = newStartIndex; index < newEndIndex; index++) {
            int row = (index / CONTAINER_COLUMNS) - newTopRow;
            menu.slots.get(index).y = yOffsetBase + row * Util.SLOT_SIZE;
        }
    }

    private void clearSlotRange(int startIndex, int count) {
        int endIndex = Math.min(startIndex + count, itemSlots);
        for (int index = startIndex; index < endIndex; index++) {
            menu.slots.get(index).y = -2000;
        }
    }

    private void setTopRowAndMoveThumb(int oldTopRow, int newTopRow) {
        setTopRow(oldTopRow, newTopRow);
        snapThumbToGradation();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        handleCtrlKey(keyCode, true);
        return handleKeyPress(keyCode, scanCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        handleCtrlKey(keyCode, false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void handleCtrlKey(int keyCode, boolean pressed) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            PacketDistributor.sendToServer(new KeyPressedPacket(Util.BACKPACK_MENU_CTRL, pressed, Optional.empty()));
        }
    }

    protected boolean handleKeyPress(int keyCode, int scanCode) {
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            return scrollDown();
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            return scrollUp();
        } else if (TOGGLE_BACKPACK_KEY.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            KeybindHandler.handleOpenCloseBackpack();
            return true;
        }
        return false;
    }

    private boolean scrollDown() {
        if (topVisibleRow == totalRows - containerRows) return true;

        int step = hasShiftDown() ? containerRows : 1;
        int newTop = Math.min(topVisibleRow + step, totalRows - containerRows);
        setTopRowAndMoveThumb(topVisibleRow, newTop);
        return true;
    }

    private boolean scrollUp() {
        if (topVisibleRow == 0) return true;

        int step = hasShiftDown() ? containerRows : 1;
        int newTop = Math.max(topVisibleRow - step, 0);
        setTopRowAndMoveThumb(topVisibleRow, newTop);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isScrollbarRequired()) {
            if (isMouseOverScrollThumb(mouseX, mouseY)) {
                scrollYOffset = (int) mouseY - scrollThumbY;
                isDragging = true;
            } else if (isMouseOverScrollBar(mouseX, mouseY)) {
                updateThumbPosition(mouseY - Util.CONTAINER_HEADER_HEIGHT - 1 - topPos);
                snapThumbToGradation();
            }
        }

        for (PanelState panel : upgradePanels) {
            if (panel.expanded && panel.panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        menu.container.setPlayerInteraction(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            updateThumbPosition(mouseY - scrollYOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            snapThumbToGradation();
            return true;
        }
        menu.container.setPlayerInteraction(false);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!isScrollbarRequired() || !isMouseOverScrollArea(mouseX, mouseY)) return false;

        int step = hasShiftDown() ? containerRows : 1;
        int newTop = scrollY < 0
                ? Math.min(topVisibleRow + step, totalRows - containerRows)
                : Math.max(topVisibleRow - step, 0);

        setTopRowAndMoveThumb(topVisibleRow, newTop);
        return true;
    }

    private boolean isMouseOverScrollArea(double mouseX, double mouseY) {
        int scrollBarX = leftPos + CONTAINER_SLOTS_MIN_X + (CONTAINER_COLUMNS * Util.SLOT_SIZE) + SCROLL_BAR_OFFSET_X;
        return mouseX >= leftPos + CONTAINER_SLOTS_MIN_X
                && mouseX <= scrollBarX + SCROLL_BAR_WIDTH
                && mouseY >= topPos + CONTAINER_SLOTS_MIN_Z
                && mouseY <= topPos + layout.containerSlotsMaxZ;
    }

    private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
        int scrollBarX = leftPos + CONTAINER_SLOTS_MIN_X + (CONTAINER_COLUMNS * Util.SLOT_SIZE) + SCROLL_BAR_OFFSET_X;
        return mouseX >= scrollBarX
                && mouseX <= scrollBarX + SCROLL_BAR_WIDTH
                && mouseY >= topPos + CONTAINER_SLOTS_MIN_Z
                && mouseY <= topPos + layout.containerSlotsMaxZ;
    }

    private boolean isMouseOverScrollThumb(double mouseX, double mouseY) {
        int scrollBarX = leftPos + CONTAINER_SLOTS_MIN_X + (CONTAINER_COLUMNS * Util.SLOT_SIZE) + SCROLL_BAR_OFFSET_X;
        return mouseX >= scrollBarX
                && mouseX <= scrollBarX + SCROLL_BAR_WIDTH
                && mouseY >= getScrollThumbY()
                && mouseY <= getScrollThumbY() + SCROLL_THUMB_HEIGHT;
    }

    @NotNull
    @ApiStatus.OverrideOnly
    public List<Rect2i> getExclusionZones() {
        if (upgradePanels.isEmpty())
            return List.of();

        int panelX = leftPos + imageWidth;
        int minTop = Integer.MAX_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        int maxWidth = 0;

        for (PanelState panel : upgradePanels) {
            int top = panel.tabY - 2;
            int width = panel.expanded ? panel.panel.getExpandedWidth() - 3 : PANEL_TAB_WIDTH + 2;
            int height = panel.expanded ? panel.panel.getExpandedHeight() : PANEL_TAB_HEIGHT + 3;

            minTop = Math.min(minTop, top);
            maxBottom = Math.max(maxBottom, top + height);
            maxWidth = Math.max(maxWidth, width);
        }

        return List.of(new Rect2i(panelX, minTop, maxWidth, maxBottom - minTop));
    }

    private record GuiTextureConfig(ResourceLocation texture, int height, int rows) {
    }

    public static boolean isHoveredUpgradeSlotInContraption() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof BackpackScreen screen)) return false;
        if (screen.getMenu().getBackpackType() != BackpackMenu.BackpackType.CONTRAPTION) return false;
        return screen.hoveredSlot instanceof UpgradeSlot;
    }

    private static class LayoutPositions {
        int containerSlotsMaxZ;
        int containerSlotsHeight;
        int toolSlotsMinZ;
        int toolSlotsMaxZ;
        int inventorySlotsMinZ;
        int inventorySlotsMaxZ;
        int hotbarSlotsMinZ;

        void calculate(int rows, boolean isSmallGui) {
            containerSlotsMaxZ = CONTAINER_SLOTS_MIN_Z + (Util.SLOT_SIZE * rows);
            containerSlotsHeight = containerSlotsMaxZ - CONTAINER_SLOTS_MIN_X;

            toolSlotsMinZ = containerSlotsMaxZ + (isSmallGui ? 2 : 4);
            toolSlotsMaxZ = toolSlotsMinZ + (Util.SLOT_SIZE * TOOL_SLOT_ROWS);

            inventorySlotsMinZ = toolSlotsMaxZ + 15;
            inventorySlotsMaxZ = inventorySlotsMinZ + (Util.SLOT_SIZE * 3);

            hotbarSlotsMinZ = inventorySlotsMaxZ + 4;
        }
    }
}
