package net.fxnt.fxntstorage.backpack.upgrade.voiding;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIcon;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIconSprites;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.button.VoidModeButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.VoidFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class VoidUpgrade extends AbstractUpgrade {

    public VoidUpgrade() {
        super(UpgradeType.VOID);
    }

    public enum VoidMode {
        // Order matters: the panel button cycles through these and the index is what gets persisted
        VOID_ALWAYS("void_always"),
        VOID_SLOT_OVERFLOW("void_slot_overflow"),
        VOID_BACKPACK_OVERFLOW("void_backpack_overflow");

        private final String id;

        VoidMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static VoidMode fromIndex(int index) {
            VoidMode[] values = values();
            return index >= 0 && index < values.length ? values[index] : VOID_ALWAYS;
        }
    }

    public static final int MIN_MODE = 0;
    public static final int MAX_MODE = VoidMode.values().length - 1;

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(UpgradeDataSync.Field.VOID_GUI_ALLOW);
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(UpgradeDataSync.Field.VOID_GUI_ALLOW, false);
    }

    @Override
    public List<UpgradeDataSync.Field> getIntSettings() {
        return List.of(UpgradeDataSync.Field.VOID_MODE);
    }

    @Override
    public Map<UpgradeDataSync.Field, Integer> getDefaultIntSettings() {
        return Map.of(UpgradeDataSync.Field.VOID_MODE, VoidMode.VOID_ALWAYS.ordinal());
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(new VoidFilterSlot(
                menu.container,
                menu.layout.voidFilter().getStartIndex(),
                274, 58,
                () -> menu.hasUpgrade(UpgradeType.VOID),
                () -> menu.isPanelExpanded(UpgradeType.VOID)
        ));
    }

    @Override
    public int getFilterSlotIndex(BackpackSlotLayout layout) {
        return layout.voidFilter().getStartIndex();
    }

    @Override
    public boolean filterAccepts(UpgradeContext context, ItemStack stack) {
        return true; // Any block or item, plus any Create filter
    }

    @Override
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        if (context.slotId() != getFilterSlotIndex(layout)) return false;

        return GhostFilterHelper.handleClick(context, stack -> filterAccepts(context, stack));
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new VoidPanel(context);
    }

    @Override
    public void onUninstalled(UpgradeContext context) {
        GhostFilterHelper.returnFilterItem(context, getFilterSlotIndex(BackpackSlotLayout.createLayout()));
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.isClientSide()) return;
        voidItems(context.container(), context.level());
    }

    // Runs the configured void mode over the item storage slots
    public static void voidItems(IBackpackContainer container, Level level) {
        if (!container.getUpgradeSetting(UpgradeDataSync.Field.VOID_GUI_ALLOW)) return;

        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        IItemHandlerModifiable itemHandler = container.getItemHandler();

        ItemStack filterStack = itemHandler.getStackInSlot(layout.voidFilter().getStartIndex());
        if (filterStack.isEmpty()) return;

        FilterItemStack filter = FilterItemStack.of(filterStack);
        if (filter.isEmpty()) return;

        VoidMode mode = VoidMode.fromIndex(container.getUpgradeIntSetting(UpgradeDataSync.Field.VOID_MODE));

        boolean voided = switch (mode) {
            case VOID_ALWAYS -> voidAlways(itemHandler, layout, filter, level);
            case VOID_SLOT_OVERFLOW -> voidSlotOverflow(itemHandler, layout, filter, level);
            case VOID_BACKPACK_OVERFLOW -> voidBackpackOverflow(itemHandler, layout, filter, level);
        };

        if (voided) container.setDataChanged();
    }

    // Deletes every matching stack the moment it lands in an item storage slot
    private static boolean voidAlways(IItemHandlerModifiable itemHandler, BackpackSlotLayout layout,
                                      FilterItemStack filter, Level level) {
        boolean voided = false;
        for (int i : layout.items().range()) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !filter.test(level, stack)) continue;
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            voided = true;
        }
        return voided;
    }

    // Keeps one slot's worth of each matching item and deletes anything past that
    private static boolean voidSlotOverflow(IItemHandlerModifiable itemHandler, BackpackSlotLayout layout,
                                            FilterItemStack filter, Level level) {
        List<ItemStack> kept = new ArrayList<>();
        boolean voided = false;

        for (int i : layout.items().range()) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !filter.test(level, stack)) continue;

            boolean alreadyKept = kept.stream().anyMatch(other -> ItemStack.isSameItemSameComponents(other, stack));
            if (alreadyKept) {
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                voided = true;
                continue;
            }

            kept.add(stack);

            // Trim anything already sitting above the slot limit
            int limit = itemHandler.getSlotLimit(i);
            if (stack.getCount() > limit) {
                itemHandler.setStackInSlot(i, stack.copyWithCount(limit));
                voided = true;
            }
        }
        return voided;
    }

    private static boolean voidBackpackOverflow(IItemHandlerModifiable itemHandler, BackpackSlotLayout layout,
                                                FilterItemStack filter, Level level) {
        boolean voided = false;
        for (int i : layout.items().range()) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !filter.test(level, stack)) continue;

            int limit = itemHandler.getSlotLimit(i);
            if (stack.getCount() > limit) {
                itemHandler.setStackInSlot(i, stack.copyWithCount(limit));
                voided = true;
            }
        }
        return voided;
    }

    public static ItemStack acceptableInsert(IBackpackContainer container, @Nullable Level level, ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty()) return stack;

        IItemHandlerModifiable itemHandler = container.getItemHandler();
        if (!UpgradeHelper.hasActiveUpgrade(itemHandler, UpgradeType.VOID)) return stack;

        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        ItemStack filterStack = itemHandler.getStackInSlot(layout.voidFilter().getStartIndex());
        if (filterStack.isEmpty()) return stack;

        FilterItemStack filter = FilterItemStack.of(filterStack);
        if (filter.isEmpty() || !filter.test(level, stack)) return stack;

        VoidMode mode = VoidMode.fromIndex(container.getUpgradeIntSetting(UpgradeDataSync.Field.VOID_MODE));
        return switch (mode) {
            case VOID_ALWAYS -> ItemStack.EMPTY;
            case VOID_SLOT_OVERFLOW -> {
                // One slot's worth of the item is kept, so only that much of the stack may be stored
                int limit = stack.getMaxStackSize() * container.getStackMultiplier();
                int room = limit - storedCount(itemHandler, layout, stack);
                yield room <= 0 ? ItemStack.EMPTY
                        : room >= stack.getCount() ? stack : stack.copyWithCount(room);
            }
            case VOID_BACKPACK_OVERFLOW ->
                    isStorageFullFor(itemHandler, layout, stack) ? ItemStack.EMPTY : stack;
        };
    }

    private static int storedCount(IItemHandlerModifiable itemHandler, BackpackSlotLayout layout, ItemStack stack) {
        int count = 0;
        for (int i : layout.items().range()) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) count += slotStack.getCount();
        }
        return count;
    }

    // The item storage has nowhere left to put this stack
    private static boolean isStorageFullFor(IItemHandlerModifiable itemHandler, BackpackSlotLayout layout, ItemStack stack) {
        for (int i : layout.items().range()) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) return false;
            if (ItemStack.isSameItemSameComponents(slotStack, stack)
                    && slotStack.getCount() < itemHandler.getSlotLimit(i)) return false;
        }
        return true;
    }

    public static class VoidPanel implements UpgradePanel {
        private static final String TOOLTIP_PREFIX = "tooltip.fxntstorage.backpack_void_upgrade.panel.";

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        private static final GuiIconSprites GUI_ALLOW = new GuiIconSprites(GuiIcon.VOID_GUI_ALLOW);
        private static final GuiIconSprites GUI_DENY = new GuiIconSprites(GuiIcon.VOID_GUI_DENY);

        private VoidModeButton modeButton;
        private SpriteButton<Boolean> guiButton;
        private boolean lastFiltered;

        private int panelX;
        private int panelY;

        public VoidPanel(UpgradeContext context) {
            this.context = context;
        }

        @Override
        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
            int oldY = this.panelY;

            this.panelX = leftPos + imageWidth;
            this.panelY = tabY - 10;

            int deltaY = this.panelY - oldY;

            for (AbstractWidget widget : widgets) {
                widget.setPosition(widget.getX(), widget.getY() + deltaY);
            }
        }

        private int voidMode() {
            return context.<BackpackMenu>menu().getUpgradeSyncValue(UpgradeDataSync.Field.VOID_MODE);
        }

        private Component modeTooltip(int modeIndex) {
            VoidMode mode = VoidMode.fromIndex(modeIndex);
            MutableComponent tooltip = Component.translatable(TOOLTIP_PREFIX + mode.getId());

            // Nothing is voided while the filter slot is empty, whatever mode is selected
            if (!hasFilter()) {
                tooltip.append("\n").append(Component.translatable(TOOLTIP_PREFIX + "no_filter")
                        .withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD));
            }

            return tooltip.append("\n")
                    .append(Component.translatable(TOOLTIP_PREFIX + mode.getId() + ".description")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }

        private boolean voidGuiItems() {
            return context.<BackpackMenu>menu().isUpgradeSettingEnabled(UpgradeDataSync.Field.VOID_GUI_ALLOW);
        }

        private Component guiTooltip(boolean allow) {
            String id = allow ? "void_gui_allow" : "void_gui_deny";
            return Component.translatable(TOOLTIP_PREFIX + id)
                    .append("\n")
                    .append(Component.translatable(TOOLTIP_PREFIX + id + ".description")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }

        private boolean hasFilter() {
            BackpackMenu menu = context.menu();
            return !menu.container.getItemHandler()
                    .getStackInSlot(menu.layout.voidFilter().getStartIndex()).isEmpty();
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            modeButton = new VoidModeButton(
                    panelX + 22, panelY + 31, 18, 18,
                    voidMode(),
                    this::modeTooltip,
                    button -> menu.cycleUpgradeIntSetting(UpgradeDataSync.Field.VOID_MODE, MIN_MODE, MAX_MODE)
            );

            guiButton = new SpriteButton<>(
                    panelX + 41, panelY + 31, 18, 18,
                    voidGuiItems(),
                    allow -> allow ? GUI_ALLOW : GUI_DENY,
                    this::guiTooltip,
                    button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.VOID_GUI_ALLOW)
            );

            widgetAdder.accept(modeButton);
            widgets.add(modeButton);
            widgetAdder.accept(guiButton);
            widgets.add(guiButton);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean filtered = hasFilter();
            if (modeButton != null && filtered != lastFiltered) {
                lastFiltered = filtered;
                modeButton.refreshTooltip();
            }
        }

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
            if (hoveredSlot instanceof VoidFilterSlot && !hoveredSlot.hasItem()) {
                graphics.renderTooltip(
                        font,
                        List.of(
                                Component.translatable(TOOLTIP_PREFIX + "filter_slot"),
                                Component.translatable(TOOLTIP_PREFIX + "filter_slot.description")
                                        .withStyle(ChatFormatting.DARK_GRAY)
                        ),
                        Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false; // Handled by widgets
        }

        @Override
        public void tick() {
            if (modeButton != null) modeButton.updateState(voidMode());
            if (guiButton != null) guiButton.updateState(voidGuiItems());
        }

        @Override
        public List<AbstractWidget> getWidgets() {
            return widgets;
        }

        @Override
        public void clearWidgets() {
            widgets.clear();
            modeButton = null;
            guiButton = null;
        }
    }
}
