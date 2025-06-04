package net.fxnt.fxntstorage.container;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.container.util.StorageBoxFilteringBox;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.fxnt.fxntstorage.container.StorageBox.VOID_UPGRADE;

public class StorageBoxEntity extends SmartBlockEntity implements Container, MenuProvider, Nameable, ThresholdSwitchObservable {
    public int slotCount = 0;
    private Component customName;

    public BlockPos pos;
    public int lastTick = 0;
    public int storedAmount = -1;
    public float percentageUsed = 0;
    public boolean doTick = false;
    public boolean voidUpgrade;
    public int updateEveryXTicks = ConfigManager.CommonConfig.STORAGE_BOX_UPDATE_TIME.get();

    private FilteringBehaviour filtering;

    private final ItemStackHandler itemHandler = createItemHandler();
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private SortOrder sortOrder;

    public StorageBoxEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.pos = pos;
        this.voidUpgrade = state.getValue(VOID_UPGRADE);
        this.sortOrder = SortOrder.COUNT;
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler() {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                StorageBoxEntity.this.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return filterTest(level, stack);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Void mode check - might be a slight delay between this check and percentageUsed being updated
                ItemStack amount = super.insertItem(slot, stack, simulate);
                if (percentageUsed == 100 && voidUpgrade) {
                    return ItemStack.EMPTY;
                }
                return amount;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, lazyItemHandler);
    }

    public void initializeEntity(int slotCount) {
        this.slotCount = slotCount;
        this.itemHandler.setSize(this.slotCount);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new StorageBoxFilteringBox());
        behaviours.add(filtering);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();

        if (level != null) level.updateNeighborsAt(pos, this.getBlockState().getBlock());
    }

    @Override
    public Component getName() {
        return getDisplayName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.customName;
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return hasCustomName() ? getCustomName() : getBlockState().getBlock().getName();
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
        if (this.level != null && this.level.isClientSide)
            ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.SET_SORT_ORDER, new FriendlyByteBuf(Unpooled.buffer()).writeEnum(order)));
        setChanged();
    }

    @NotNull
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        extraData.writeBlockPos(this.worldPosition);
        return new StorageBoxMenu(pContainerId, pPlayerInventory, extraData);
    }

    private void writeStoredData(CompoundTag tag) {
        tag.putInt("SlotCount", slotCount);
        tag.putInt("StoredAmount", calculateStoredAmount());
        tag.putFloat("PercentageUsed", calculatePercentageUsed());
        tag.putBoolean("VoidUpgrade", voidUpgrade);
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        writeStoredData(tag);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Items", itemHandler.serializeNBT());
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        tag.putString("SortOrder", sortOrder.name());
        writeStoredData(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        itemHandler.deserializeNBT(tag.getCompound("Items"));
        slotCount = tag.getInt("SlotCount");
        storedAmount = tag.getInt("StoredAmount");
        percentageUsed = tag.getFloat("PercentageUsed");
        voidUpgrade = tag.getBoolean("VoidUpgrade");
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        sortOrder = (tag.contains("SortOrder", Tag.TAG_STRING)) ? SortOrder.valueOf(tag.getString("SortOrder")) : SortOrder.COUNT;
    }

    public int getContainerSize() {
        return slotCount;
    }

    @Override
    public boolean isEmpty() {
        return this.storedAmount <= 0;
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        itemHandler.extractItem(pSlot, pAmount, false);
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        itemHandler.insertItem(pSlot, ItemStack.EMPTY, false);
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        itemHandler.setStackInSlot(pSlot, pStack);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return true;
    }

    public FilteringBehaviour getFilter() {
        return filtering;
    }

    public int getStoredAmount() {
        return calculateStoredAmount();
    }

    public int getPercentageUsed() {
        return Math.round(calculatePercentageUsed());
    }

    public int calculateStoredAmount() {
        int storedAmount = 0;
        for (int i = 0; i < slotCount; i++) {
            storedAmount += itemHandler.getStackInSlot(i).getCount();
        }
        return storedAmount;
    }

    private int calculateMaxValue() {
        int totalSpace = 0;
        int maxItemStackSize = this.getMaxStackSize();
        for (int i = 0; i < slotCount; ++i) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                maxItemStackSize = itemHandler.getStackInSlot(i).getMaxStackSize();
            }
            totalSpace += maxItemStackSize;
        }
        return totalSpace;
    }

    private int calculateCurrentValue() {
        int usedSpace = 0;
        for (int i = 0; i < slotCount; ++i) {
            usedSpace += itemHandler.getStackInSlot(i).getCount();
        }
        return usedSpace;
    }

    public float calculatePercentageUsed() {
        float percentageUsed = 0;
        int totalSpace = 0;
        int usedSpace = 0;
        for (int i = 0; i < slotCount; i++) {
            int amountInSlot = itemHandler.getStackInSlot(i).getCount();
            int maxItemStackSize = this.getMaxStackSize();
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                maxItemStackSize = itemHandler.getStackInSlot(i).getMaxStackSize();
            }
            totalSpace += maxItemStackSize;
            usedSpace += amountInSlot;
        }
        if (totalSpace > 0) {
            percentageUsed = ((float) usedSpace / totalSpace) * 100;
        }
        return percentageUsed;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Items", itemHandler.serializeNBT());
        tag.putString("SortOrder", sortOrder.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        super.handleUpdateTag(tag);
        itemHandler.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("SortOrder", Tag.TAG_STRING)) {
            this.sortOrder = SortOrder.valueOf(tag.getString("SortOrder"));
        }
    }

    @Override
    public boolean canPlaceItem(int pIndex, @NotNull ItemStack pStack) {
        return filterTest(level, pStack);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel != null && !pLevel.isClientSide) {

            if (lastTick++ >= updateEveryXTicks) {
                lastTick = 0;
                doTick = true;
            }
            if (!doTick) return;

            this.storedAmount = calculateStoredAmount();
            this.percentageUsed = calculatePercentageUsed();
            BlockState currentState = this.getBlockState();
            pLevel.sendBlockUpdated(pPos, pState, currentState, Block.UPDATE_ALL);

            boolean copyNbt = !isContainerModified();
            if (level != null && currentState.getValue(StorageBox.COPY_NBT) != copyNbt) {
                level.setBlock(worldPosition, getBlockState().setValue(StorageBox.COPY_NBT, copyNbt), Block.UPDATE_ALL);
            }

            int totalSlots = itemHandler.getSlots();
            boolean allSlotsFull = true;

            int filledSlots = 0;
            for (int i = 0; i < slotCount; i++) {
                ItemStack slot = itemHandler.getStackInSlot(i);
                if (!slot.isEmpty()) {
                    filledSlots++;
                    if (slot.getCount() < slot.getMaxStackSize()) {
                        allSlotsFull = false;
                    }
                } else {
                    allSlotsFull = false;
                }
            }
            int emptySlots = totalSlots - filledSlots;

            EnumProperties.StorageUsed newStorageUsed = EnumProperties.StorageUsed.EMPTY;

            if (allSlotsFull) {
                newStorageUsed = EnumProperties.StorageUsed.FULL;
            } else if (emptySlots == 0) {
                newStorageUsed = EnumProperties.StorageUsed.SLOTS_FILLED;
            } else if (filledSlots > 0) {
                newStorageUsed = EnumProperties.StorageUsed.HAS_ITEMS;
            }

            if (currentState.getValue(StorageBox.STORAGE_USED) != newStorageUsed) {
                pLevel.setBlock(pPos, currentState.setValue(StorageBox.STORAGE_USED, newStorageUsed), 3); // 3 is the update flag
            }
            doTick = false;
        }
        super.tick();
    }

    private boolean isContainerModified() {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return !hasCustomName();
    }

    // Transferring Items
    public void transferToStorage(@NotNull BlockState pState, Level pLevel, @NotNull Player pPlayer, @NotNull Boolean transferAll) {
        // Get the item in the players main hand and check the hand is NOT empty and the item matches the filter (if one applied)
        ItemStack itemInHand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isVoidEnabled = pState.getValue(VOID_UPGRADE);

        /*
            Single-click: add entire stack in main hand
            double-click: add every stack in player inventory matching item filter
         */

        if (transferAll) {
            ItemStack filterItem = filtering.getFilter();
            for (int i = 0; i < pPlayer.getInventory().getContainerSize(); i++) {
                ItemStack playerStack = pPlayer.getInventory().getItem(i);
                if (playerStack.isEmpty() || !ItemStack.isSameItemSameTags(filterItem, playerStack)) continue;

                // Transfer items to the container
                ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, playerStack, false);

                // Void Mode check
                if (remainder.getCount() > 0 && isVoidEnabled) {
                    remainder = ItemStack.EMPTY;
                }

                // Update player inventory with remaining items
                pPlayer.getInventory().setItem(i, remainder);
            }
        } else {
            if (itemInHand.isEmpty() || !filterTest(pLevel, itemInHand)) return;

            ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, itemInHand, false);

            // Void Mode check
            if (remainder.getCount() > 0 && isVoidEnabled) {
                remainder = ItemStack.EMPTY;
            }

            if (remainder.getCount() <= itemInHand.getCount()) {
                pPlayer.setItemInHand(InteractionHand.MAIN_HAND, remainder);
            } else {
                pPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
        this.setChanged();
    }

    public void transferFromStorage(@NotNull Player pPlayer) {
        ItemStack filterItem = filtering.getFilter();

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(itemHandler.getStackInSlot(i), filterItem)) continue;

                int maxStack = Math.min(itemHandler.getStackInSlot(i).getMaxStackSize(), itemHandler.getStackInSlot(i).getCount());
                int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;
                ItemStack toExtract = stackInSlot.copyWithCount(amountToExtract);

                ItemHandlerHelper.giveItemToPlayer(pPlayer, toExtract);
                itemHandler.extractItem(i, amountToExtract, false);
                break;
            }
        }
        this.setChanged();
    }

    public boolean filterTest(Level level, @NotNull ItemStack stack) {
        // Prevent a StorageBox being placed in a StorageBox #TheDreamIsReal
        if (stack.is(ModTags.Items.STORAGE_BOX_ITEM)) {
            return false;
        }

        ItemStack filterItem = filtering.getFilter();
        return FilterItemStack.of(filterItem).test(level, stack);
    }

    public void toggleVoidUpgrade() {
        BlockState blockState = this.getBlockState();
        Level level = this.getLevel();
        if (level != null) {
            this.voidUpgrade = !blockState.getValue(VOID_UPGRADE);
            level.setBlockAndUpdate(this.getBlockPos(), blockState.setValue(VOID_UPGRADE, this.voidUpgrade));
            this.setChanged();
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void clearContent() {
    }

    // ThresholdSwitchObservable
    @Override
    public int getMaxValue() {
        return calculateMaxValue();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return calculateCurrentValue();
    }

    @Override
    public MutableComponent format(int i) {
        return CreateLang.translateDirect("create.gui.threshold_switch.currently", i);
    }

    public void applyInventoryToBlock(ItemStackHandler wrapped) {
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            itemHandler.setStackInSlot(i, i < wrapped.getSlots() ? wrapped.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }

}
