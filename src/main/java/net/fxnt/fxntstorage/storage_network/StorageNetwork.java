package net.fxnt.fxntstorage.storage_network;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.StorageNetworkSyncPacket;
import net.fxnt.fxntstorage.simple_storage.CompactingItemHandler;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StorageNetwork {
    private final StorageControllerEntity controller;
    @Nullable
    private Level level;
    private BlockPos controllerPos;
    private Set<BlockPos> components = new HashSet<>();
    private final NonNullList<StorageNetworkItem> boxes = NonNullList.create();

    private final List<SlotMapping> slotMappings = new ArrayList<>();
    private int networkVersion = 0;
    private int tickCount = 0;
    private Set<ItemStack> filterItems = new HashSet<>();

    private final IItemHandlerModifiable itemHandler = new NetworkItemHandler();

    private record SlotMapping(int boxIndex, int tierSlot) {}

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

        checkBoxes();

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

        if (!newComponents.equals(this.components)) {
            Set<BlockPos> oldComponents = new HashSet<>(this.components);
            oldComponents.removeAll(newComponents);

            for (BlockPos removedPos : oldComponents) {
                BlockEntity blockEntity = this.level.getBlockEntity(removedPos);
                if (blockEntity instanceof StorageInterfaceEntity storageInterface) {
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
            SimpleStorageBoxEntity entity = box.simpleStorageBoxEntity;
            if (entity.compactingUpgrade && entity.compactingChain != null) {
                for (Item item : entity.compactingChain.acceptedItems()) items.add(new ItemStack(item));
            } else {
                ItemStack filterItem = entity.getFilterItem();
                if (!filterItem.isEmpty()) items.add(filterItem.copy());
            }
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

        rebuildSlotMappings();
    }

    private void rebuildSlotMappings() {
        slotMappings.clear();
        for (int i = 0; i < boxes.size(); i++) {
            SimpleStorageBoxEntity box = boxes.get(i).simpleStorageBoxEntity;
            int tierCount = (box.compactingUpgrade && box.compactingChain != null)
                    ? box.compactingChain.tiers() : 1;
            for (int s = 0; s < tierCount; s++) {
                slotMappings.add(new SlotMapping(i, s));
            }
        }
    }

    private IItemHandler networkHandlerFor(int boxIndex) {
        SimpleStorageBoxEntity box = boxes.get(boxIndex).simpleStorageBoxEntity;
        if (box.compactingUpgrade && box.compactingChain != null) {
            return box.getCapabilityHandler();
        }
        return box.getItemHandler();
    }

    public void insertItems(ItemStack itemStack) {
        insertItems(itemStack, true);
    }

    public void insertItems(ItemStack itemStack, boolean allowEmpty) {
        ItemStack remaining = itemStack.copy();

        while (!remaining.isEmpty()) {
            SimpleStorageBoxEntity targetBox = findBestTargetBox(remaining, Map.of(), Map.of(), allowEmpty);
            if (targetBox == null) break;

            ItemStack beforeInsertion = remaining.copy();
            remaining = insertIntoBox(targetBox, remaining, false);

            if (remaining.getCount() >= beforeInsertion.getCount()) {
                break;
            }
        }

        itemStack.setCount(remaining.getCount());
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    public boolean unpackItems(Level level, BlockPos pos, Direction side, List<ItemStack> items, boolean allowEmpty) {
        boolean allStored = true;
        for (ItemStack itemStack : items) {
            ItemStack remaining = itemStack.copy();
            insertItems(remaining, allowEmpty);
            if (remaining.isEmpty()) continue;

            allStored = false;
            Block.popResourceFromFace(level, pos.relative(side), side, remaining);
        }
        return allStored;
    }

    // One routing pass into the best box, as an item handler insert (callers re-insert any remainder)
    public ItemStack insertItem(ItemStack itemStack, boolean simulate, boolean allowEmpty) {
        if (itemStack.isEmpty()) return ItemStack.EMPTY;

        SimpleStorageBoxEntity targetBox = findBestTargetBox(itemStack, Map.of(), Map.of(), allowEmpty);
        if (targetBox == null) return itemStack;

        return insertIntoBox(targetBox, itemStack, simulate);
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
        if (slot < 0 || slot >= slotMappings.size() || itemStack.isEmpty()) return false;
        return findBestTargetBox(itemStack) != null;
    }

    public boolean canTakeItem(int slot, ItemStack itemStack) {
        return slot >= 0 && slot < slotMappings.size() && !itemStack.isEmpty();
    }

    private @Nullable SimpleStorageBoxEntity findBestTargetBox(ItemStack itemStack) {
        return findBestTargetBox(itemStack, Map.of(), Map.of(), true);
    }

    private @Nullable SimpleStorageBoxEntity findBestTargetBox(ItemStack itemStack,
                                                               Map<SimpleStorageBoxEntity, Integer> reservedUnits,
                                                               Map<SimpleStorageBoxEntity, ItemStack> claimedFilters,
                                                               boolean allowEmpty) {
        SimpleStorageBoxEntity emptyBox = null;
        SimpleStorageBoxEntity voidBox = null;

        for (StorageNetworkItem networkItem : boxes) {
            SimpleStorageBoxEntity box = networkItem.simpleStorageBoxEntity;
            boolean hasRealSpace = fitCount(box, itemStack, reservedUnits.getOrDefault(box, 0)) > 0;
            ItemStack filter = claimedFilters.getOrDefault(box, box.getFilterItem());

            if (box.compactingUpgrade && box.compactingChain != null) {
                if (acceptsCompactingItem(box, itemStack)) {
                    if (hasRealSpace) return box;
                    if (box.hasVoidUpgrade() && voidBox == null) voidBox = box;
                }
            } else if (ItemStack.isSameItemSameComponents(filter, itemStack)) {
                if (hasRealSpace) return box;
                if (box.hasVoidUpgrade() && voidBox == null) voidBox = box;
            } else if (filter.isEmpty() && hasRealSpace && emptyBox == null) {
                emptyBox = box;
            }
        }

        // A matching void box is the item's home, so it voids overflow rather than claiming an empty box
        if (voidBox != null) return voidBox;

        return allowEmpty && emptyFillAllowed() ? emptyBox : null;
    }

    // The controller's "fill empty storage" option
    private boolean emptyFillAllowed() {
        ScrollValueBehaviour behaviour = controller.getBehaviour(ScrollOptionBehaviour.TYPE);
        return behaviour == null || behaviour.getValue() == 0;
    }

    private int fitCount(SimpleStorageBoxEntity box, ItemStack itemStack, int reservedUnits) {
        int free = box.getMaxItemCapacity() - box.getStoredAmount() - reservedUnits;
        if (free <= 0) return 0;
        if (box.compactingUpgrade && box.compactingChain != null) {
            int unitsPerItem = box.compactingChain.toT0Units(itemStack.getItem(), 1);
            return unitsPerItem > 0 ? free / unitsPerItem : 0;
        }
        return free;
    }

    private int unitsPerItem(SimpleStorageBoxEntity box, ItemStack itemStack) {
        if (box.compactingUpgrade && box.compactingChain != null) {
            return Math.max(1, box.compactingChain.toT0Units(itemStack.getItem(), 1));
        }
        return 1;
    }

    public boolean canInsertAll(List<ItemStack> items) {
        return canInsertAll(items, true);
    }

    public boolean canInsertAll(List<ItemStack> items, boolean allowEmpty) {
        Map<SimpleStorageBoxEntity, Integer> reservedUnits = new IdentityHashMap<>();
        Map<SimpleStorageBoxEntity, ItemStack> claimedFilters = new IdentityHashMap<>();

        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;
            int remaining = itemStack.getCount();

            while (remaining > 0) {
                SimpleStorageBoxEntity box = findBestTargetBox(itemStack, reservedUnits, claimedFilters, allowEmpty);
                if (box == null) return false;

                int reserved = reservedUnits.getOrDefault(box, 0);
                int toInsert = Math.min(remaining, fitCount(box, itemStack, reserved));
                if (toInsert <= 0) {
                    // Only a full void box is returned without space; it destroys the rest
                    if (box.hasVoidUpgrade()) break;
                    return false;
                }

                reservedUnits.put(box, reserved + toInsert * unitsPerItem(box, itemStack));
                if (box.getFilterItem().isEmpty() && !(box.compactingUpgrade && box.compactingChain != null)) {
                    claimedFilters.putIfAbsent(box, itemStack.copyWithCount(1));
                }
                remaining -= toInsert;
            }
        }
        return true;
    }

    private boolean acceptsCompactingItem(SimpleStorageBoxEntity box, ItemStack itemStack) {
        IItemHandler handler = box.getCapabilityHandler();
        for (int s = 0; s < handler.getSlots(); s++) {
            if (handler.isItemValid(s, itemStack)) return true;
        }
        return false;
    }

    private ItemStack insertIntoBox(SimpleStorageBoxEntity targetBox, ItemStack itemStack, boolean simulate) {
        if (targetBox.compactingUpgrade && targetBox.compactingChain != null) {
            IItemHandler handler = targetBox.getCapabilityHandler();
            for (int s = 0; s < handler.getSlots(); s++) {
                if (handler.isItemValid(s, itemStack)) {
                    return handler.insertItem(s, itemStack, simulate);
                }
            }
            return itemStack;
        }

        ItemStack remaining = itemStack.copy();
        ItemStack current = targetBox.getItemHandler().getStackInSlot(0);

        int available = targetBox.maxItemCapacity - targetBox.getStoredAmount();

        if (available <= 0 && targetBox.hasVoidUpgrade())
            return ItemStack.EMPTY;
        if (available <= 0)
            return itemStack;

        if (current.isEmpty()) {
            int toInsert = Math.min(available, remaining.getCount());

            if (toInsert > 0 && !simulate) {
                ItemStack copy = remaining.copyWithCount(toInsert);
                targetBox.getItemHandler().setStackInSlot(0, copy);
                targetBox.setFilter(copy);
                targetBox.setChanged();
            }

            remaining.shrink(toInsert);
        } else if (ItemStack.isSameItemSameComponents(current, remaining)) {
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
            return slotMappings.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= slotMappings.size()) return ItemStack.EMPTY;
            SlotMapping mapping = slotMappings.get(slot);
            return networkHandlerFor(mapping.boxIndex()).getStackInSlot(mapping.tierSlot());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
            if (slot < 0 || slot >= slotMappings.size()) return itemStack;
            return StorageNetwork.this.insertItem(itemStack, simulate, true);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= slotMappings.size()) return ItemStack.EMPTY;
            SlotMapping mapping = slotMappings.get(slot);
            IItemHandler handler = networkHandlerFor(mapping.boxIndex());
            ItemStack current = handler.getStackInSlot(mapping.tierSlot());
            if (current.isEmpty()) return ItemStack.EMPTY;
            if (!canTakeItem(slot, current)) return ItemStack.EMPTY;
            int toExtract = Math.min(current.getCount(), amount);
            return handler.extractItem(mapping.tierSlot(), toExtract, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= slotMappings.size()) return 0;
            SlotMapping mapping = slotMappings.get(slot);
            return networkHandlerFor(mapping.boxIndex()).getSlotLimit(mapping.tierSlot());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canPlaceItem(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < 0 || slot >= slotMappings.size()) return;
            SlotMapping mapping = slotMappings.get(slot);
            SimpleStorageBoxEntity box = boxes.get(mapping.boxIndex()).simpleStorageBoxEntity;
            if (box.compactingUpgrade && box.compactingChain != null) {
                // Tier slots are views over one T0 count, so set the tier rather than raw slot 0
                new CompactingItemHandler(box, box.compactingChain).setStackInSlot(mapping.tierSlot(), stack);
            } else {
                box.getItemHandler().setStackInSlot(0, stack);
            }
        }
    }
}
