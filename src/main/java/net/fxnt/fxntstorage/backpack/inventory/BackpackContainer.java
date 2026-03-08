package net.fxnt.fxntstorage.backpack.inventory;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataManager;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SetActivePanelPacket;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.network.packet.SyncNBTDataPacket;
import net.fxnt.fxntstorage.network.packet.UpgradeDataPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BackpackContainer implements IBackpackContainer, ICapabilityProvider, IItemHandlerModifiable {
    private final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    private final int CONTAINER_SIZE = layout.getTotalSlots();

    private final Player player;
    private final ItemStack stack;
    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE);
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private final NonNullList<String> upgrades = NonNullList.create();

    private int stackMultiplier;
    private SortOrder sortOrder = SortOrder.COUNT;

    // Unified upgrade data management
    private final UpgradeDataManager upgradeData = new UpgradeDataManager();

    public BackpackContainer(Player player, ItemStack itemStack) {
        this.player = player;
        this.stack = itemStack;

        if (itemStack.getItem() instanceof BackpackItem backpackItem &&
                backpackItem.getBlock() instanceof BackpackBlock backpackBlock) {
            stackMultiplier = backpackBlock.getStackMultiplier();
        }
        loadItemsFromStack(itemStack);
    }

    public void setTag(CompoundTag tag) {
        if (!player.level().isClientSide) return;
        stack.setTag(tag);
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        CompoundTag blockEntityTag = itemStack.getTagElement("BlockEntityTag");

        if (blockEntityTag != null && blockEntityTag.contains("Items")) {
            ListTag listTag = blockEntityTag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag tag = listTag.getCompound(i);
                int slot = tag.getByte("Slot") & 255;

                // Create an ItemStack
                ItemStack slotStack = ItemStack.of(tag);

                // Check for ActualCount and set count accordingly
                if (tag.contains("ActualCount", Tag.TAG_INT)) {
                    int actualCount = tag.getInt("ActualCount");
                    slotStack.setCount(Math.max(actualCount, 0));
                }

                if (slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, slotStack);
                }
            }

            upgrades.clear();
            ListTag upgradesList = blockEntityTag.getList("Upgrades", Tag.TAG_STRING);
            for (int i = 0; i < upgradesList.size(); i++) {
                upgrades.add(i, upgradesList.getString(i));
            }

            if (blockEntityTag.contains("StackMultiplier", Tag.TAG_INT)) {
                stackMultiplier = blockEntityTag.getInt("StackMultiplier");
            }

            sortOrder = (blockEntityTag.contains("SortOrder", Tag.TAG_STRING))
                    ? SortOrder.valueOf(blockEntityTag.getString("SortOrder"))
                    : SortOrder.COUNT;

            // Load upgrade data (panels and settings) and copy into our manager
            UpgradeDataManager loadedData = UpgradeDataManager.loadFromItem(itemStack);
            upgradeData.copyFrom(loadedData);
        }
    }

    public CompoundTag saveItemsToStack() {
        CompoundTag tag = new CompoundTag();

        // Save items
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.putInt("ActualCount", itemHandler.getStackInSlot(i).getCount());
                itemHandler.getStackInSlot(i).save(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", itemHandler.getSlots());
        tag.put("Items", nbt);

        // Save upgrades
        ListTag upgradesList = new ListTag();
        for (int i = 0; i < upgrades.size(); ++i) {
            upgradesList.add(i, StringTag.valueOf(upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("StackMultiplier", stackMultiplier);
        tag.putString("SortOrder", sortOrder.name());

        upgradeData.saveToItem(stack);
        return tag;
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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(capability, lazyItemHandler);
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
            ModNetwork.sendToServer(new SetSortOrderPacket(sortOrder));
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
            ModNetwork.sendToServer(new SetActivePanelPacket(type));
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
    public int getExpandedPanelsBitmask() {
        return upgradeData.getExpandedPanelsBitmask();
    }

    @Override
    public void setExpandedPanelsBitmask(int mask) {
        upgradeData.setExpandedPanelsBitmask(mask);
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
            ModNetwork.sendToServer(new UpgradeDataPacket(setting.getIndex(), value));
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
    public ItemStackHandler getItemHandler() {
        return itemHandler;
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
        if (player == null) {
            FXNTStorage.LOGGER.warn("setDataChanged called with null player - skipping");
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            FXNTStorage.LOGGER.warn("setDataChanged called with non-server player: {}", player.getClass().getSimpleName());
            return;
        }
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
        if (!(player instanceof ServerPlayer)) return;

        NonNullList<ItemStack> oldItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        NonNullList<ItemStack> newItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");

        if (blockEntityTag == null) {
            CompoundTag tag = new CompoundTag();
            ContainerHelper.saveAllItems(tag, NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY));
            ItemStack itemStack1 = stack;
            itemStack1.addTagElement("BlockEntityTag", tag);
            blockEntityTag = BlockItem.getBlockEntityData(itemStack1);
        } else {
            ListTag listTag = blockEntityTag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag tag = listTag.getCompound(i);
                int slot = tag.getByte("Slot") & 255;

                ItemStack slotStack = ItemStack.of(tag);

                if (tag.contains("ActualCount", Tag.TAG_INT)) {
                    int actualCount = tag.getInt("ActualCount");
                    slotStack.setCount(Math.max(actualCount, 0));
                }

                if (slot < oldItemStacks.size()) {
                    oldItemStacks.set(slot, slotStack);
                }
            }
        }

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            newItemStacks.set(i, itemHandler.getStackInSlot(i));
        }

        SortOrder oldSort = (blockEntityTag != null && blockEntityTag.contains("SortOrder")) ? SortOrder.valueOf(blockEntityTag.getString("SortOrder")) : SortOrder.COUNT;
        SortOrder newSort = (sortOrder != null) ? sortOrder : SortOrder.COUNT;
        sortOrder = newSort;

        boolean upgradeDirty = upgradeData.isDirty();
        if (upgradeDirty)
            upgradeData.saveToItem(stack);

        if (!itemStacksAreSame(oldItemStacks, newItemStacks) || !newSort.equals(oldSort) || upgradeDirty) {
            refreshUpgrades();
            CompoundTag tag = saveItemsToStack();
            stack.getOrCreateTag().put("BlockEntityTag", tag);
            ModNetwork.sendToPlayer((ServerPlayer) player, new SyncNBTDataPacket(stack));
        }
    }

    private static boolean itemStacksAreSame(NonNullList<ItemStack> oldStack, NonNullList<ItemStack> newStack) {
        if (oldStack.size() != newStack.size()) return false;

        for (int i = 0; i < oldStack.size(); ++i) {
            if (!ItemStack.matches(oldStack.get(i), newStack.get(i))) {
                return false;
            }
        }
        return true;
    }
}
