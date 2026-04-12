package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class SimpleStorageBoxEntity extends BlockEntity implements MenuProvider, Nameable, ThresholdSwitchObservable, IHaveGoggleInformation {
    private int tickCount = 0;
    private Component customName;

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
    private boolean storageSlotChanged = false;
    private boolean upgradeSlotChanged = false;
    private boolean blockStateNeedsUpdate = false;

    private final ItemStackHandler itemHandler = createItemHandler(SLOT_COUNT);
    private final Lazy<IItemHandlerModifiable> lazyItemHandler = Lazy.of(() -> itemHandler);

    private record StorageStats(int stored, int capacity, EnumProperties.StorageUsed fillLevel) {
        float percentage() {
            return capacity == 0 ? 0f : (stored * 100f) / capacity;
        }
    }

    public SimpleStorageBoxEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    private ItemStackHandler createItemHandler(int slotCount) {
        return new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < VOID_UPGRADE_SLOT) storageSlotChanged = true;
                if (slot >= VOID_UPGRADE_SLOT) upgradeSlotChanged = true;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                ItemStack amount = super.insertItem(slot, stack, simulate);
                if (this.stacks.getFirst().getCount() >= maxItemCapacity && voidUpgrade) {
                    return ItemStack.EMPTY;
                }
                return voidUpgrade || simulate && amount.getCount() < stack.getCount() ? ItemStack.EMPTY : amount;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isPlayerInteraction || slot < VOID_UPGRADE_SLOT) {
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
            public boolean isItemValid(int slot, ItemStack stack) {
                if (stack.is(ModTags.Items.STORAGE_BOX_UPGRADE) && !isPlayerInteraction) return false;
                if (filterTest(stack)) {
                    if (slot > 0) return false;
                    if (voidUpgrade) return true;
                    return this.stacks.getFirst().getCount() < maxItemCapacity;
                }
                return false;
            }

            @Override
            protected int getStackLimit(int slot, ItemStack stack) {
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

    public IItemHandlerModifiable getItemHandler() {
        return lazyItemHandler.get();
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

    private StorageStats calculateStats() {
        int stored = itemHandler.getStackInSlot(0).getCount();
        int stackSize = filterItem.isEmpty() ? ITEM_STACK_SIZE : filterItem.getMaxStackSize();
        int capacity = (BASE_CAPACITY << getCapacityUpgrades()) * stackSize;

        EnumProperties.StorageUsed fillLevel = EnumProperties.StorageUsed.EMPTY;
        if (stored >= capacity) fillLevel = EnumProperties.StorageUsed.FULL;
        else if (stored > 0) fillLevel = EnumProperties.StorageUsed.HAS_ITEMS;

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

    public int getStoredAmount() {
        return calculateStoredAmount();
    }

    public int getMaxItemCapacity() {
        this.maxItemCapacity = calculateMaxCapacity();
        return this.maxItemCapacity;
    }

    public void setUpgradeSlotChanged(boolean hasChanged) {
        this.upgradeSlotChanged = hasChanged;
    }

    @Override
    public Component getName() {
        return customName != null ? customName : getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return customName;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : getBlockState().getBlock().getName();
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
        if (level != null && level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        if (level instanceof ServerLevel serverLevel)
            serverLevel.getLightEngine().checkBlock(worldPosition); // Re-calc light levels
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        customName = componentInput.get(DataComponents.CUSTOM_NAME);
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getStacks()));
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of());
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            stacks.add(itemHandler.getStackInSlot(i));
        }
        return stacks;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        storageSlotChanged = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.putInt("MaxItemCapacity", this.getMaxItemCapacity());  // Needed for MountedStorage
        tag.putInt("StoredAmount", this.getStoredAmount());
        tag.putBoolean("VoidUpgrade", this.hasVoidUpgrade());  // Needed for MountedStorage
        if (!filterItem.isEmpty()) {
            tag.put("FilterItem", filterItem.copyWithCount(1).saveOptional(registries));
        }
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.maxItemCapacity = tag.getInt("MaxItemCapacity"); // Needed for MountedStorage
        this.storedAmount = tag.getInt("StoredAmount");
        this.voidUpgrade = tag.getBoolean("VoidUpgrade"); // Needed for MountedStorage
        CompoundTag filterTag = tag.getCompound("FilterItem");
        filterItem = (filterTag.isEmpty()) ? ItemStack.EMPTY : ItemStack.parseOptional(registries, filterTag);
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);

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

            migrateSlotItems(itemsTag, registries, oldSize); // Slot layout migration
        } else {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }

    private void migrateSlotItems(CompoundTag itemsTag, HolderLookup.Provider registries, int oldSize) {
        ItemStackHandler oldHandler = new ItemStackHandler(oldSize);
        oldHandler.deserializeNBT(registries, itemsTag);
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

    public void initBlockState(Level level) {
        setFilter(getItemHandler().getStackInSlot(0));
        BlockState newState = getBlockState().setValue(SimpleStorageBox.STORAGE_USED, calculateStats().fillLevel());
        level.setBlock(worldPosition, newState , Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, newState, newState, Block.UPDATE_ALL);
    }

    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide) return;

        // Compute stats once when a slot changes, update the cached fields immediately
        // so the item handler always sees current capacity, then reset the dirty flags.
        // blockStateNeedsUpdate carries the intent through to the timer gate so the
        // visual block state update is deferred without re-running calculateStats().
        if (upgradeSlotChanged || storageSlotChanged) {
            StorageStats stats = calculateStats();
            this.storedAmount = stats.stored();
            this.maxItemCapacity = stats.capacity();
            upgradeSlotChanged = storageSlotChanged = false;
            blockStateNeedsUpdate = true;
        }

        if (tickCount++ < ConfigManager.ServerConfig.STORAGE_BOX_UPDATE_TIME.get()) return;
        tickCount = 0;

        if (blockStateNeedsUpdate) {
            updateBlockState(level, blockPos, blockState);
            blockStateNeedsUpdate = false;
        }
    }

    private void updateBlockState(Level level, BlockPos blockPos, BlockState blockState) {
        EnumProperties.StorageUsed status;
        if (storedAmount >= maxItemCapacity) status = EnumProperties.StorageUsed.FULL;
        else if (storedAmount > 0) status = EnumProperties.StorageUsed.HAS_ITEMS;
        else status = EnumProperties.StorageUsed.EMPTY;

        BlockState newState = blockState.getValue(SimpleStorageBox.STORAGE_USED) != status
                ? blockState.setValue(SimpleStorageBox.STORAGE_USED, status)
                : blockState;

        level.setBlock(blockPos, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(blockPos, blockState, newState, Block.UPDATE_ALL);
    }

    public void transferToStorage(Player pPlayer, Boolean transferAll) {
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
                if (playerStack.isEmpty() || !ItemStack.isSameItemSameComponents(filterItem, playerStack)) continue;

                // Transfer items to the container
                ItemStack remainder = itemHandler.insertItem(0, playerStack, false);
                pPlayer.getInventory().setItem(i, remainder);
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

                ItemStack remainder = itemHandler.insertItem(0, itemInHand, false);
                pPlayer.setItemInHand(InteractionHand.MAIN_HAND, remainder);
            }
        }
        setChanged();
    }

    public void transferFromStorage(Player pPlayer) {
        ItemStack slot0 = itemHandler.getStackInSlot(0);

        if (!slot0.isEmpty()) {
            int maxStack = Math.min(slot0.getMaxStackSize(), slot0.getCount());
            int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;

            ItemStack extracted = itemHandler.extractItem(0, amountToExtract, false);

            if (!extracted.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(pPlayer, extracted);
            }
        }
    }

    public void removeFilter() {
        this.filterItem = ItemStack.EMPTY;
        storageSlotChanged = true; // Trigger block update
    }

    public void setFilter(ItemStack itemStack) {
        this.filterItem = itemStack.copyWithCount(1);
    }

    public boolean filterTest(ItemStack stack) {
        if (stack.is(ModTags.Items.STORAGE_BOX_ITEM) || stack.is(ModTags.Items.STORAGE_BOX_UPGRADE))
            return false;
        return this.filterItem.isEmpty() || ItemStack.isSameItemSameComponents(stack, this.filterItem);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player.isSpectator()) return null;
        return new SimpleStorageBoxMenu(containerId, playerInventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    // ThresholdSwitchObservable //
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

    public void applyInventoryToBlock(ItemStackHandler wrapped, ItemStack filterItem) {
        setFilter(filterItem);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, wrapped.getStackInSlot(i));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (filterItem.isEmpty() || ConfigManager.ClientConfig.SIMPLE_STORAGE_GOGGLE_INFO.get() == ConfigManager.ClientConfig.SimpleStorageGoggleOverlay.OFF) return false;

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) return false;
        if (blockHit.getDirection() != getBlockState().getValue(SimpleStorageBox.FACING)) return false;

        boolean hasPotion = filterItem.has(DataComponents.POTION_CONTENTS);
        boolean hasEnchantments = (filterItem.has(DataComponents.ENCHANTMENTS) && !filterItem.get(DataComponents.ENCHANTMENTS).isEmpty()) || filterItem.has(DataComponents.STORED_ENCHANTMENTS);
        boolean hasTrim = filterItem.has(DataComponents.TRIM);

        if ((!hasPotion && !hasEnchantments && !hasTrim) && ConfigManager.ClientConfig.SIMPLE_STORAGE_GOGGLE_INFO.get() == ConfigManager.ClientConfig.SimpleStorageGoggleOverlay.ONLY_TAGGED) return false;

        List<Component> vanillaTooltip = filterItem.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);

        for (Component component : vanillaTooltip) {
            tooltip.add(Component.literal("    ").append(component.copy()));
        }

        return true;
    }
}
