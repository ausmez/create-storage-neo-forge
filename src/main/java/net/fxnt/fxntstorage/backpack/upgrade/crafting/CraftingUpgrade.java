package net.fxnt.fxntstorage.backpack.upgrade.crafting;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.slot.CraftingGridSlot;
import net.fxnt.fxntstorage.backpack.client.menu.slot.CraftingResultSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradePanel;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class CraftingUpgrade extends AbstractUpgrade {

    // Guard against endless crafting loops (in practice it should never be reached)
    private static final int MAX_CRAFT_ITERATIONS = 1024;

    public CraftingUpgrade() {
        super(UpgradeType.CRAFTING);
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        IBackpackContainer backpack = menu.container;
        int matrixStart = menu.layout.craftingMatrix().getStartIndex();

        BackpackCraftingContainer craftingContainer =
                new BackpackCraftingContainer(backpack, matrixStart, menu::onCraftingGridChanged);
        ResultContainer resultContainer = new ResultContainer();

        BooleanSupplier hasUpgrade = () -> menu.hasUpgrade(UpgradeType.CRAFTING);
        BooleanSupplier expanded = () -> menu.isPanelExpanded(UpgradeType.CRAFTING);

        List<Slot> slots = new ArrayList<>(BackpackCraftingContainer.WIDTH * BackpackCraftingContainer.HEIGHT + 1);
        for (int row = 0; row < BackpackCraftingContainer.HEIGHT; row++) {
            for (int col = 0; col < BackpackCraftingContainer.WIDTH; col++) {
                int idx = col + row * BackpackCraftingContainer.WIDTH;
                slots.add(new CraftingGridSlot(craftingContainer, idx, 0, 0, hasUpgrade, expanded));
            }
        }
        Slot resultSlot = new CraftingResultSlot(
                menu.player, craftingContainer, resultContainer, 0, 0, 0, hasUpgrade, expanded);
        slots.add(resultSlot);

        menu.registerCraftingComponents(craftingContainer, resultContainer, resultSlot);
        return slots;
    }

    @Override
    public void onRemoved(UpgradeContext context) {
        if (context.isClientSide()) return;
        if (!(context.menu() instanceof BackpackMenu menu)) return;

        IItemHandlerModifiable handler = context.itemHandler();
        Player player = context.player();
        BackpackSlotLayout layout = menu.layout;

        boolean changed = false;
        for (int i : layout.craftingMatrix().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ItemStack toReturn = stack.copy();
            handler.setStackInSlot(i, ItemStack.EMPTY);
            changed = true;

            if (player != null) {
                menu.moveStackToPlayerInventory(toReturn);
                if (!toReturn.isEmpty()) {
                    player.drop(toReturn, false);
                }
            }
        }

        if (changed) {
            menu.onCraftingGridChanged();
        }
    }

    @Override
    public Optional<ItemStack> onQuickMove(UpgradeContext context) {
        if (!(context.menu() instanceof BackpackMenu menu)) return Optional.empty();
        Slot resultSlot = menu.getCraftingResultSlot();
        if (resultSlot == null || menu.getCraftingContainer() == null) return Optional.empty();
        if (!menu.hasActiveUpgrade(UpgradeType.CRAFTING) || !menu.isPanelExpanded(UpgradeType.CRAFTING)) {
            return Optional.empty();
        }

        int slotIndex = context.slotId();
        Player player = context.player();

        // Shift-click the result
        if (slotIndex == resultSlot.index) {
            if (context.isClientSide()) return Optional.of(ItemStack.EMPTY);

            ItemStack first = resultSlot.getItem();
            if (first.isEmpty()) return Optional.of(ItemStack.EMPTY);
            ItemStack template = first.copy();

            int iterations = 0;
            while (iterations++ < MAX_CRAFT_ITERATIONS) {
                ItemStack current = resultSlot.getItem();
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, template)) break;
                if (!canFullyFit(menu, current)) break;

                ItemStack crafted = current.copy();
                ItemStack toGive = current.copy();
                // Deposit into the backpack's item storage first, then the player inventory.
                menu.moveStackToStorageThenPlayer(toGive); // guaranteed to fully fit

                if (resultSlot instanceof CraftingResultSlot crs) {
                    crs.accountQuickCraft(crafted, crafted.getCount());
                }
                resultSlot.onTake(player, crafted); // consumes ingredients and recomputes the result
            }
            return Optional.of(ItemStack.EMPTY);
        }

        // Shift-click an ingredient out of the grid - send it to the player inventory
        if (menu.layout.craftingMatrix().contains(slotIndex)) {
            Slot slot = menu.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && menu.moveStackToPlayerInventory(stack)) {
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                }
                slot.setChanged();
            }
            return Optional.of(ItemStack.EMPTY);
        }

        return Optional.empty();
    }

    private static boolean canFullyFit(BackpackMenu menu, ItemStack stack) {
        int needed = stack.getCount();
        needed = subtractCapacity(menu, stack, needed,
                menu.layout.items().getStartIndex(), menu.layout.items().getEndIndex());
        int invStart = menu.layout.getTotalSlots();
        needed = subtractCapacity(menu, stack, needed, invStart, invStart + 36);
        return needed <= 0;
    }

    private static int subtractCapacity(BackpackMenu menu, ItemStack stack, int needed, int start, int end) {
        for (int i = start; i < end && needed > 0; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty()) {
                if (slot.mayPlace(stack)) {
                    needed -= slot.getMaxStackSize(stack);
                }
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                needed -= Math.max(0, slot.getMaxStackSize(existing) - existing.getCount());
            }
        }
        return needed;
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new CraftingPanel();
    }

    public static class CraftingPanel implements UpgradePanel {

        // Expanded crafting panel background
        private static final int PANEL_WIDTH = 68;
        private static final int PANEL_HEIGHT = 128;
        private static final int TEXTURE_U = 68;
        private static final int TEXTURE_V = 0;

        // Item positions inside the panel background atlas
        private static final int[] GRID_COLUMN_X = {8, 26, 44};
        private static final int[] GRID_ROW_Y = {24, 42, 60};
        private static final int RESULT_X = 26;
        private static final int RESULT_Y = 101;

        @Override
        public int getExpandedWidth() {
            return PANEL_WIDTH;
        }

        @Override
        public int getExpandedHeight() {
            return PANEL_HEIGHT;
        }

        @Override
        public int getTextureU() {
            return TEXTURE_U;
        }

        @Override
        public int getTextureV() {
            return TEXTURE_V;
        }

        @Override
        public void layoutSlots(List<Slot> slots, int imageWidth, int relativeTabY) {
            int panelLeft = imageWidth - 3;
            int panelTop = relativeTabY - 2;
            for (Slot slot : slots) {
                if (slot instanceof CraftingResultSlot) {
                    slot.x = panelLeft + RESULT_X;
                    slot.y = panelTop + RESULT_Y;
                } else {
                    int idx = slot.getContainerSlot(); // 0..8, row-major
                    slot.x = panelLeft + GRID_COLUMN_X[idx % 3];
                    slot.y = panelTop + GRID_ROW_Y[idx / 3];
                }
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        }

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
        }

        @Override
        public List<AbstractWidget> getWidgets() {
            return List.of();
        }

        @Override
        public void clearWidgets() {
        }

        @Override
        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public void tick() {
        }
    }
}
