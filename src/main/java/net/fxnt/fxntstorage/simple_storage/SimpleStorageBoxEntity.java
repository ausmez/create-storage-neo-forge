package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SimpleStorageBoxEntity extends BaseContainerBlockEntity implements ThresholdSwitchObservable {
    private int tickCount = 0;

    public static final int BASE_CAPACITY = 32;
    public static final int ITEM_STACK_SIZE = 64;
    public int maxItemCapacity = ITEM_STACK_SIZE * BASE_CAPACITY;
    public int storedAmount = 0;
    public boolean voidUpgrade = false;

    /*
        Slot0 = Total item count
        Slot1 = Void Upgrade Item slot
        Slot2-10 = Capacity Upgrade Item slots
     */
    public static final int VOID_UPGRADE_SLOT = 1;
    public static final int CAPACITY_UPGRADE_SLOT_START = 2;
    public static final int MAX_CAPACITY_UPGRADES = 9;
    public static final int BASE_SLOT_COUNT = 2;

    public static int SLOT_COUNT = BASE_SLOT_COUNT + MAX_CAPACITY_UPGRADES; // Item Slot + Void Upgrade Slot + Capacity Upgrade Slots
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler = createItemHandler(SLOT_COUNT);
    private LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.empty();

    public SimpleStorageBoxEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.lazyItemHandler.invalidate();
    }

    public @NotNull ItemStackHandler createItemHandler(int slotCount) {
        return new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                SimpleStorageBoxEntity.this.setChanged();
            }

            @Override
            public CompoundTag serializeNBT() {
                ListTag nbtTagList = new ListTag();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putInt("Slot", i);
                        itemTag.putInt("ActualCount", stacks.get(i).getCount());
                        stacks.get(i).save(itemTag);
                        nbtTagList.add(itemTag);
                    }
                }
                CompoundTag nbt = new CompoundTag();
                nbt.put("Items", nbtTagList);
                nbt.putInt("Size", stacks.size());
                return nbt;
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
                ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < tagList.size(); i++) {
                    CompoundTag itemTags = tagList.getCompound(i);
                    int slot = itemTags.getInt("Slot");
                    ItemStack slotStack = ItemStack.of(itemTags);
                    if (itemTags.contains("ActualCount", Tag.TAG_INT)) {
                        slotStack.setCount(itemTags.getInt("ActualCount"));
                    }
                    stacks.set(slot, slotStack);
                }
                onLoad();
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                ItemStack amount = super.insertItem(slot, stack, simulate);
                if (storedAmount >= maxItemCapacity && voidUpgrade) {
                    return ItemStack.EMPTY;
                }
                return amount;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if ((isPlayerInteraction && slot >= VOID_UPGRADE_SLOT) || slot <= 1) {
                    if (amount == 0) {
                        return ItemStack.EMPTY;
                    } else {
                        this.validateSlotIndex(slot);
                        ItemStack existing = this.stacks.get(slot);
                        if (existing.isEmpty()) {
                            return ItemStack.EMPTY;
                        } else {
                            int toExtract = Math.min(amount, maxItemCapacity);
                            if (existing.getCount() <= toExtract) {
                                if (!simulate) {
                                    this.stacks.set(slot, ItemStack.EMPTY);
                                    this.onContentsChanged(slot);
                                    return existing;
                                } else {
                                    return existing.copy();
                                }
                            } else {
                                if (!simulate) {
                                    this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                                    this.onContentsChanged(slot);
                                }

                                return existing.copyWithCount(toExtract);
                            }
                        }
                    }
                }
                return ItemStack.EMPTY;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (stack.is(ModTags.Items.STORAGE_BOX_UPGRADE) && !isPlayerInteraction) return false;
                if (filterTest(stack)) {
                    if (slot > 0) return voidUpgrade;
                    if (voidUpgrade) return true;
                    return this.stacks.get(0).getCount() < maxItemCapacity;
                }
                return false;
            }

            @Override
            protected int getStackLimit(int slot, @NotNull ItemStack stack) {
                if (slot == 0) return maxItemCapacity;
                return super.getStackLimit(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                if (slot == 0) return maxItemCapacity;
                return super.getSlotLimit(slot);
            }
        };
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getCapacityUpgrades() {
        int upgradeCount = 0;
        for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
            if (this.itemHandler.getStackInSlot(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                upgradeCount++;
            }
        }
        return upgradeCount;
    }

    public boolean hasVoidUpgrade() {
        this.voidUpgrade = this.itemHandler.getStackInSlot(VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
        return this.voidUpgrade;
    }

    public int getStoredAmount() {
        this.storedAmount = this.itemHandler.getStackInSlot(0).getCount();
        return this.storedAmount;
    }

    public int getMaxItemCapacity() {
        int capacity = BASE_CAPACITY << getCapacityUpgrades();
        int stackSize = filterItem.isEmpty() ? 64 : filterItem.getMaxStackSize();

        this.maxItemCapacity = capacity * stackSize;
        return this.maxItemCapacity;
    }

    public ItemStack getFilterItem() {
        return this.filterItem;
    }

    public void setPlayerInteraction(boolean isPlayer) {
        this.isPlayerInteraction = isPlayer;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.fxntstorage.simple_storage_box_title");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", itemHandler.serializeNBT());
        tag.putInt("MaxItemCapacity", this.getMaxItemCapacity()); // Needed for MountedStorage
        tag.putInt("StoredAmount", this.getStoredAmount());
        tag.putBoolean("VoidUpgrade", this.hasVoidUpgrade()); // Needed for MountedStorage
        tag.put("FilterItem", filterItem.copyWithCount(1).save(new CompoundTag()));
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.maxItemCapacity = tag.getInt("MaxItemCapacity"); // Needed for MountedStorage
        this.storedAmount = tag.getInt("StoredAmount");
        this.voidUpgrade = tag.getBoolean("VoidUpgrade"); // Needed for MountedStorage
        CompoundTag filterTag = tag.getCompound("FilterItem");
        this.filterItem = (filterTag.isEmpty()) ? ItemStack.EMPTY : ItemStack.of(filterTag);

        CompoundTag itemsTag = tag.getCompound("Items");
        int oldSize = itemsTag.getInt("Size");

        // --- Slot layout migration check (1.1.2)
        if (oldSize != SLOT_COUNT) {
            FXNTStorage.LOGGER.debug("Migrating slot layout from previous version of Simple Storage Box at {}", worldPosition);
            if (oldSize == 0) {
                // Slot layout does not contain a Size tag
                ListTag existingItems = tag.getList("Items", Tag.TAG_COMPOUND);
                oldSize = tag.getInt("slotCount");
                CompoundTag newTag = new CompoundTag();

                newTag.putInt("Size", oldSize);
                newTag.put("Items", existingItems);
                itemsTag = newTag;

                this.maxItemCapacity = tag.getInt("maxItemCapacity");
                this.storedAmount = tag.getInt("storedAmount");
                this.voidUpgrade = tag.getBoolean("voidUpgrade");
            }

            migrateSlotItems(itemsTag, oldSize); // Slot layout migration
        } else {
            this.itemHandler.deserializeNBT(tag.getCompound("Items"));
        }
    }

    private void migrateSlotItems(CompoundTag itemsTag, int oldSize) {
        ItemStackHandler oldHandler = createItemHandler(oldSize);
        oldHandler.deserializeNBT(itemsTag);
        migrateSlotItems(oldHandler);
    }

    private void migrateSlotItems(ItemStackHandler oldHandler) {
        // --- Old Slot0 + Slot1 -> New Slot0
        ItemStack slot0 = oldHandler.getStackInSlot(0);
        ItemStack slot1 = oldHandler.getStackInSlot(1);

        if (!slot0.isEmpty() || !slot1.isEmpty()) {
            ItemStack merged = slot0.copy();

            int totalCount = slot0.getCount() + slot1.getCount();
            merged.setCount(Math.min(totalCount, maxItemCapacity));

            this.itemHandler.setStackInSlot(0, merged);
        }

        // --- Old Slot3 -> New Slot1
        this.itemHandler.setStackInSlot(1, oldHandler.getStackInSlot(3).copy());

        // --- Old Slot4-12 -> New Slot2-10
        for (int oldSlot = 4; oldSlot <= 12; oldSlot++) {
            int newSlot = (oldSlot - 4) + 2;
            if (newSlot < this.itemHandler.getSlots()) {
                this.itemHandler.setStackInSlot(newSlot, oldHandler.getStackInSlot(oldSlot).copy());
            }
        }
        this.setChanged();
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void forceTick() {
        tickCount = 999;
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        // Update StoredAmount and MaxItemCapacity
        getStoredAmount();
        getMaxItemCapacity();

        ItemStack slot0 = this.itemHandler.getStackInSlot(0);

        // Set filter item to items inside to prevent wrong items being put in
        if (!slot0.isEmpty() && !ItemStack.isSameItemSameTags(slot0, filterItem)) {
            setFilter(slot0);
        }

        if (tickCount++ < ConfigManager.CommonConfig.STORAGE_BOX_UPDATE_TIME.get()) return;
        tickCount = 0;

        updateBlockState(level);
    }

    private boolean isContainerModified() {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return hasCustomName();
    }

    private void updateBlockState(Level level) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        BlockState currentState = getBlockState();
        EnumProperties.StorageUsed status = EnumProperties.StorageUsed.EMPTY;
        int stored = getStoredAmount();

        if (stored >= getMaxItemCapacity()) status = EnumProperties.StorageUsed.FULL;
        else if (stored > 0) status = EnumProperties.StorageUsed.HAS_ITEMS;

        if (currentState.getValue(SimpleStorageBox.STORAGE_USED) != status) {
            level.setBlock(worldPosition, currentState.setValue(SimpleStorageBox.STORAGE_USED, status), Block.UPDATE_ALL);
        }

        boolean copyNbt = isContainerModified();
        if (currentState.getValue(SimpleStorageBox.COPY_NBT) != copyNbt) {
            level.setBlock(worldPosition, currentState.setValue(SimpleStorageBox.COPY_NBT, copyNbt), Block.UPDATE_ALL);
        }
    }

    public void transferToStorage(@NotNull Player pPlayer, Boolean transferAll) {
        // Get the item in the players main hand and check the hand is NOT empty and the item matches the filter (if one applied)
        ItemStack itemInHand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (itemInHand.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {

            if (itemInHand.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())) {
                if (!this.hasVoidUpgrade()) {
                    this.itemHandler.setStackInSlot(VOID_UPGRADE_SLOT, itemInHand.copyWithCount(1));
                    if (!pPlayer.isCreative()) {
                        itemInHand.shrink(1);
                        pPlayer.getInventory().setChanged();
                    }
                } else {
                    ItemStack voidStack = this.itemHandler.getStackInSlot(VOID_UPGRADE_SLOT);
                    int slot = pPlayer.getInventory().getSlotWithRemainingSpace(voidStack);
                    if (slot > -1) {
                        pPlayer.getInventory().getItem(slot).grow(1);
                        pPlayer.getInventory().setChanged();
                    } else {
                        slot = pPlayer.getInventory().getFreeSlot();
                        if (slot > -1) {
                            pPlayer.getInventory().setItem(slot, voidStack);
                            pPlayer.getInventory().setChanged();
                        } else {
                            pPlayer.drop(voidStack, false);
                        }
                    }
                    this.itemHandler.setStackInSlot(VOID_UPGRADE_SLOT, ItemStack.EMPTY);
                }
            } else if (itemInHand.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                boolean canAddUpgrade = false;
                for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
                    if (this.itemHandler.getStackInSlot(i).isEmpty()) {
                        this.itemHandler.setStackInSlot(i, itemInHand.copyWithCount(1));
                        canAddUpgrade = true;
                        break;
                    }
                }
                if (!pPlayer.isCreative() && canAddUpgrade) {
                    itemInHand.shrink(1);
                    pPlayer.getInventory().setChanged();
                } else if (!canAddUpgrade) {
                    pPlayer.displayClientMessage(Component.translatable("fxntstorage.storage_box_capacity_upgrade_max"), true);
                }
            }
        }

        /*
            Single-click: add entire stack in main hand
            double-click: add every stack in player inventory matching item filter
         */
        if (transferAll) {

            for (int i = 0; i < pPlayer.getInventory().getContainerSize(); i++) {
                ItemStack playerStack = pPlayer.getInventory().getItem(i);
                if (playerStack.isEmpty() || !ItemStack.isSameItemSameTags(filterItem, playerStack)) continue;

                // Transfer items to the container
                ItemStack remainder = insertItems(playerStack);
                if (remainder.getCount() <= itemInHand.getCount() || remainder.getCount() == playerStack.getCount()) {
                    pPlayer.getInventory().setItem(i, remainder);
                } else {
                    pPlayer.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        } else {
            if (itemInHand.isEmpty() || !filterTest(itemInHand)) return;

            int availableSpace = this.getMaxItemCapacity() - this.getStoredAmount();
            int srcAmount = itemInHand.getCount();

            // If no space available and void upgrade is present, void the items in hand
            if (availableSpace <= 0 && hasVoidUpgrade()) {
                itemInHand.shrink(srcAmount);
                return;
            }

            int moveAmount = Math.min(srcAmount, availableSpace);
            if (moveAmount > 0) {
                // If no filter has been set, set it the item in hand
                if (this.getFilterItem().isEmpty()) setFilter(itemInHand);

                ItemStack remainder = insertItems(itemInHand);
                if (remainder.getCount() <= itemInHand.getCount() || remainder.getCount() == moveAmount) {
                    pPlayer.setItemInHand(InteractionHand.MAIN_HAND, remainder);
                } else {
                    pPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
        setChanged();
    }

    public void transferFromStorage(@NotNull Player pPlayer) {
        ItemStack slot0 = itemHandler.getStackInSlot(0);

        if (!slot0.isEmpty()) {
            int maxStack = Math.min(slot0.getMaxStackSize(), slot0.getCount());
            int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;
            ItemStack toExtract = slot0.copyWithCount(amountToExtract);

            ItemHandlerHelper.giveItemToPlayer(pPlayer, toExtract);
            slot0.shrink(amountToExtract);
            this.setChanged();
        }
    }

    public ItemStack insertItems(ItemStack srcStack) {
        if (this.filterTest(srcStack)) {
            int availableSpace = this.getMaxItemCapacity() - this.getStoredAmount();
            int srcAmount = srcStack.getCount();

            if (availableSpace <= 0 && hasVoidUpgrade()) {
                srcStack.shrink(srcAmount);
                return srcStack;
            }

            int moveAmount = Math.min(srcAmount, availableSpace);
            if (moveAmount > 0) {
                // If no filter has been set, set it the item in hand
                if (this.getFilterItem().isEmpty()) setFilter(srcStack);

                if (!this.itemHandler.getStackInSlot(0).isEmpty()) {
                    this.itemHandler.getStackInSlot(0).grow(moveAmount);
                } else {
                    this.itemHandler.setStackInSlot(0, srcStack.copyWithCount(moveAmount));
                }

                srcStack.shrink(moveAmount);
                setChanged();
            }
        }
        return srcStack;
    }

    @Override
    public boolean canPlaceItem(int pIndex, @NotNull ItemStack pStack) {
        // Used by StorageNetwork
        if (!this.filterTest(pStack)) return false;
        if (this.hasVoidUpgrade()) return true;

        int freeSpace = this.getMaxItemCapacity() - this.getStoredAmount();

        return freeSpace > 0;
    }

    public void removeFilter() {
        this.filterItem = ItemStack.EMPTY;
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(SimpleStorageBox.COPY_NBT, false), Block.UPDATE_ALL);
        }
    }

    public void setFilter(ItemStack itemStack) {
        this.filterItem = itemStack.copyWithCount(1);
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(SimpleStorageBox.COPY_NBT, true), Block.UPDATE_ALL);
        }
    }

    public boolean filterTest(ItemStack stack) {
        if (stack.is(ModTags.Items.STORAGE_BOX_ITEM) || stack.is(ModTags.Items.STORAGE_BOX_UPGRADE))
            return false;
        return this.filterItem.isEmpty() || ItemStack.isSameItemSameTags(stack, this.filterItem);
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory) {
        return new SimpleStorageBoxMenu(i, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    @Override
    public int getContainerSize() {
        return this.itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return this.itemHandler.getStackInSlot(i);
    }

    @Override
    public @NotNull ItemStack removeItem(int pIndex, int pCount) {
        return this.itemHandler.extractItem(pIndex, pCount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pIndex) {
        this.itemHandler.setStackInSlot(pIndex, ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int i, @NotNull ItemStack itemStack) {
        this.itemHandler.setStackInSlot(i, itemStack);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // NOOP
    }

    // ThresholdSwitchObservable //
    @Override
    public int getMaxValue() {
        return this.maxItemCapacity;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return this.storedAmount;
    }

    @Override
    public MutableComponent format(int i) {
        return CreateLang.translateDirect("create.gui.threshold_switch.currently", i);
    }

    public void applyInventoryToBlock(ItemStackHandler wrapped, ItemStack filterItem) {
        setFilter(filterItem);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, wrapped.getStackInSlot(i));
        }
    }

}
