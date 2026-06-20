package net.fxnt.fxntstorage.backpack.inventory;

import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModAttachmentTypes;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.network.packet.SetActivePanelPacket;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.network.packet.UpgradeDataPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BackpackContainer implements IBackpackContainer, IItemHandlerModifiable {
    private final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    private final int CONTAINER_SIZE = layout.getTotalSlots();

    private Player player;
    private ItemStack stack;
    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE) {
        @Override
        public int getSlotLimit(int slot) {
            ItemStack current = getStackInSlot(slot);
            int maxStack = current.isEmpty() ? 64 : current.getMaxStackSize();
            return maxStack * stackMultiplier;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return stack.getMaxStackSize() * stackMultiplier;
        }
    };
    private final NonNullList<String> upgrades = NonNullList.create();

    private int stackMultiplier;
    private SortOrder sortOrder = SortOrder.COUNT;

    // Unified upgrade data management
    private final UpgradeDataManager upgradeData = new UpgradeDataManager();

    public BackpackContainer(@Nullable Player player, ItemStack itemStack) {
        this.player = player;
        this.stack = itemStack;
        loadItemsFromStack(itemStack);
    }

    public void setContext(ItemStack itemStack, @Nullable Player player) {
        this.stack = itemStack;
        this.player = player;
        if (player != null && !player.level().isClientSide)
            readInventory(itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public void readInventory(ItemContainerContents contents) {
        int slots = itemHandler.getSlots();
        int contentSlots = contents.getSlots();
        for (int i = 0; i < slots; i++) {
            // For slots beyond the contents range, set EMPTY so stale items are cleared correctly.
            itemHandler.setStackInSlot(i, i < contentSlots ? contents.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        readInventory(itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

        // Load upgrades list
        if (itemStack.getComponents().has(ModDataComponents.BACKPACK_UPGRADES)) {
            upgrades.clear();
            List<String> upgradeList = itemStack.getComponents().get(ModDataComponents.BACKPACK_UPGRADES);
            if (upgradeList != null) {
                upgrades.addAll(upgradeList);
            }
        }

        // Load stack multiplier
        stackMultiplier = Optional.ofNullable(
                itemStack.getComponents().get(ModDataComponents.BACKPACK_STACK_MULTIPLIER)
        ).orElse(((BackpackBlock) ((BackpackItem) itemStack.getItem()).getBlock()).getStackMultiplier());

        // Load sort order
        sortOrder = Optional.ofNullable(
                itemStack.getComponents().get(ModDataComponents.INVENTORY_SORT_ORDER)
        ).orElse(SortOrder.COUNT);

        // Load upgrade data (panels and settings) and copy into our manager
        UpgradeDataManager loadedData = UpgradeDataManager.loadFromItem(itemStack);
        upgradeData.copyFrom(loadedData);
    }

    public void saveItemsToStack() {
        this.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        this.stack.set(ModDataComponents.BACKPACK_UPGRADES, upgrades);
        this.stack.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, stackMultiplier);
        this.stack.set(ModDataComponents.INVENTORY_SORT_ORDER, sortOrder);

        // Save upgrade data
        upgradeData.saveToItem(stack);
    }

    public void saveSettings() {
        upgradeData.saveToItem(stack);
        setChanged();
    }

    public List<ItemStack> getItems() {
        List<ItemStack> itemList = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            itemList.add(itemHandler.getStackInSlot(i));
        }
        return itemList;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        if (player != null && player.level().isClientSide)
            PacketDistributor.sendToServer(new SetSortOrderPacket(sortOrder));
        setDataChanged();
    }

    @Override
    public boolean isPanelExpanded(UpgradeType type) {
        return upgradeData.isPanelExpanded(type);
    }

    @Override
    public void togglePanelExpanded(UpgradeType type) {
        upgradeData.togglePanel(type);
        if (player != null && player.level().isClientSide) {
            PacketDistributor.sendToServer(new SetActivePanelPacket(type));
        } else if (player != null) {
            setDataChanged();
        }
    }

    @Override
    public void clearPanelExpanded(UpgradeType type) {
        upgradeData.clearPanel(type);
        if (player != null && !player.level().isClientSide) {
            setDataChanged();
        }
    }

    @Override
    public UpgradeType getExpandedPanel() {
        return upgradeData.getExpandedPanel();
    }

    @Override
    public void setExpandedPanel(UpgradeType type) {
        upgradeData.setExpandedPanel(type);
        setDataChanged();
    }

    @Override
    public boolean getUpgradeSetting(UpgradeDataSync.Field setting) {
        return upgradeData.getSetting(setting);
    }

    @Override
    public void setUpgradeSetting(UpgradeDataSync.Field setting, boolean value) {
        upgradeData.setSetting(setting, value);
        if (player != null && player.level().isClientSide)
            PacketDistributor.sendToServer(new UpgradeDataPacket(setting.getIndex(), value));
        setDataChanged();
    }

    @Override
    public int getSlots() {
        return itemHandler.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int i) {
        return itemHandler.getStackInSlot(i);
    }

    @Override
    public void setStackInSlot(int i, @NotNull ItemStack itemStack) {
        itemHandler.setStackInSlot(i, itemStack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack itemStack, boolean simulate) {
        ItemStack remainder = itemHandler.insertItem(slot, itemStack, simulate);
        if (!simulate && remainder.getCount() != itemStack.getCount())
            saveItemsToStack();
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack result = itemHandler.extractItem(slot, amount, simulate);
        if (!simulate && !result.isEmpty())
            saveItemsToStack();
        return result;
    }

    @Override
    public int getSlotLimit(int i) {
        return itemHandler.getSlotLimit(i);
    }

    @Override
    public boolean isItemValid(int i, @NotNull ItemStack itemStack) {
        return true;
    }

    @Override
    public int getStackMultiplier() {
        return stackMultiplier;
    }

    @Override
    public void setPlayerInteraction(boolean isPlayer) {
        // noop
    }

    @Override
    public void setDataChanged() {
        if (player == null || player.level().isClientSide) return;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack selectedStack = player.getInventory().getSelected();
        if (selectedStack.getItem() instanceof BackpackItem) {
            return true;
        }
        return BackpackHelper.isWearingBackpack(player);
    }

    public void refreshUpgrades() {
        upgrades.clear();
        upgrades.addAll(UpgradeHelper.refreshUpgradeList(itemHandler));
    }

    public void setChanged() {
        if (player == null || player.level().isClientSide) return;

        var cap = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (cap != null) {
            for (int i : layout.upgrades().range()) {
                if (!ItemStack.matches(cap.getStackInSlot(i), itemHandler.getStackInSlot(i))) {
                    refreshUpgrades();

                    Set<UpgradeType> installedTypes = new HashSet<>(UpgradeHelper.getInstalledUpgrades(itemHandler));
                    for (UpgradeType type : UpgradeType.values()) {
                        if (installedTypes.contains(type)) continue;

                        IUpgrade upgrade = UpgradeRegistry.get(type);
                        if (upgrade != null) {
                            for (UpgradeDataSync.Field field : upgrade.getSettings()) {
                                upgradeData.clearSetting(field);
                            }
                        }
                        // Collapse the panel for this removed upgrade type.
                        upgradeData.clearPanel(type);
                    }

                    break;
                }
            }
        }

        saveItemsToStack();

        boolean openMenuShowsThis = player.containerMenu instanceof BackpackMenu menu && menu.container == this;
        boolean isCachedWornContainer =
                player.getData(ModAttachmentTypes.WORN_BACKPACK_CONTAINER) == this;
        if (!openMenuShowsThis && !isCachedWornContainer) {
            BackpackContainer.Cache.invalidateWornBackpack(player);
        }
    }

    public static class Cache {
        public static BackpackContainer getOrCreateWornBackpack(Player player, ItemStack backpack) {
            BackpackContainer cached = player.getData(ModAttachmentTypes.WORN_BACKPACK_CONTAINER);

            if (cached != null) {
                cached.setContext(backpack, player);
                return cached;
            }

            BackpackContainer newContainer = new BackpackContainer(player, backpack);
            player.setData(ModAttachmentTypes.WORN_BACKPACK_CONTAINER, newContainer);
            return newContainer;
        }

        public static void invalidateWornBackpack(Player player) {
            if (player.hasData(ModAttachmentTypes.WORN_BACKPACK_CONTAINER))
                player.removeData(ModAttachmentTypes.WORN_BACKPACK_CONTAINER);
        }

        public static BackpackContainer getForCapability(ItemStack backpack) {
            return new BackpackContainer(null, backpack);
        }
    }
}
