package net.fxnt.fxntstorage.storage_network;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StorageNetwork {
    public final StorageControllerEntity controller;
    @Nullable
    public Level level;
    public BlockPos controllerPos;
    private final int searchRange = ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_RANGE.get();
    public int blankSlot = -1;
    public Set<BlockPos> components = new HashSet<>();
    public NonNullList<StorageNetworkItem> boxes = NonNullList.create();
    public int networkVersion = 0;
    private int tick = 0;

    private final IItemHandlerModifiable itemHandler = new NetworkItemHandler();

    public StorageNetwork(StorageControllerEntity controller) {
        this.controller = controller;
        this.level = controller.getLevel();
        this.controllerPos = controller.getBlockPos();
        this.components = getConnectedComponents(this.level, this.controllerPos);
        getBoxes(this.level, this.components);
    }

    public void tick() {
        checkBoxes(); // Check if boxes removed every tick
        moveNewItems(); // Move items from blank stack into a matching / empty box
        if (tick >= ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) {
            refreshStorageNetwork();
            tick = 0;
        }
        tick++;
    }

    private void moveNewItems() {
        ItemStack blankStack = itemHandler.getStackInSlot(blankSlot);
        if (!blankStack.isEmpty()) {
            this.insertItems(blankStack);
            blankStack.setCount(0);
        }
    }

    private void refreshStorageNetwork() {
        this.level = this.controller.getLevel();
        this.controllerPos = this.controller.getBlockPos();
        Set<BlockPos> newComponents = getConnectedComponents(this.level, this.controllerPos);

        // Check if components have changed
        if (!newComponents.equals(this.components)) {
            Set<BlockPos> oldComponents = new HashSet<>(this.components);

            // Remove newComponents from oldComponents (to find what has been removed)
            oldComponents.removeAll(newComponents);

            // For each removed component, check if it's a StorageInterface
            for (BlockPos removedPos : oldComponents) {
                BlockEntity blockEntity = this.level.getBlockEntity(removedPos);
                if (blockEntity instanceof StorageInterfaceEntity storageInterface) {
                    // The connection to the StorageInterface at this position was removed
                    storageInterface.forgetController();
                }
            }

            this.components = newComponents;
            this.networkVersion = (this.networkVersion + 1) % 1000;
        }

        getBoxes(this.level, this.components);
    }

    private void checkBoxes() {
        if (level == null) return;

        boolean networkChanged = false;
        for (StorageNetworkItem networkItem : boxes) {
            BlockEntity blockEntity = level.getBlockEntity(networkItem.blockPos);
            if (!(blockEntity instanceof SimpleStorageBoxEntity boxEntity) || !boxEntity.equals(networkItem.simpleStorageBoxEntity)) {
                networkChanged = true;
                break;
            }
        }
        if (networkChanged) {
            refreshStorageNetwork();
        }
    }

    private Set<BlockPos> getConnectedComponents(@Nullable Level level, BlockPos origin) {
        if (level == null) return new HashSet<>();

        List<BlockPos> positions = new ArrayList<>();
        positions.add(origin);

        int lastCheckedPos = 0;
        int distanceToController = 0;

        while (distanceToController < searchRange && lastCheckedPos < positions.size()) {
            for (int i = lastCheckedPos; i < positions.size(); i++) {
                BlockPos checkPos = positions.get(i);
                for (Direction direction : Direction.values()) {
                    BlockPos pos = checkPos.relative(direction);
                    if (isNetworkComponent(level.getBlockState(pos)) && squaredDistance(controllerPos, pos) <= searchRange * searchRange) {
                        if (!positions.contains(pos)) positions.add(pos);
                    }
                }
                lastCheckedPos = i;
            }
            lastCheckedPos++;
            distanceToController++;
        }

        return new HashSet<>(positions);
    }

    private int squaredDistance(BlockPos pos1, BlockPos pos2) {
        int dx = pos1.getX() - pos2.getX();
        int dy = pos1.getY() - pos2.getY();
        int dz = pos1.getZ() - pos2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void getBoxes(Level level, Set<BlockPos> components) {
        this.boxes.clear();

        int b = 0;
        for (BlockPos blockPos : components) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof SimpleStorageBoxEntity boxEntity) {
                StorageNetworkItem networkItem = new StorageNetworkItem(boxEntity);
                this.boxes.add(b, networkItem);
                b++;
            } else if (blockEntity instanceof StorageInterfaceEntity interfaceEntity) {
                interfaceEntity.setController(this.controller);
            }
        }

        // Add Blank Slot to void items
        this.blankSlot = boxes.size() * 2;
    }

    public void insertItems(ItemStack itemStack) {
        List<SimpleStorageBoxEntity> emptyBoxes = new ArrayList<>();
        boolean boxFound = false;

        for (StorageNetworkItem networkItem : boxes) {
            SimpleStorageBoxEntity blockEntity = networkItem.simpleStorageBoxEntity;
            if (blockEntity.getFilterItem().isEmpty()) {
                if (!emptyBoxes.contains(blockEntity)) emptyBoxes.add(blockEntity);
            } else if (blockEntity.filterTest(itemStack)) {
                ItemStack remaining = blockEntity.insertItems(itemStack);
                if (remaining.isEmpty())
                    boxFound = true;
            }
        }
        if (!boxFound && itemStack.getCount() > 0) {
            for (SimpleStorageBoxEntity box : emptyBoxes) {
                itemStack = box.insertItems(itemStack);
            }
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    private boolean isNetworkComponent(BlockState blockState) {
        return blockState.is(ModTags.Blocks.STORAGE_NETWORK_BLOCK);
    }

    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        if (slot == blankSlot) return boxes.stream().anyMatch(box -> box.simpleStorageBoxEntity.hasVoidUpgrade());
        int boxIndex = slot / 2;
        int boxSlot = slot % 2;
        if (boxIndex >= boxes.size()) return false;
        return boxes.get(boxIndex).simpleStorageBoxEntity.canPlaceItem(boxSlot, itemStack);
    }

    public boolean canTakeItem(int slot, ItemStack itemStack) {
        if (slot == blankSlot) return false;
        int boxIndex = slot / 2;
        int boxSlot = slot % 2;
        if (boxIndex >= boxes.size()) return false;
        return boxes.get(boxIndex).simpleStorageBoxEntity.canTakeItem(boxes.get(boxIndex).simpleStorageBoxEntity, boxSlot, itemStack);
    }

    public static class StorageNetworkItem {
        public final SimpleStorageBoxEntity simpleStorageBoxEntity;
        public final BlockPos blockPos;

        public StorageNetworkItem(SimpleStorageBoxEntity entity) {
            this.simpleStorageBoxEntity = entity;
            this.blockPos = entity.getBlockPos();
        }
    }

    private class NetworkItemHandler implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return boxes.size() * 2 + 1; // 2 slots per box + 1 blank
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == blankSlot) return ItemStack.EMPTY;

            int boxIndex = slot / 2;
            int boxSlot = slot % 2;
            if (boxIndex >= boxes.size()) return ItemStack.EMPTY;

            return boxes.get(boxIndex).simpleStorageBoxEntity.getItem(boxSlot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
            if (itemStack.isEmpty()) return ItemStack.EMPTY;
            if (!canPlaceItem(slot, itemStack)) return itemStack;

            ItemStack remaining = itemStack.copy();

            int boxIndex = slot / 2;
            int boxSlot = slot % 2;
            if (boxIndex >= boxes.size()) return itemStack;

            SimpleStorageBoxEntity targetBox = boxes.get(boxIndex).simpleStorageBoxEntity;
            ItemStack current = targetBox.getItem(boxSlot);

            if (!ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_FILL_EMPTY.get()) {
                // If target is empty, make sure NO other box wants this item
                if (targetBox.getFilterItem().isEmpty()) {
                    for (StorageNetworkItem networkItem : boxes) {
                        SimpleStorageBoxEntity box = networkItem.simpleStorageBoxEntity;
                        if (box == targetBox) continue;

                        if (!box.getFilterItem().isEmpty() && box.filterTest(itemStack)) {
                            // Another box is filtered to accept this item
                            return itemStack;
                        }
                    }
                }
            }

            int available = targetBox.maxItemCapacity - targetBox.getStoredAmount();
            if (available <= 0 && targetBox.hasVoidUpgrade())
                return ItemStack.EMPTY; // Void items
            if (available <= 0 && !targetBox.hasVoidUpgrade())
                return itemStack; // No room

            if (current.isEmpty()) {
                int toInsert = Math.min(available, remaining.getCount());

                if (toInsert > 0 && !simulate) {
                    ItemStack copy = remaining.copy();
                    copy.setCount(toInsert);
                    targetBox.setItem(boxSlot, copy);
                }

                remaining.shrink(toInsert);
            } else if (ItemStack.isSameItemSameTags(current, remaining)) {
                int space = Math.min(available, targetBox.getMaxStackSize() - current.getCount());
                int toInsert = Math.min(space, remaining.getCount());

                if (toInsert > 0) {
                    if (!simulate) {
                        current.grow(toInsert);
                        targetBox.setItem(boxSlot, current);
                    }
                    remaining.shrink(toInsert);
                }
            }

            return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == blankSlot) return ItemStack.EMPTY;

            int boxIndex = slot / 2;
            int boxSlot = slot % 2;
            if (boxIndex >= boxes.size()) return ItemStack.EMPTY;

            SimpleStorageBoxEntity box = boxes.get(boxIndex).simpleStorageBoxEntity;
            ItemStack current = box.getItem(boxSlot);
            if (current.isEmpty()) return ItemStack.EMPTY;
            if (!canTakeItem(slot, current)) return ItemStack.EMPTY;

            int toExtract = Math.min(current.getCount(), amount);
            if (simulate) {
                if (toExtract == 0) {
                    return ItemStack.EMPTY;
                } else {
                    ItemStack copy = current.copy();
                    copy.setCount(toExtract);
                    return copy;
                }
            }

            return box.removeItem(boxSlot, toExtract);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) return boxes.get(0).simpleStorageBoxEntity.maxItemCapacity;
            return Math.min(64, getStackInSlot(slot).getMaxStackSize());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canPlaceItem(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot == blankSlot) return;

            int boxIndex = slot / 2;
            int boxSlot = slot % 2;
            if (boxIndex >= boxes.size()) return;

            boxes.get(boxIndex).simpleStorageBoxEntity.setItem(boxSlot, stack);
        }
    }

}
