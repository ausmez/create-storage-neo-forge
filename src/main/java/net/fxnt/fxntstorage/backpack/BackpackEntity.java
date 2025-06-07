package net.fxnt.fxntstorage.backpack;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.backpack.main.BackpackBlockMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackAsBlockUpgradeHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BackpackEntity extends BlockEntity implements IBackpackContainer, MenuProvider, Nameable {
    private int slotCount;

    private final BlockPos pos;
    private int lastTick = 0;
    private boolean doTick = false;
    private Component customName;
    private boolean initializedBlock = false;
    private boolean isGhostSlotLocked = false;

    private final Block block;
    public int stackMultiplier;
    private SortOrder sortOrder;

    public NonNullList<String> upgrades = NonNullList.create();
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler;
    private LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.empty();
    private final int GHOST_SLOT;

    public BackpackEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.pos = pPos;
        this.block = pBlockState.getBlock();
        this.sortOrder = SortOrder.COUNT;

        if (this.block instanceof BackpackBlock backpackBlock) {
            this.stackMultiplier = backpackBlock.getStackMultiplier();
            this.slotCount = BackpackBlock.getSlotCount();
        }

        this.itemHandler = createItemHandler();
        this.GHOST_SLOT = this.itemHandler.getSlots() - 1;
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(slotCount + 1) { // +1 for ghost slot
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
                    stacks.set(slot, ItemStack.of(itemTags).copyWithCount(slotStack.getCount()));
                }
                onLoad();
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }
        };
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
    public @Nullable Component getCustomName() {
        return customName;
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
            return new ItemStack(ModBlocks.BACKPACK.get()).getHoverName();
        }
    }

    public void setData(int slotCount, int maxStackSize) {
        // Called when Block Creates Entity
        this.slotCount = slotCount;
        this.stackMultiplier = maxStackSize;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
//        itemHandler.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("Items")) {
            ListTag listTag = tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
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
        if (tag.contains("StackMultiplier")) {
            this.stackMultiplier = tag.getInt("StackMultiplier");
        }
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        sortOrder = (tag.contains("SortOrder", Tag.TAG_STRING)) ? SortOrder.valueOf(tag.getString("SortOrder")) : SortOrder.COUNT;
    }

    // Serialize the BlockEntity
    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", itemHandler.serializeNBT());

        ListTag upgradesList = new ListTag();
        for (int i = 0; i < this.upgrades.size(); i++) {
            upgradesList.add(i, StringTag.valueOf(this.upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("StackMultiplier", this.stackMultiplier);
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        tag.putString("SortOrder", this.sortOrder.name());
    }

    public ItemStack saveToItemStack(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement("BlockEntityTag");
        saveAdditional(tag);
        // Save custom display name
        if (this.customName != null) {
            CompoundTag displayTag = stack.getOrCreateTagElement("display");
            displayTag.putString("Name", Component.Serializer.toJson(this.customName));
        }

        return stack;
    }

    public void refreshUpgrades() {
        this.upgrades.clear();
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.ITEM_SLOT_COUNT + BackpackBlock.TOOL_SLOT_COUNT;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.UPGRADE_SLOT_COUNT;

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
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
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

            if (stack.isEmpty() || (ItemStack.isSameItemSameTags(stack, pStack) && stack.getCount() < this.stackMultiplier * pStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    public static boolean filterTest(@NotNull ItemStack stack) {
        // Test to see if we're allowing this item into the backpack
        return stack.getItem() instanceof BackpackItem;
    }

    public void serverTick(Level level) {
        if (level != null) {
            if (!level.isClientSide) {
                // Need to run moveItems() every updateClientStorageData
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

    public void moveItems() {
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
                if (mergeSlot.getCount() < (this.stackMultiplier * ghostSlot.getMaxStackSize())) {
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
            int mergeSlotFreeSpace = (this.stackMultiplier * mergeStack.getMaxStackSize()) - mergeStack.getCount();
            int amountToMove = Math.min(ghostStack.getCount(), mergeSlotFreeSpace);
            mergeStack.grow(amountToMove);
            ghostStack.shrink(amountToMove);
        }
        this.setChanged();
        isGhostSlotLocked = false;
    }

    @Override
    public int getStackMultiplier() {
        return this.stackMultiplier;
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
    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public void setTag(CompoundTag tag) {
        // NOOP
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder order) {
        sortOrder = order;
        if (this.level != null && this.level.isClientSide)
            ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.SET_SORT_ORDER, new FriendlyByteBuf(Unpooled.buffer()).writeEnum(order)));
        setChanged();
    }

    public int calcRedstoneFromInventory() {
        int itemsFound = 0;
        float proportion = 0.0F;

        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; ++i) {
            ItemStack itemstack = this.itemHandler.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                proportion += (float) itemstack.getCount() / (itemstack.getMaxStackSize() * this.stackMultiplier);
                ++itemsFound;
            }
        }

        proportion /= (float) Util.ITEM_SLOT_END_RANGE;
        return Mth.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        extraData.writeBlockPos(pos);
        return new BackpackBlockMenu(pContainerId, pPlayerInventory, extraData);
    }

}
