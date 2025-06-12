package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimpleStorageBoxEntity extends BaseContainerBlockEntity implements ThresholdSwitchObservable {
    public BlockPos pos;
    public int tick = 0;

    public static int BASE_CAPACITY = 32;
    public static int ITEM_STACK_SIZE = 64;
    public int maxCapacity = BASE_CAPACITY; // Measured in stacks so max planks = 64 * 8000, max ender pearls = 16 * 8000
    public int maxItemCapacity = ITEM_STACK_SIZE * maxCapacity;
    public int slot0MaxCapacity = maxItemCapacity; // - itemStackSize;
    public int storedAmount = 0;
    public boolean voidUpgrade = false;
    public int capacityUpgrades = 0;

    /*
        Slot0 = Total item count (slot0Amount)
        Slot1 = Insertion slot (slot1Amount)
        Slot2 = "Fake slot" where items to be voided go
        Slot3 = Void Upgrade Item slot
        Slot4-12 = Capacity Upgrade Item slots
     */
    public static final int VOID_UPGRADE_SLOT = 3;
    public static final int CAPACITY_UPGRADE_SLOT_START = 4;
    public static final int MAX_CAPACITY_UPGRADES = 9;
    public static final int BASE_SLOT_COUNT = 3;

    public int SLOT_COUNT = BASE_SLOT_COUNT + 1 + MAX_CAPACITY_UPGRADES; // 2 + RemainderSlot + Void Upgrade Slot + Capacity Upgrade Slots
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler = createItemHandler();
    private final Lazy<IItemHandlerModifiable> lazyItemHandler = Lazy.of(() -> itemHandler);

    public SimpleStorageBoxEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.pos = pPos;
    }

    private @NotNull ItemStackHandler createItemHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                SimpleStorageBoxEntity.this.setChanged();
                if (level != null)
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isPlayerInteraction || slot <= 1) {
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
                if (slot > 2 && stack.is(ModTags.Items.STORAGE_BOX_UPGRADE)) return true;
                if (filterTest(stack)) {
                    if (isPlayerInteraction)
                        return true;
                    if (slot > 1 && !voidUpgrade)
                        return false;
                    if (voidUpgrade)
                        return true;
                    return !(getPercent() == 100);
                }
                return false;
            }

            @Override
            public int getSlotLimit(int slot) {
                if (slot == 0) return maxItemCapacity;
                return super.getSlotLimit(slot);
            }
        };
    }

    public IItemHandlerModifiable getItemHandler() {
        return lazyItemHandler.get();
    }

    public int getCapacityUpgrades() {
        this.capacityUpgrades = 0;
        for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
            if (this.itemHandler.getStackInSlot(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                this.capacityUpgrades++;
            }
        }
        return this.capacityUpgrades;
    }

    public boolean hasVoidUpgrade() {
        this.voidUpgrade = this.itemHandler.getStackInSlot(VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
        return this.voidUpgrade;
    }

    public int getStoredAmount() {
        // Take into account items in slot 1 as this affects items being inserted
        this.storedAmount = this.itemHandler.getStackInSlot(0).getCount() + this.itemHandler.getStackInSlot(1).getCount();
        return this.storedAmount;
    }

    public int getMaxItemCapacity() {
        this.maxCapacity = BASE_CAPACITY;
        if (this.getCapacityUpgrades() > 0) {
            for (int i = 0; i < this.capacityUpgrades; i++) {
                this.maxCapacity *= 2;
            }
        }
        this.maxItemCapacity = this.maxCapacity * 64;

        if (!filterItem.isEmpty()) {
            ITEM_STACK_SIZE = filterItem.getMaxStackSize();
            // If the filter has an item then get max stack size of item and multiply by maxCapacity
            this.maxItemCapacity = this.maxCapacity * filterItem.getMaxStackSize();
            this.slot0MaxCapacity = this.maxItemCapacity - filterItem.getMaxStackSize();
        }
        return this.maxItemCapacity;
    }

    public ItemStack getFilterItem() {
        return this.filterItem;
    }

    public void setPlayerInteraction(boolean isPlayer) {
        this.isPlayerInteraction = isPlayer;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        if (this.hasCustomName()) components.set(DataComponents.CUSTOM_NAME, this.getName());
        if (!this.isEmpty()) components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.putInt("MaxCapacity", this.maxCapacity);
        tag.putInt("MaxItemCapacity", this.getMaxItemCapacity());
        tag.putInt("StoredAmount", this.getStoredAmount());
        tag.putBoolean("VoidUpgrade", this.hasVoidUpgrade());
        CompoundTag filterTag = new CompoundTag();
        filterTag.putString("id", BuiltInRegistries.ITEM.getKey(this.filterItem.getItem()).toString());
        filterTag.putByte("Count", (byte) 1);
        tag.put("FilterItem", filterTag);
        super.saveAdditional(tag, registries);
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        this.maxCapacity = tag.getInt("MaxCapacity");
        this.maxItemCapacity = tag.getInt("MaxItemCapacity");
        this.storedAmount = tag.getInt("StoredAmount");
        this.voidUpgrade = tag.getBoolean("VoidUpgrade");
        CompoundTag filterTag = tag.getCompound("FilterItem");
        this.setFilter(
                ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), filterTag)
                        .resultOrPartial()
                        .orElse(ItemStack.EMPTY)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        ItemStack slot0 = this.itemHandler.getStackInSlot(0);
        ItemStack slot1 = this.itemHandler.getStackInSlot(1);

        // Get Stored Amount
        storedAmount = getStoredAmount();

        // Set filter item to items inside to prevent wrong items being put in
        if (!slot0.isEmpty() && !ItemStack.isSameItemSameComponents(slot0, filterItem)) {
            setFilter(slot0);
        }

        getMaxItemCapacity();
        if (!slot1.isEmpty() && (getStoredAmount() != maxItemCapacity)) {
            moveItems();
        }

        if (tick++ >= ConfigManager.CommonConfig.STORAGE_BOX_UPDATE_TIME.get()) {
            tick = 0;
            updateBlockState(level);
        }
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
    }

    private void moveItems() {
        ItemStack slot0 = this.itemHandler.getStackInSlot(0);
        ItemStack slot1 = this.itemHandler.getStackInSlot(1);

        // If full & using void upgrade then items go into slot 2 (delete them all!)
        if (!this.itemHandler.getStackInSlot(2).isEmpty()) {
            this.itemHandler.setStackInSlot(2, ItemStack.EMPTY);
        }

        // Incoming items are placed into slot 1
        // Move items from slot 1 to slot 0 (slot 0 is bulk storage)
        if (slot1.isEmpty()) return;

        int slot1Amount = slot1.getCount();

        // If no items in slot 0, then add
        if (slot0.isEmpty()) {

            this.itemHandler.setStackInSlot(0, slot1.copy());
            this.itemHandler.setStackInSlot(1, ItemStack.EMPTY);

        } else {
            // Always move items from slot 1 to 0 if space available
            int slot0FreeSpace = this.slot0MaxCapacity - slot0.getCount();
            int amountToMove = Math.min(slot1Amount, slot0FreeSpace);
            slot0.grow(amountToMove);
            slot1.shrink(amountToMove);
            this.setChanged();
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
                            dropItems(pPlayer.level(), voidStack);
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
                if (playerStack.isEmpty() || !ItemStack.isSameItemSameComponents(filterItem, playerStack)) continue;

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
            // moveItems() is needed to prevent a double insert from belt/hopper/chute
            this.moveItems();
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

                if (!this.itemHandler.getStackInSlot(1).isEmpty()) {
                    this.itemHandler.getStackInSlot(1).grow(moveAmount);
                } else {
                    this.itemHandler.setStackInSlot(1, srcStack.copyWithCount(moveAmount));
                }

                srcStack.shrink(moveAmount);
                setChanged();
            }
        }
        return srcStack;
    }

    public void dropItems(Level level, ItemStack itemStack) {
        Direction facing = SimpleStorageBox.getDirectionFacing(getBlockState());
        float xOffset = 0.5f;
        float zOffset = 0.5f;
        if (facing == Direction.NORTH) zOffset = 0.5f - 0.8f;
        if (facing == Direction.WEST) xOffset = 0.5f - 0.8f;
        if (facing == Direction.EAST) xOffset = 1.3f;
        if (facing == Direction.SOUTH) zOffset = 1.3f;

        float dropX = this.pos.getX() + xOffset;
        float dropY = this.pos.getY();
        float dropZ = this.pos.getZ() + zOffset;
        // Create Item Entities
        ItemStack dropStack = itemStack.split(itemStack.getCount());
        ItemEntity droppedItems = new ItemEntity(level, dropX, dropY, dropZ, dropStack);
        Vec3 motion = droppedItems.getDeltaMovement();
        droppedItems.push(-motion.x, -motion.y, -motion.z);
        level.addFreshEntity(droppedItems);
    }

    public void removeFilter() {
        this.filterItem = ItemStack.EMPTY;
    }

    public void setFilter(ItemStack itemStack) {
        this.filterItem = itemStack.copyWithCount(1);
    }

    public boolean filterTest(ItemStack stack) {
        // Prevent inception
        if (stack.is(ModTags.Items.STORAGE_BOX_ITEM) || stack.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {
            return false;
        }

        return this.filterItem.isEmpty() || ItemStack.isSameItemSameComponents(stack, this.filterItem);
    }

    public float getPercent() {
        return (float) this.storedAmount / this.maxItemCapacity * 100;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.fxntstorage.simple_storage_box_title");
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Used by StorageNetwork
        if (!this.filterTest(stack)) return false;
        if (this.hasVoidUpgrade()) return true;

        int freeSpace = this.getMaxItemCapacity() - this.getStoredAmount();
        int amountToPlace = stack.getCount();

        return freeSpace >= amountToPlace;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> itemList = NonNullList.create();

        for (int i = 0; i < this.itemHandler.getSlots(); ++i) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            itemList.add(stack);
        }
        return itemList;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        for (int i = 0; i < nonNullList.size(); ++i) {
            this.itemHandler.setStackInSlot(i, nonNullList.get(i));
        }
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

    public void applyInventoryToBlock(ItemStackHandler wrapped) {
        setFilter(wrapped.getStackInSlot(0));
        for (int i = 0; i < wrapped.getSlots(); ++i) {
            itemHandler.setStackInSlot(i, wrapped.getStackInSlot(i));
        }
    }

}
