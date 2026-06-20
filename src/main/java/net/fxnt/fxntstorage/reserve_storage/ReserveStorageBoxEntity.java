package net.fxnt.fxntstorage.reserve_storage;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.utility.CreateLang;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReserveStorageBoxEntity extends BlockEntity implements Container, MenuProvider, Nameable, ThresholdSwitchObservable {
    static final int SLOT_COUNT = 36;
    static final int STORAGE_SLOTS = 27;
    static final int GHOST_SLOT_OFFSET = 27;
    static final int GHOST_SLOTS = 9;

    private Component customName = null;
    private SortOrder sortOrder = SortOrder.COUNT;
    private int tickCount = 0;
    private int storedAmount = 0;
    private float percentageUsed = 0;

    private final ItemStackHandler itemHandler = createItemHandler();
    private final ReserveStorageBoxAutomationHandler automationHandler = new ReserveStorageBoxAutomationHandler(itemHandler);

    private record StorageStats(int stored, int capacity, EnumProperties.StorageUsed fillLevel) {
        float percentage() {
            return capacity == 0 ? 0f : (stored * 100f) / capacity;
        }
    }

    public ReserveStorageBoxEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            public int getSlotLimit(int slot) {
                return slot >= 27 ? ReserveStorageBoxGhostSlot.MAX_COUNT : super.getSlotLimit(slot);
            }
        };
    }

    protected void initBlockState(Level level) {
        BlockState newState = getBlockState().setValue(ReserveStorageBox.STORAGE_USED, calculateStats().fillLevel());
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, newState, newState, Block.UPDATE_ALL);
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    public ReserveStorageBoxAutomationHandler getAutomationHandler() {
        return automationHandler;
    }

    public static int getSlotCount() {
        return SLOT_COUNT;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
        if (this.level != null && this.level.isClientSide)
            PacketDistributor.sendToServer(new SetSortOrderPacket(sortOrder));
        setChanged();
    }

    public static int getGhostSlotOffset() {
        return GHOST_SLOT_OFFSET;
    }

    @Override
    public Component getName() {
        return getDisplayName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return customName;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : getBlockState().getBlock().getName();
    }

    protected void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (tickCount++ < ConfigManager.ServerConfig.STORAGE_BOX_UPDATE_TIME.get()) return;
        tickCount = 0;

        int oldStoredAmount = this.storedAmount;
        float oldPercentageUsed = this.percentageUsed;

        StorageStats stats = calculateStats();
        this.storedAmount = stats.stored();
        this.percentageUsed = stats.percentage();

        boolean statsChanged = this.storedAmount != oldStoredAmount || this.percentageUsed != oldPercentageUsed;
        boolean fillLevelChanged = state.getValue(ReserveStorageBox.STORAGE_USED) != stats.fillLevel();

        if (statsChanged) this.setChanged();

        if (fillLevelChanged) {
            level.setBlock(pos, state.setValue(ReserveStorageBox.STORAGE_USED, stats.fillLevel()), Block.UPDATE_CLIENTS);
        } else if (statsChanged) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        customName = componentInput.get(DataComponents.CUSTOM_NAME);
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, customName);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getStacks()));
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            stacks.add(itemHandler.getStackInSlot(i));
        }
        return stacks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        StorageStats stats = calculateStats();
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("StoredAmount", stats.stored());
        tag.putFloat("PercentageUsed", stats.percentage());
        tag.putString("ReserveSlotStatus", calculateSlotStatus());
        tag.putString("SortOrder", sortOrder.name());
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        super.saveAdditional(tag, registries);
    }

    /**
     * Computes the 9-character per-slot reserve status string embedded in the
     * block entity NBT so Create includes it in the contraption's structure template
     * and sends it to the client at entity-spawn time — same as StoredAmount /
     * PercentageUsed.  Characters: F = fully met, P = partial, U = unmet, E = empty.
     */
    private String calculateSlotStatus() {
        char[] status = new char[GHOST_SLOTS]; // GHOST_SLOTS
        for (int i = 0; i < GHOST_SLOTS; i++) {
            ItemStack ghost = itemHandler.getStackInSlot(GHOST_SLOT_OFFSET + i); // GHOST_SLOT_OFFSET
            if (ghost.isEmpty()) {
                status[i] = 'E';
                continue;
            }
            int required = ghost.getCount();
            int stored = 0;
            for (int j = 0; j < STORAGE_SLOTS; j++) { // STORAGE_SLOTS
                ItemStack slot = itemHandler.getStackInSlot(j);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, ghost)) {
                    stored += slot.getCount();
                }
            }
            status[i] = stored >= required ? 'F' : stored > 0 ? 'P' : 'U';
        }
        return new String(status);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        storedAmount = tag.getInt("StoredAmount");
        percentageUsed = tag.getFloat("PercentageUsed");
        if (tag.contains("SortOrder", Tag.TAG_STRING))
            sortOrder = SortOrder.valueOf(tag.getString("SortOrder"));
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player.isSpectator()) return null;
        return new ReserveStorageBoxMenu(containerId, playerInventory, this);
    }

    @Override
    public int getMaxValue() {
        return calculateStats().capacity();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return calculateStats().stored();
    }

    @Override
    public MutableComponent format(int value) {
        return CreateLang.translateDirect("create.gui.threshold_switch.currently", value);
    }

    private StorageStats calculateStats() {
        int stored = 0;
        int capacity = 0;
        boolean allSlotsFull = true;
        int filledSlots = 0;

        for (int i = 0; i < 27; i++) {
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

        int emptySlots = SLOT_COUNT - 9 - filledSlots;
        EnumProperties.StorageUsed fillLevel;
        if (allSlotsFull) fillLevel = EnumProperties.StorageUsed.FULL;
        else if (emptySlots == 0) fillLevel = EnumProperties.StorageUsed.SLOTS_FILLED;
        else if (filledSlots > 0) fillLevel = EnumProperties.StorageUsed.HAS_ITEMS;
        else fillLevel = EnumProperties.StorageUsed.EMPTY;

        return new StorageStats(stored, capacity, fillLevel);
    }

    public int getPercentageUsed() {
        return Math.round(calculateStats().percentage());
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return calculateStats().stored() < 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && Container.stillValidBlockEntity(this, player, 0);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < STORAGE_SLOTS;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot < STORAGE_SLOTS;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void applyInventoryToBlock(ItemStackHandler wrapped) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, i < wrapped.getSlots() ? wrapped.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }
}
