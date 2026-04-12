package net.fxnt.fxntstorage.container;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static net.fxnt.fxntstorage.container.StorageBox.STORAGE_USED;
import static net.fxnt.fxntstorage.container.StorageBox.VOID_UPGRADE;

public class StorageBoxEntity extends SmartBlockEntity implements Container, MenuProvider, Nameable, ThresholdSwitchObservable {
    public int slotCount = 0;
    private int tickCount = 0;

    private Component customName;
    private int storedAmount = 0;
    private float percentageUsed = 0;
    boolean voidUpgrade;

    private FilteringBehaviour filtering;
    private VersionedInventoryTrackerBehaviour invVersionTracker;

    private final ItemStackHandler itemHandler = createItemHandler();
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private SortOrder sortOrder;

    private record StorageStats(int stored, int capacity, EnumProperties.StorageUsed fillLevel) {
        float percentage() {
            return capacity == 0 ? 0f : (stored * 100f) / capacity;
        }
    }

    public StorageBoxEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
            public boolean isItemValid(int slot, ItemStack stack) {
                return level != null && filterTest(stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                ItemStack result = super.insertItem(slot, stack, simulate);
                if (voidUpgrade && calculatePercentageUsed() >= 100f && filterTest(result)) {
                    return ItemStack.EMPTY;
                }
                return result;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack stackToExtract = getStackInSlot(slot);
                if (!filterTest(stackToExtract)) return ItemStack.EMPTY;
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, lazyItemHandler);
    }

    public void initializeEntity(int slotCount) {
        this.slotCount = slotCount;
        this.itemHandler.setSize(this.slotCount);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        if (level instanceof ServerLevel serverLevel)
            serverLevel.getLightEngine().checkBlock(worldPosition);

        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
        behaviours.add(filtering =
                new FilteringBehaviour(this, new StorageBoxFilteringBox())
                        .withCallback($ -> invVersionTracker.reset()));
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
    public Component getCustomName() {
        return this.customName;
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? getCustomName() : getBlockState().getBlock().getName();
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
        if (this.level != null && this.level.isClientSide)
            ModNetwork.sendToServer(new SetSortOrderPacket(sortOrder));
        setChanged();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if (pPlayer.isSpectator()) return null;
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

        if (slotCount == 0) {
            FXNTStorage.LOGGER.debug("Migrating slot layout from previous version of Storage Box @ {}", worldPosition);
            // Slot layout is pre1.1.0
            slotCount = tag.getInt("slotCount");
            storedAmount = tag.getInt("storedAmount");
            percentageUsed = tag.getInt("percentageUsed");
            voidUpgrade = tag.getBoolean("voidUpgrade");

            // Catch edge case
            if (slotCount == 0 && tag.contains("Items") && tag.getCompound("Items").contains("Size")) {
                slotCount = tag.getCompound("Items").getInt("Size");
            }
        }
    }

    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return this.storedAmount <= 0;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return itemHandler.getStackInSlot(pSlot);
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return itemHandler.extractItem(pSlot, pAmount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        ItemStack existing = itemHandler.getStackInSlot(pSlot).copy();
        itemHandler.setStackInSlot(pSlot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        itemHandler.setStackInSlot(pSlot, pStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return !this.isRemoved()
                && Container.stillValidBlockEntity(this, pPlayer, 7);
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

    private StorageStats calculateStats() {
        int stored = 0;
        int capacity = 0;
        boolean allSlotsFull = true;
        int filledSlots = 0;

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            stored += stack.getCount();
            capacity += stack.isEmpty() ? 64 : stack.getMaxStackSize();
            if (!stack.isEmpty()) {
                filledSlots++;
                if (stack.getCount() < stack.getMaxStackSize()) allSlotsFull = false;
            } else {
                allSlotsFull = false;
            }
        }

        int emptySlots = itemHandler.getSlots() - filledSlots;
        EnumProperties.StorageUsed fillLevel;
        if (allSlotsFull) fillLevel = EnumProperties.StorageUsed.FULL;
        else if (emptySlots == 0) fillLevel = EnumProperties.StorageUsed.SLOTS_FILLED;
        else if (filledSlots > 0) fillLevel = EnumProperties.StorageUsed.HAS_ITEMS;
        else fillLevel = EnumProperties.StorageUsed.EMPTY;

        return new StorageStats(stored, capacity, fillLevel);
    }

    private int calculateStoredAmount() {
        return calculateStats().stored();
    }

    private int calculateMaxCapacity() {
        return calculateStats().capacity();
    }

    public float calculatePercentageUsed() {
        return calculateStats().percentage();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Items", itemHandler.serializeNBT());
        tag.putString("SortOrder", sortOrder.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        itemHandler.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("SortOrder", Tag.TAG_STRING)) {
            this.sortOrder = SortOrder.valueOf(tag.getString("SortOrder"));
        }
    }

    public void initBlockState(Level level) {
        BlockState newState = getBlockState()
                .setValue(STORAGE_USED, calculateStats().fillLevel())
                .setValue(VOID_UPGRADE, voidUpgrade);
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, newState, newState, Block.UPDATE_ALL);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (!pLevel.isClientSide) {
            if (tickCount++ < ConfigManager.ServerConfig.STORAGE_BOX_UPDATE_TIME.get()) return;
            tickCount = 0;

            int oldStoredAmount = this.storedAmount;
            float oldPercentageUsed = this.percentageUsed;

            StorageStats stats = calculateStats();
            this.storedAmount = stats.stored();
            this.percentageUsed = stats.percentage();

            boolean statsChanged = this.storedAmount != oldStoredAmount || this.percentageUsed != oldPercentageUsed;
            boolean fillLevelChanged = pState.getValue(StorageBox.STORAGE_USED) != stats.fillLevel();

            if (statsChanged) this.setChanged();

            if (fillLevelChanged) {
                pLevel.setBlock(pPos, pState.setValue(StorageBox.STORAGE_USED, stats.fillLevel()), Block.UPDATE_CLIENTS);
            } else if (statsChanged) {
                pLevel.sendBlockUpdated(pPos, pState, pState, Block.UPDATE_CLIENTS);
            }
        }
        super.tick();
    }

    public void transferToStorage(BlockState pState, Player pPlayer, Boolean transferAll) {
        // Get the item in the players main hand and check the hand is NOT empty and the item matches the filter (if one applied)
        ItemStack itemInHand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isVoidEnabled = pState.getValue(VOID_UPGRADE);

        /*
            Single-click: add entire stack in main hand
            double-click: add every stack in player inventory matching item filter
         */

        if (transferAll) {
            for (int i = 0; i < pPlayer.getInventory().items.size(); i++) {
                ItemStack playerStack = pPlayer.getInventory().getItem(i);
                if (playerStack.isEmpty() || !filtering.test(playerStack)) continue;

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
            if (itemInHand.isEmpty() || !filterTest(itemInHand)) return;

            ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, itemInHand, false);

            // Void Mode check
            if (remainder.getCount() > 0 && isVoidEnabled) {
                remainder = ItemStack.EMPTY;
            }

            pPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        this.setChanged();
    }

    public void transferFromStorage(Player pPlayer) {
        ItemStack filterItem = filtering.getFilter();

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(stackInSlot, filterItem)) continue;

                int maxStack = Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount());
                int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;
                ItemStack toExtract = stackInSlot.copyWithCount(amountToExtract);

                ItemHandlerHelper.giveItemToPlayer(pPlayer, toExtract);
                itemHandler.extractItem(i, amountToExtract, false);
                break;
            }
        }
        this.setChanged();
    }

    boolean filterTest(ItemStack stack) {
        if (stack.is(ModTags.Items.STORAGE_BOX_ITEM))
            return false;
        return filtering.test(stack);
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
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ThresholdSwitchObservable
    @Override
    public int getMaxValue() {
        return calculateMaxCapacity();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return calculateStoredAmount();
    }

    @Override
    public MutableComponent format(int i) {
        return CreateLang.translateDirect("create.gui.threshold_switch.currently", i);
    }

    public void applyInventoryToBlock(ItemStackHandler wrapped) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, i < wrapped.getSlots() ? wrapped.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }

    public static class StorageBoxFilteringBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return Vec3.ZERO;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction side = getSide();
            float horizontalAngle = AngleHelper.horizontalAngle(side);
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 10.8, 14.5f), horizontalAngle, Direction.Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {

            Direction facing = StorageBox.getDirectionFacing(state);

            if (facing != null && facing.getAxis().isVertical()) {
                super.rotate(level, pos, state, ms);
                return;
            }

            if (state.getBlock() instanceof StorageBox) {
                super.rotate(level, pos, state, ms);
                TransformStack.of(ms).rotateX(0f);
                return;
            }
            float yRot = AngleHelper.horizontalAngle(Objects.requireNonNull(StorageBox.getDirectionFacing(state))) + (facing == Direction.DOWN ? 180 : 0);
            TransformStack.of(ms).rotateYDegrees(yRot).rotateXDegrees(facing == Direction.DOWN ? -90 : 90);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = StorageBox.getDirectionFacing(state);
            if (facing == null) return false;
            if (facing.getAxis().isVertical()) return direction.getAxis().isHorizontal();
            return direction == facing;
        }
    }
}
