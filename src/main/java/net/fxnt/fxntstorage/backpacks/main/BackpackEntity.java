package net.fxnt.fxntstorage.backpacks.main;

import net.fxnt.fxntstorage.backpacks.upgrades.BackpackAsBlockUpgradeHandler;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class BackpackEntity extends BaseContainerBlockEntity implements IBackpackContainer {
    private int slotCount = BackpackBlock.getSlotCount();
    private final BlockPos pos;
    private int lastTick = 0;
    private boolean doTick = false;
    private Component customName;
    private boolean initializedBlock = false;
    private boolean isGhostSlotLocked = false;

    private final Block block;
    public int maxStackSize;

    public NonNullList<String> upgrades = NonNullList.create();
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler = new ItemStackHandler(slotCount + 1) { // +1 for ghost slot
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isPlayerInteraction || slot < Util.ITEM_SLOT_END_RANGE)
                return super.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (filterTest(stack))
                return false;
            if (isPlayerInteraction)
                return true;
            if (slot != GHOST_SLOT || !itemHandler.getStackInSlot(GHOST_SLOT).isEmpty() || isGhostSlotLocked)
                return false;
            return hasEmptyOrNonMaxSlot(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            BackpackEntity.this.setChanged();
        }
    };
    private LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.empty();
    private final int GHOST_SLOT = itemHandler.getSlots() - 1;

    public BackpackEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BACK_PACK_ENTITY.get(), pos, blockState);
        this.pos = pos;
        this.block = blockState.getBlock();
        if (this.block instanceof BackpackBlock backpackBlock) {
            this.maxStackSize = backpackBlock.getMaxStackSize();
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public NonNullList<String> getUpgrades() {
        return this.upgrades;
    }

    public void setCustomName(@NotNull Component hoverName) {
        this.customName = hoverName;
    }

    @Override
    public @NotNull Component getName() {
        return this.getDisplayName();
    }

    @SuppressWarnings("deprecation")
    public @NotNull Component getDisplayName() {
        if (this.customName != null) return this.customName;

        Level blockLevel = this.level;
        if (blockLevel != null) {
            return this.block.getCloneItemStack(this.level, this.pos, this.getBlockState()).getHoverName();
        } else {
            return new ItemStack(ModItems.BACK_PACK.get()).getHoverName();
        }
    }

    public void setData(int slotCount, int maxStackSize) {
        // Called when Block Creates Entity
        this.slotCount = slotCount;
        this.maxStackSize = maxStackSize;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                int slot = compoundTag.getByte("Slot") & 255;
                ItemStack slotStack = ItemStack.of(compoundTag);
                if (compoundTag.contains("ActualCount", Tag.TAG_INT)) {
                    slotStack.setCount(compoundTag.getInt("ActualCount"));
                }
                if (slot < this.itemHandler.getSlots()) {
                    this.itemHandler.setStackInSlot(slot, slotStack);
                }
            }
        }
        if (tag.contains("Upgrades")) {
            this.upgrades.clear();
            ListTag upgradesList = tag.getList("Upgrades", Tag.TAG_STRING);
            for (int i = 0; i < upgradesList.size(); i++) {
                this.upgrades.add(i, upgradesList.getString(i));
            }
        }
        if (tag.contains("maxStackSize")) {
            this.maxStackSize = tag.getInt("maxStackSize");
        }
    }

    // Serialize the BlockEntity
    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        tag = saveEverything(tag);
        super.saveAdditional(tag);
    }

    public CompoundTag saveEverything(CompoundTag tag) {
        ListTag itemsList = new ListTag();
        for (int i = 0; i < this.itemHandler.getSlots(); ++i) {
            ItemStack tagStack = this.itemHandler.getStackInSlot(i);
            if (!tagStack.isEmpty()) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putByte("Slot", (byte) i);
                tagStack.save(compoundTag);
                compoundTag.putInt("ActualCount", tagStack.getCount());
                itemsList.add(compoundTag);
            }
        }
        tag.put("Items", itemsList);

        ListTag upgradesList = new ListTag();
        for (int i = 0; i < this.upgrades.size(); i++) {
            upgradesList.add(i, StringTag.valueOf(this.upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("maxStackSize", this.maxStackSize);

        return tag;
    }

    public ItemStack saveToItemStack(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement("BlockEntityTag");
        saveEverything(tag);
        // Save custom display name
        if (this.customName != null) {
            CompoundTag displayTag = stack.getOrCreateTagElement("display");
            displayTag.putString("Name", Component.Serializer.toJson(this.customName));
        }

        return stack;
    }

    public void refreshUpgrades() {
        this.upgrades.clear();
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.itemSlotCount + BackpackBlock.toolSlotCount;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.upgradeSlotCount;

        for (int i = UPGRADE_SLOT_START_INDEX; i < UPGRADE_SLOT_END_INDEX; i++) {
            ItemStack itemStack = this.itemHandler.getStackInSlot(i);
            if (itemStack.getItem() instanceof UpgradeItem upgradeItem) {
                // ADD TO UPGRADE CACHE
                String upgradeName = upgradeItem.getUpgradeName();
                if (!this.upgrades.contains(upgradeName)) {
                    this.upgrades.add(upgradeName);
                }
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        super.handleUpdateTag(tag);
        this.load(tag);
    }

    private boolean hasEmptyOrNonMaxSlot(ItemStack pStack) {
        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);

            if (stack.isEmpty() || (ItemStack.isSameItemSameTags(stack, pStack) && stack.getCount() < this.maxStackSize * pStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    public static boolean filterTest(@NotNull ItemStack stack) {
        // Test to see if we're allowing this item into the backpack
        // Use to prevent inception, needs to be called on any backpack interaction (including quick move / mouse interaction)
        // Add filtering in here to
        // Prevent inception
        return stack.getItem() instanceof BackpackItem;
    }


    public void serverTick(Level level) {
        if (level != null) {
            if (!level.isClientSide) {
                // Need to run moveItems() every tick
                moveItems();

                this.lastTick++;
                int updateEveryXTicks = 30;
                if (this.lastTick >= updateEveryXTicks) {
                    this.lastTick = 0;
                    this.doTick = true;
                }
                if (!this.doTick) return;

                if (!this.initializedBlock) {
                    level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), BackpackBlock.UPDATE_ALL);
                    this.initializedBlock = true;
                }

                if (this.upgrades.contains(Util.MAGNET_UPGRADE)) {
                    BackpackAsBlockUpgradeHandler upgradeHandler = new BackpackAsBlockUpgradeHandler(this);
                    upgradeHandler.applyMagnetUpgrade();
                }
                this.doTick = false;
            }
        }
    }

    private void moveItems() {
        ItemStack ghostSlot = this.itemHandler.getStackInSlot(GHOST_SLOT);

        // Incoming items are placed into GHOST_SLOT
        // Move items from GHOST_SLOT to first available slot in item storage
        if (ghostSlot.isEmpty()) return;

        // Lock the slot (prevent accepting new items)
        isGhostSlotLocked = true;

        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; i++) {
            ItemStack mergeSlot = this.itemHandler.getStackInSlot(i);

            // If an empty slot is found, break out of loop
            if (mergeSlot.isEmpty()) {
                doMove(i, mergeSlot, ghostSlot);
                break;
            }

            // If a slot with the same item is found, and the slot is < maxStackSize, break out of loop
            if (ItemStack.isSameItemSameTags(mergeSlot, ghostSlot)) {
                if (mergeSlot.getCount() < (this.maxStackSize * ghostSlot.getMaxStackSize())) {
                    doMove(i, mergeSlot, ghostSlot);
                    break;
                }
            }
        }
    }

    private void doMove(int mergeSlotId, @NotNull ItemStack mergeStack, ItemStack ghostStack) {
        if (mergeStack.isEmpty()) {
            // Add item to empty slot
            this.itemHandler.setStackInSlot(mergeSlotId, ghostStack.copy());
            ghostStack.shrink(ghostStack.getCount());
        } else {
            // Increment item in merge slot, decrement item in ghost slot
            int mergeSlotFreeSpace = (this.maxStackSize * mergeStack.getMaxStackSize()) - mergeStack.getCount();
            int amountToMove = Math.min(ghostStack.getCount(), mergeSlotFreeSpace);
            mergeStack.grow(amountToMove);
            ghostStack.shrink(amountToMove);
        }
        this.setChanged();
        isGhostSlotLocked = false;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return IntStream.range(0, itemHandler.getSlots())
                .mapToObj(itemHandler::getStackInSlot)
                .allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        return itemHandler.extractItem(pSlot, pAmount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        return itemHandler.insertItem(pSlot, ItemStack.EMPTY, false);
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        itemHandler.setStackInSlot(pSlot, pStack);
        if (pStack.getCount() > this.getStackMultiplier() * pStack.getMaxStackSize()) {
            pStack.setCount(this.getStackMultiplier() * pStack.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public int getStackMultiplier() {
        return this.maxStackSize;
    }

    @Override
    public void setPlayerInteraction(boolean isPlayer) {
        this.isPlayerInteraction = isPlayer;
    }

    @Override
    public void setDataChanged() {
        this.setChanged();
    }

    @Override
    public void setChanged() {
        refreshUpgrades();
        super.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true; // TODO determine player validity
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory) {
        return new BackpackBlockMenu(i, inventory, this);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return getDisplayName();
    }

    @Override
    public void clearContent() {
        // noop
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public void setTag(CompoundTag tag) {
        // NOOP
    }
}
