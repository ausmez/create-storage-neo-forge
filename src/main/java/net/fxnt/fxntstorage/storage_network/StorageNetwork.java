package net.fxnt.fxntstorage.storage_network;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.StorageNetworkSyncPacket;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StorageNetwork {
    private final StorageControllerEntity controller;
    @Nullable
    private Level level;
    private BlockPos controllerPos;
    private Set<BlockPos> components = new HashSet<>();
    private final NonNullList<StorageNetworkItem> boxes = NonNullList.create();
    private int networkVersion = 0;
    private int tickCount = 0;
    private Set<ItemStack> filterItems = new HashSet<>();

    private final IItemHandlerModifiable itemHandler = new NetworkItemHandler();

    public StorageNetwork(StorageControllerEntity controller) {
        this.controller = controller;
        this.level = controller.getLevel();
        this.controllerPos = controller.getBlockPos();
        if (this.level != null) {
            this.components = getConnectedComponents(this.level, this.controllerPos);
            getBoxes(this.level, this.components);
        }
    }

    public void tick() {
        if (level == null) {
            refreshStorageNetwork();
            return;
        }

        checkBoxes(); // Check if boxes removed every tick

        if (tickCount++ < ConfigManager.ServerConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) return;
        tickCount = 0;

        refreshStorageNetwork();
    }

    private void refreshStorageNetwork() {
        Level newLevel = this.controller.getLevel();
        if (newLevel == null) return;
        this.level = newLevel;
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

            Set<UUID> playersHighlighting = controller.getAllHighlightingPlayers();
            for (UUID playerId : playersHighlighting) {
                ServerPlayer player = Objects.requireNonNull(level.getServer()).getPlayerList().getPlayer(playerId);
                if (player != null) {
                    PacketDistributor.sendToPlayer(player, new StorageNetworkSyncPacket(controllerPos, newComponents));
                }
            }
        }

        getBoxes(this.level, this.components);

        Set<ItemStack> newFilterItems = getCurrentFilters();
        if (!areFilterSetsEqual(filterItems, newFilterItems)) {
            filterItems = newFilterItems;
        }
    }

    private boolean areFilterSetsEqual(Set<ItemStack> oldSet, Set<ItemStack> newSet) {
        if (oldSet.size() != newSet.size()) return false;

        Set<String> oldKeys = new HashSet<>();
        for (ItemStack s : oldSet) oldKeys.add(itemKey(s));
        for (ItemStack s : newSet) {
            if (!oldKeys.contains(itemKey(s))) return false;
        }
        return true;
    }

    private static String itemKey(ItemStack stack) {
        return stack.getItemHolder().getRegisteredName() + "@" + stack.getComponentsPatch().hashCode();
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

    public Set<BlockPos> getComponents() {
        return this.components;
    }

    private Set<ItemStack> getCurrentFilters() {
        Set<ItemStack> items = new HashSet<>();
        for (StorageNetworkItem box : boxes) {
            ItemStack filterItem = box.simpleStorageBoxEntity.getFilterItem();
            if (!filterItem.isEmpty())
                items.add(filterItem.copy());
        }
        return items;
    }

    private Set<BlockPos> getConnectedComponents(@Nullable Level level, BlockPos origin) {
        if (level == null) return new HashSet<>();

        int searchRange = ConfigManager.ServerConfig.SIMPLE_STORAGE_NETWORK_RANGE.get();
        Set<BlockPos> visited = new LinkedHashSet<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();

        visited.add(origin);
        frontier.add(origin);

        int sqRange = searchRange * searchRange;

        while (!frontier.isEmpty()) {
            BlockPos current = frontier.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor)
                        && isNetworkComponent(level.getBlockState(neighbor))
                        && squaredDistance(controllerPos, neighbor) <= sqRange) {
                    visited.add(neighbor);
                    frontier.add(neighbor);
                }
            }
        }
        return visited;
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
    }

    public void insertItems(ItemStack itemStack) {
        ItemStack remaining = itemStack.copy();

        while (!remaining.isEmpty()) {
            SimpleStorageBoxEntity targetBox = StorageNetwork.this.findBestTargetBox(remaining);
            if (targetBox == null) break;

            ItemStack beforeInsertion = remaining.copy();
            remaining = insertIntoBox(targetBox, remaining, 0, false);

            if (remaining.getCount() >= beforeInsertion.getCount()) {
                break;
            }
        }

        itemStack.setCount(remaining.getCount());
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    public List<StorageNetworkItem> getBoxes() {
        return Collections.unmodifiableList(boxes);
    }

    private boolean isNetworkComponent(BlockState blockState) {
        return blockState.is(ModTags.Blocks.STORAGE_NETWORK_BLOCK);
    }

    public boolean isItemInNetwork(ItemStack stack) {
        if (stack.isEmpty()) return false;

        for (ItemStack filterStack : filterItems) {
            if (ItemStack.isSameItemSameComponents(stack, filterStack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        if (slot < 0 || slot >= boxes.size()) return false;
        return boxes.get(slot).simpleStorageBoxEntity.getItemHandler().insertItem(0, itemStack, true).getCount() < itemStack.getCount();
    }

    public boolean canTakeItem(int slot, ItemStack itemStack) {
        return slot >= 0 && slot < boxes.size() && !itemStack.isEmpty();
    }

    private @Nullable SimpleStorageBoxEntity findBestTargetBox(ItemStack itemStack) {
        SimpleStorageBoxEntity emptyBox = null;
        Predicate<SimpleStorageBoxEntity> canAccept = box -> {
            int available = box.getMaxItemCapacity() - box.getStoredAmount();
            return available > 0 || box.hasVoidUpgrade();
        };

        // Find boxes that have matching filter set, or first empty box
        for (StorageNetworkItem networkItem : boxes) {
            SimpleStorageBoxEntity box = networkItem.simpleStorageBoxEntity;
            if (ItemStack.isSameItemSameComponents(box.getFilterItem(), itemStack) && canAccept.test(box)) {
                return box;
            } else if (box.getFilterItem().isEmpty() && canAccept.test(box) && emptyBox == null) {
                emptyBox = box;
            }
        }

        ScrollValueBehaviour behaviour = controller.getBehaviour(ScrollOptionBehaviour.TYPE);
        return (behaviour == null || behaviour.getValue() == 0) ? emptyBox : null;
    }

    private ItemStack insertIntoBox(SimpleStorageBoxEntity targetBox, ItemStack itemStack, int boxSlot, boolean simulate) {
        ItemStack remaining = itemStack.copy();
        ItemStack current = targetBox.getItemHandler().getStackInSlot(boxSlot);

        int available = targetBox.maxItemCapacity - targetBox.getStoredAmount();

        // Handle void upgrade
        if (available <= 0 && targetBox.hasVoidUpgrade())
            return ItemStack.EMPTY; // Void all items
        if (available <= 0 && !targetBox.hasVoidUpgrade())
            return itemStack; // No room

        if (current.isEmpty()) {
            // Inserting into empty slot
            int toInsert = Math.min(available, remaining.getCount());

            if (toInsert > 0 && !simulate) {
                ItemStack copy = remaining.copy();
                copy.setCount(toInsert);
                targetBox.getItemHandler().setStackInSlot(boxSlot, copy);
                targetBox.setFilter(copy);
                targetBox.setChanged();
            }

            remaining.shrink(toInsert);
        } else if (ItemStack.isSameItemSameComponents(current, remaining)) {
            // Inserting into slot with matching items
            int space = Math.min(available, targetBox.maxItemCapacity - current.getCount());
            int toInsert = Math.min(space, remaining.getCount());

            if (toInsert > 0) {
                if (!simulate) {
                    current.grow(toInsert);
                    targetBox.setChanged();
                }
                remaining.shrink(toInsert);
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
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
            return boxes.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= boxes.size())
                return ItemStack.EMPTY;
            return boxes.get(slot).simpleStorageBoxEntity.getItemHandler().getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
            if (slot < 0 || slot >= boxes.size())
                return itemStack;
            if (itemStack.isEmpty())
                return ItemStack.EMPTY;
            if (!canPlaceItem(slot, itemStack))
                return itemStack;

            int boxSlot = 0;

            // Find the best target box, ignoring the slot param
            SimpleStorageBoxEntity targetBox = StorageNetwork.this.findBestTargetBox(itemStack);
            if (targetBox == null) {
                return itemStack; // No suitable box found
            }

            // Insert into the selected box
            return StorageNetwork.this.insertIntoBox(targetBox, itemStack, boxSlot, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int boxSlot = 0;
            if (slot >= boxes.size())
                return ItemStack.EMPTY;

            SimpleStorageBoxEntity box = boxes.get(slot).simpleStorageBoxEntity;
            ItemStack current = box.getItemHandler().getStackInSlot(boxSlot);
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

            return box.getItemHandler().extractItem(boxSlot, toExtract, false);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= boxes.size()) return 0;
            return boxes.get(slot).simpleStorageBoxEntity.maxItemCapacity;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canPlaceItem(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot >= boxes.size()) return;
            boxes.get(slot).simpleStorageBoxEntity.getItemHandler().setStackInSlot(0, stack);
        }
    }
}
