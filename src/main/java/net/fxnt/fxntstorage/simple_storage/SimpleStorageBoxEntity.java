package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class SimpleStorageBoxEntity extends BaseContainerBlockEntity implements ThresholdSwitchObservable {
    public String title = "Simple Storage Box";
    public BlockPos pos;
    public int tick = 0;

    public int baseCapacity = 32;
    public int itemStackSize = 64;
    public int maxCapacity = baseCapacity; // Measured in stacks so max planks = 64 * 8000, max ender pearls = 16 * 8000
    public int maxItemCapacity = itemStackSize * maxCapacity;
    public int slot0MaxCapacity = maxItemCapacity - itemStackSize;
    public int slot0Amount = 0;
    public int slot1Amount = 0;
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
    public final int VOID_UPGRADE_SLOT = 3;
    public final int CAPACITY_UPGRADE_SLOT_START = 4;
    public final int MAX_CAPACITY_UPGRADES = 9;
    public final int BASE_SLOT_COUNT = 3;

    public int slotCount = BASE_SLOT_COUNT + 1 + MAX_CAPACITY_UPGRADES; // 2 + RemainderSlot + Void Upgrade Slot + Capacity Upgrade Slots
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler = new ItemStackHandler(slotCount) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            SimpleStorageBoxEntity.this.setChanged();
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isPlayerInteraction || slot <= 1)
                return super.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
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
    };
    private LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.empty();

    public SimpleStorageBoxEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), position, state);
        this.pos = position;
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

    public int getCapacityUpgrades() {
        this.capacityUpgrades = 0;
        for (int i = this.CAPACITY_UPGRADE_SLOT_START; i < this.CAPACITY_UPGRADE_SLOT_START + this.MAX_CAPACITY_UPGRADES; i++) {
            if (this.itemHandler.getStackInSlot(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                this.capacityUpgrades++;
            }
        }
        return this.capacityUpgrades;
    }

    public boolean hasVoidUpgrade() {
        this.voidUpgrade = this.itemHandler.getStackInSlot(this.VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
        return this.voidUpgrade;
    }

    public int getStoredAmount() {
        // Take into account items in slot 1 as this affects items being inserted
        this.storedAmount = this.itemHandler.getStackInSlot(0).getCount() + this.itemHandler.getStackInSlot(1).getCount();
        return this.storedAmount;
    }

    public int getMaxItemCapacity() {
        this.maxCapacity = this.baseCapacity;
        if (this.getCapacityUpgrades() > 0) {
            for (int i = 0; i < this.capacityUpgrades; i++) {
                this.maxCapacity *= 2;
            }
        }
        this.maxItemCapacity = this.maxCapacity * 64;

        if (!filterItem.isEmpty()) {
            this.itemStackSize = filterItem.getMaxStackSize();
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
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    public void saveInventoryToTag(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < this.slotCount; i++) {
            CompoundTag compoundTag = new CompoundTag();
            ItemStack itemStack = this.itemHandler.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                compoundTag.putByte("Slot", (byte) i);
                itemStack.save(compoundTag);
                compoundTag.putInt("ActualCount", itemStack.getCount());
                listTag.add(compoundTag);
            }
        }
        tag.put("Items", listTag);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        this.saveInventoryToTag(tag);
        tag.putString("title", this.title);
        tag.putInt("slotCount", this.slotCount);
        tag.putInt("maxCapacity", this.maxCapacity);
        tag.putInt("maxItemCapacity", this.getMaxItemCapacity());
        tag.putInt("storedAmount", this.getStoredAmount());
        tag.putBoolean("voidUpgrade", this.hasVoidUpgrade());
        tag.putInt("capacityUpgrades", this.getCapacityUpgrades());
        tag.putInt("slot0Amount", this.itemHandler.getStackInSlot(0).getCount());
        tag.putInt("slot1Amount", this.itemHandler.getStackInSlot(1).getCount());
        CompoundTag filterTag = new CompoundTag();
        this.filterItem.save(filterTag);
        tag.put("filterItem", filterTag);
        super.saveAdditional(tag);
    }

    public void loadInventoryFromTag(CompoundTag tag) {
        if (tag.contains("Items")) {
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag compoundTag = listTag.getCompound(i);
                int slot = compoundTag.getByte("Slot") & 255;
                ItemStack slotStack = ItemStack.of(compoundTag);
                if (compoundTag.contains("ActualCount", Tag.TAG_INT)) {
                    slotStack.setCount(compoundTag.getInt("ActualCount"));
                }
                this.itemHandler.setStackInSlot(slot, slotStack);
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.loadInventoryFromTag(tag);
        this.title = tag.getString("title");
        this.slotCount = tag.getInt("slotCount");
        this.maxCapacity = tag.getInt("maxCapacity");
        this.maxItemCapacity = tag.getInt("maxItemCapacity");
        this.storedAmount = tag.getInt("storedAmount");
        this.voidUpgrade = tag.getBoolean("voidUpgrade");
        this.capacityUpgrades = tag.getInt("capacityUpgrades");
        this.slot0Amount = tag.getInt("slot0Amount");
        this.itemHandler.getStackInSlot(0).setCount(this.slot0Amount);
        this.slot1Amount = tag.getInt("slot1Amount");
        this.itemHandler.getStackInSlot(1).setCount(this.slot1Amount);
        CompoundTag filterTag = tag.getCompound("filterItem");
        this.filterItem = ItemStack.of(filterTag);
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

    public void serverTick(Level level, BlockPos blockPos) {
        if (level.isClientSide) return;

        ItemStack slot0 = this.itemHandler.getStackInSlot(0);

        // Get Stored Amount
        storedAmount = getStoredAmount();

        // Set filter item to items inside to prevent wrong items being put in
        if (!slot0.isEmpty() && !ItemStack.isSameItemSameTags(slot0, filterItem)) {
            setFilter(slot0);
        }

        getMaxItemCapacity();
        moveItems();

        if (tick >= ConfigManager.CommonConfig.STORAGE_BOX_UPDATE_TIME.get()) {

            BlockState currentState = getBlockState();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

            EnumProperties.StorageUsed newStorageUsed = EnumProperties.StorageUsed.EMPTY;

            int storedAmount = getStoredAmount();

            if (storedAmount >= getMaxItemCapacity()) {
                newStorageUsed = EnumProperties.StorageUsed.FULL;
            } else if (storedAmount > 0) {
                newStorageUsed = EnumProperties.StorageUsed.HAS_ITEMS;
            }

            if (currentState.getValue(SimpleStorageBox.STORAGE_USED) != newStorageUsed) {
                level.setBlock(blockPos, currentState.setValue(SimpleStorageBox.STORAGE_USED, newStorageUsed), 3); // 3 is the update flag
            }
        }
        this.tick++;
    }

    private void moveItems() {
        ItemStack slot0 = this.itemHandler.getStackInSlot(0);
        ItemStack slot1 = this.itemHandler.getStackInSlot(1);

        if (getPercent() == 100 && !voidUpgrade) return;

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
                    this.itemHandler.setStackInSlot(this.VOID_UPGRADE_SLOT, itemInHand.copyWithCount(1));
                    if (!pPlayer.isCreative()) {
                        itemInHand.shrink(1);
                        pPlayer.getInventory().setChanged();
                    }
                } else {
                    ItemStack voidStack = this.itemHandler.getStackInSlot(this.VOID_UPGRADE_SLOT);
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
                            dropItems(this.getLevel(), voidStack);
                        }
                    }
                    this.itemHandler.setStackInSlot(this.VOID_UPGRADE_SLOT, ItemStack.EMPTY);
                }
            } else if (itemInHand.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                boolean canAddUpgrade = false;
                for (int i = this.CAPACITY_UPGRADE_SLOT_START; i < this.CAPACITY_UPGRADE_SLOT_START + this.MAX_CAPACITY_UPGRADES; i++) {
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

        return this.filterItem.isEmpty() || ItemStack.isSameItemSameTags(stack, this.filterItem);
    }

    // ThresholdSwitchObservable //
    @Override
    public float getPercent() {
        return (float) this.storedAmount / this.maxItemCapacity * 100;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.literal(title);
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
        return IntStream.range(0, itemHandler.getSlots())
                .mapToObj(itemHandler::getStackInSlot)
                .allMatch(ItemStack::isEmpty);
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

    public boolean canPlaceItem(int index, @NotNull ItemStack itemStack) {
        // TODO: Changes to TRUE immediately after removing an item from the container, and for some reason,
        // TODO: the belt funnel will insist on inserting to Slot2????
        // Check filter
        if (!this.filterTest(itemStack)) return false;

        // Check space in slot 0
        int freeSpace = this.getMaxItemCapacity() - this.getStoredAmount();

        if (this.hasVoidUpgrade()) return true;

        int amountToPlace = itemStack.getCount();
        return freeSpace >= amountToPlace;
    }

    @Override
    public void clearContent() {
        // NOOP
    }

}
