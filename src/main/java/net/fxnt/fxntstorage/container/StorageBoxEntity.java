package net.fxnt.fxntstorage.container;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.math.BlockFace;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.container.util.StorageBoxFilteringBox;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.fxnt.fxntstorage.container.StorageBox.VOID_UPGRADE;

public class StorageBoxEntity extends SmartBlockEntity implements Container, MenuProvider, Nameable, ThresholdSwitchObservable {
    public int slotCount = 0;

    private Component customName;

    public BlockPos pos;
    public int lastTick = 0;
    public int storedAmount = -1;
    public int percentageUsed = 0;
    public boolean doTick = false;
    public boolean voidUpgrade;
    public int updateEveryXTicks = ConfigManager.CommonConfig.STORAGE_BOX_UPDATE_TIME.get();

    public FilteringBehaviour filtering;
    public InvManipulationBehaviour invManipulation;
    public VersionedInventoryTrackerBehaviour invVersionTracker;

    private final ItemStackHandler items = createItemHandler();
    private final Lazy<IItemHandlerModifiable> itemHandler = Lazy.of(() -> items);

    public StorageBoxEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.pos = pos;
        this.voidUpgrade = state.getValue(VOID_UPGRADE);
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
                return level != null && filterTest(level, stack);
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

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler.get();
    }

    public void initializeEntity(int slotCount) {
        this.slotCount = slotCount;
        this.items.setSize(this.slotCount);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        invManipulation = new InvManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, Objects.requireNonNull(StorageBox.getDirectionFacing(s))).getOpposite());
        behaviours.add(invManipulation);
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
        filtering = new FilteringBehaviour(this, new StorageBoxFilteringBox());
        behaviours.add(1, filtering);
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
        return customName;
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Override
    public @Nullable Component getCustomName() {
        return customName;
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return customName != null ? customName : getBlockState().getBlock().getName();
    }

    @NotNull
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        extraData.writeBlockPos(this.worldPosition);
        return new StorageBoxMenu(pContainerId, pPlayerInventory, extraData);
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            items.setStackInSlot(i, itemStacks.get(i));
        }
    }

    private void writeStoredData(CompoundTag tag) {
        tag.putInt("SlotCount", slotCount);
        tag.putInt("StoredAmount", calculateStoredAmount());
        tag.putInt("PercentageUsed", calculatePercentageUsed());
        tag.putBoolean("VoidUpgrade", voidUpgrade);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeStoredData(tag);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Items", items.serializeNBT(registries));
        writeStoredData(tag);
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        slotCount = tag.getInt("SlotCount");
        storedAmount = tag.getInt("StoredAmount");
        percentageUsed = tag.getInt("PercentageUsed");
        voidUpgrade = tag.getBoolean("VoidUpgrade");
        if (tag.contains("CustomName", CompoundTag.TAG_STRING))
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of());
        for (int i = 0; i < items.getSlots(); ++i) {
            stacks.add(items.getStackInSlot(i));
        }
        return stacks;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        setVoidUpgrade(componentInput.getOrDefault(ModDataComponents.VOID_UPGRADE, false));
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.VOID_UPGRADE, this.voidUpgrade);
        components.set(DataComponents.CUSTOM_NAME, this.customName);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getStacks()));
    }

    public int getContainerSize() {
        return this.items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return this.storedAmount < 1;
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return items.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        items.extractItem(pSlot, pAmount, false);
        return items.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        items.insertItem(pSlot, ItemStack.EMPTY, false);
        return items.getStackInSlot(pSlot);
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        items.setStackInSlot(pSlot, pStack);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return true;
    }

    public int getStoredAmount() {
        return calculateStoredAmount();
    }

    public int getPercentageUsed() {
        return calculatePercentageUsed();
    }

    public int calculateStoredAmount() {
        int storedAmount = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            storedAmount += items.getStackInSlot(i).getCount();
        }
        return storedAmount;
    }

    private int calculateMaxValue() {
        int totalSpace = 0;
        int maxItemStackSize = this.getMaxStackSize();
        for (int i = 0; i < items.getSlots(); ++i) {
            if (!items.getStackInSlot(i).isEmpty()) {
                maxItemStackSize = items.getStackInSlot(i).getMaxStackSize();
            }
            totalSpace += maxItemStackSize;
        }
        return totalSpace;
    }

    private int calculateCurrentValue() {
        int usedSpace = 0;
        for (int i = 0; i < items.getSlots(); ++i) {
            usedSpace += items.getStackInSlot(i).getCount();
        }
        return usedSpace;
    }

    public int calculatePercentageUsed() {
        double percentageUsed = 0;
        int totalSpace = 0;
        int usedSpace = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            int amountInSlot = items.getStackInSlot(i).getCount();
            int maxItemStackSize = this.getMaxStackSize();
            if (!items.getStackInSlot(i).isEmpty()) {
                maxItemStackSize = items.getStackInSlot(i).getMaxStackSize();
            }
            totalSpace += maxItemStackSize;
            usedSpace += amountInSlot;
        }
        if (totalSpace > 0) {
            percentageUsed = ((double) usedSpace / totalSpace) * 100;
        }
        return (int) Math.round(percentageUsed);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Items", items.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (!pLevel.isClientSide) {
            lastTick++;

            if (lastTick >= updateEveryXTicks) {
                lastTick = 0;
                doTick = true;
            }
            if (!doTick) return;

            BlockState currentState = this.getBlockState();
            this.storedAmount = calculateStoredAmount();
            this.percentageUsed = calculatePercentageUsed();
            pLevel.sendBlockUpdated(pPos, pState, currentState, Block.UPDATE_ALL);

            int totalSlots = items.getSlots();
            boolean allSlotsFull = true;

            int filledSlots = 0;
            for (int i = 0; i < slotCount; i++) {
                ItemStack slot = items.getStackInSlot(i);
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
                if (playerStack.isEmpty() || !ItemStack.isSameItemSameComponents(filterItem, playerStack)) continue;

                // Transfer items to the container
                ItemStack remainder = ItemHandlerHelper.insertItem(items, playerStack, false);

                // Void Mode check
                if (remainder.getCount() > 0 && isVoidEnabled) {
                    remainder = ItemStack.EMPTY;
                }

                // Update player inventory with remaining items
                pPlayer.getInventory().setItem(i, remainder);
            }
        } else {
            if (itemInHand.isEmpty() || !filterTest(pLevel, itemInHand)) return;

            ItemStack remainder = ItemHandlerHelper.insertItem(items, itemInHand, false);

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

        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stackInSlot = items.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(items.getStackInSlot(i), filterItem)) continue;

                int maxStack = Math.min(items.getStackInSlot(i).getMaxStackSize(), items.getStackInSlot(i).getCount());
                int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;
                ItemStack toExtract = stackInSlot.copyWithCount(amountToExtract);

                ItemHandlerHelper.giveItemToPlayer(pPlayer, toExtract);
                items.extractItem(i, amountToExtract, false);
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

    public void setVoidUpgrade(Boolean bool) {
        if (this.voidUpgrade != bool) this.toggleVoidUpgrade();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.getSlots(); ++i) {
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
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

}
