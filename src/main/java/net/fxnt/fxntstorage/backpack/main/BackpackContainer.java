package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BackpackContainer implements IBackpackContainer, IItemHandlerModifiable {
    private final int CONTAINER_SIZE = BackpackBlock.getSlotCount();
    private final Player player;
    private int stackMultiplier;
    private final ItemStack stack;

    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE);
    private final NonNullList<String> upgrades = NonNullList.create();

    private SortOrder sortOrder = SortOrder.COUNT;

    public BackpackContainer(ItemStack itemStack, @Nullable Player player) {
        this.player = player;
        this.stack = itemStack;

        if (itemStack.getItem() instanceof BackpackItem backpackItem &&
                backpackItem.getBlock() instanceof BackpackBlock backpackBlock) {
            stackMultiplier = backpackBlock.getStackMultiplier();
        }
        loadItemsFromStack(itemStack);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        if (itemStack.getComponents().has(DataComponents.CONTAINER)) {
            readInventory(itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        }

        if (itemStack.getComponents().has(ModDataComponents.BACKPACK_UPGRADES)) {
            upgrades.clear();
            List<String> upgradeList = itemStack.getComponents().get(ModDataComponents.BACKPACK_UPGRADES);

            if (upgradeList != null) {
                upgrades.clear();
                upgrades.addAll(upgradeList);
            }
        }

        stackMultiplier = Optional.ofNullable(
                itemStack.getComponents().get(ModDataComponents.BACKPACK_STACK_MULTIPLIER)
        ).orElse(((BackpackBlock) ((BackpackItem) itemStack.getItem()).getBlock()).getStackMultiplier());

        sortOrder = Optional.ofNullable(
                itemStack.getComponents().get(ModDataComponents.INVENTORY_SORT_ORDER)
        ).orElse(SortOrder.COUNT);
    }

    public void saveItemsToStack() {
        this.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        this.stack.set(ModDataComponents.BACKPACK_UPGRADES, upgrades);
        this.stack.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, stackMultiplier);
        this.stack.set(ModDataComponents.INVENTORY_SORT_ORDER, sortOrder);
    }

    public List<ItemStack> getItems() {
        List<ItemStack> itemList = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            itemList.add(itemHandler.getStackInSlot(i));
        }
        return itemList;
    }

    public NonNullList<String> getUpgrades() {
        return upgrades;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        if (player.level().isClientSide)
            PacketDistributor.sendToServer(new SetSortOrderPacket(sortOrder));
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
    public @NotNull ItemStack insertItem(int i, @NotNull ItemStack itemStack, boolean b) {
        return itemHandler.insertItem(i, itemStack, b);
    }

    @Override
    public @NotNull ItemStack extractItem(int i, int i1, boolean b) {
        return itemHandler.extractItem(i, i1, b);
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

    public void refreshUpgrades() {
        upgrades.clear();
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.ITEM_SLOT_COUNT + BackpackBlock.TOOL_SLOT_COUNT;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.UPGRADE_SLOT_COUNT;

        for (int i = UPGRADE_SLOT_START_INDEX; i < UPGRADE_SLOT_END_INDEX; i++) {
            ItemStack itemStack = itemHandler.getStackInSlot(i);
            if (itemStack.getItem() instanceof UpgradeItem upgradeItem) {
                String upgradeName = upgradeItem.getUpgradeName();
                if (!upgrades.contains(upgradeName)) {
                    upgrades.add(upgradeName);
                }
            }
        }
    }

    public void setChanged() {
        if (player == null || player.level().isClientSide) return;

        var cap = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (cap != null) {

            NonNullList<ItemStack> oldInventory = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
            NonNullList<ItemStack> newInventory = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

            SortOrder oldSort = stack.get(ModDataComponents.INVENTORY_SORT_ORDER);
            SortOrder newSort = (sortOrder != null) ? sortOrder : SortOrder.COUNT;
            sortOrder = newSort;

            for (int i = 0; i < cap.getSlots(); ++i) {
                oldInventory.set(i, cap.getStackInSlot(i));
            }

            for (int j = 0; j < itemHandler.getSlots(); ++j) {
                newInventory.set(j, itemHandler.getStackInSlot(j));
            }

            if (!newInventory.equals(oldInventory) || !newSort.equals(oldSort)) {
                refreshUpgrades();
                saveItemsToStack();
            }
        }
    }

}
