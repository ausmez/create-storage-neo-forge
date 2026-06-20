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
import net.minecraft.core.Direction;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.neoforged.neoforge.items.IItemHandler;
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
    public boolean compactingUpgrade = false;
    public @Nullable CompactingChain compactingChain = null;
    public int compactingSelectedTier = 0;

    /*
        Slot0 = Total item count (always T0 units when compacting is active)
        Slot1 = Void/Compacting Upgrade Item slot
        Slot2-10 = Capacity Upgrade Item slots
     */
    public static final int VOID_UPGRADE_SLOT = 1;
    public static final int CAPACITY_UPGRADE_SLOT_START = 2;
    public static final int MAX_CAPACITY_UPGRADES = 9;
    public static final int BASE_SLOT_COUNT = 2;

    public static final int SLOT_COUNT = BASE_SLOT_COUNT + MAX_CAPACITY_UPGRADES; // Item Slot + Void Upgrade Slot + Capacity Upgrade Slots
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isPlayerInteraction = false;
    private boolean storageSlotChanged = false;
    private boolean upgradeSlotChanged = false;
    private boolean blockStateNeedsUpdate = false;
    private boolean pendingLightFix = false;

    final ItemStackHandler itemHandler = createItemHandler();

    private record StorageStats(int stored, int capacity, EnumProperties.StorageUsed fillLevel) {
    }

    public SimpleStorageBoxEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < VOID_UPGRADE_SLOT) {
                    storageSlotChanged = true;
                    if (filterItem.isEmpty() && !this.stacks.getFirst().isEmpty()) {
                        setFilter(this.stacks.getFirst());
                    }
                }
                if (slot == VOID_UPGRADE_SLOT) {
                    ItemStack upgradeSlot = this.stacks.get(VOID_UPGRADE_SLOT);
                    voidUpgrade = upgradeSlot.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
                    boolean nowCompacting = upgradeSlot.is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE.get());
                    if (nowCompacting && !compactingUpgrade) {
                        compactingUpgrade = true;
                        onCompactingUpgradeInstalled();
                    } else if (!nowCompacting && compactingUpgrade) {
                        compactingUpgrade = false;
                        onCompactingUpgradeRemoved();
                    }
                }
                if (slot >= VOID_UPGRADE_SLOT) upgradeSlotChanged = true;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                ItemStack amount = super.insertItem(slot, stack, simulate);
                if (this.stacks.getFirst().getCount() >= maxItemCapacity && voidUpgrade) {
                    return ItemStack.EMPTY;
                }
                return voidUpgrade ? ItemStack.EMPTY : amount;
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
                            int toExtract = Math.min(Math.min(amount, maxItemCapacity), existing.getMaxStackSize());
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
        return voidUpgrade;
    }

    public boolean hasCompactingUpgrade() {
        return compactingUpgrade;
    }

    private int compactingSlotFor(ItemStack stack) {
        if (compactingChain == null) return -1;
        int tiers = compactingChain.tiers();
        if (stack.getItem() == compactingChain.t0()) return tiers - 1;
        if (stack.getItem() == compactingChain.t1()) return tiers - 2;
        if (compactingChain.t2() != null && stack.getItem() == compactingChain.t2()) return 0;
        return -1;
    }

    public IItemHandler getCapabilityHandler() {
        if (compactingUpgrade && compactingChain != null) {
            return new CompactingItemHandler(this, compactingChain);
        }
        // Expose only slot 0 to external capability consumers such as Jade
        return storageOnlyHandler;
    }

    private final IItemHandler storageOnlyHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? itemHandler.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? itemHandler.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? itemHandler.getSlotLimit(0) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && itemHandler.isItemValid(0, stack);
        }
    };

    private void buildCompactingChain() {
        if (!filterItem.isEmpty() && level != null && !CompactingRecipeHelper.isEmpty()) {
            compactingChain = CompactingRecipeHelper.buildChain(filterItem.getItem());
            if (compactingChain != null) {
                compactingSelectedTier = Math.min(compactingSelectedTier, compactingChain.tiers() - 1);
            }
        } else {
            compactingChain = null;
        }
    }

    public void onCompactingUpgradeInstalled() { // Public for ponder scene
        compactingSelectedTier = 0;
        // Ensure recipe data is available
        if (level != null && CompactingRecipeHelper.isEmpty()) {
            CompactingRecipeHelper.rebuild(level.getRecipeManager(), level.registryAccess());
        }
        buildCompactingChain();
        if (compactingChain != null) {
            // Convert stored items to T0 if they're currently T1/T2
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (!stored.isEmpty() && stored.getItem() != compactingChain.t0()) {
                int t0Units = compactingChain.toT0Units(stored.getItem(), stored.getCount());
                if (t0Units > 0) {
                    filterItem = new ItemStack(compactingChain.t0());
                    // Capacity is now scaled off the highest tier, so recompute before clamping.
                    itemHandler.setStackInSlot(0, new ItemStack(compactingChain.t0(), Math.min(t0Units, computeCapacity())));
                }
            }
        }
    }

    private void onCompactingUpgradeRemoved() {
        if (compactingChain == null) return;

        // Convert stored T0 back to the highest possible tier, leaving that in the box and
        // ejecting the leftover (< 1 highest-tier unit) as the lowest tier
        int t0Stored = itemHandler.getStackInSlot(0).getCount();
        if (t0Stored <= 0) {
            compactingChain = null;
            return;
        }

        CompactingChain chain = compactingChain;
        CompactingChain.TierResult result = chain.toHighestTier(t0Stored);
        int remainder = chain.remainderAfterHighestTier(t0Stored);

        ItemStack highestTierStack = new ItemStack(result.item(), result.count());
        itemHandler.setStackInSlot(0, highestTierStack);
        filterItem = highestTierStack.copyWithCount(1);

        if (remainder > 0 && level != null) {
            spawnCompactingRemainder(chain, remainder);
        }
        compactingChain = null;
    }

    private void spawnCompactingRemainder(CompactingChain chain, int remainder) {
        BlockPos pos = getBlockPos();
        Direction facing = getBlockState().getValue(SimpleStorageBox.FACING);

        // Spawn at the center of the front face
        double spawnX = pos.getX() + 0.5 + facing.getStepX() * 0.6;
        double spawnY = pos.getY() + 0.5;
        double spawnZ = pos.getZ() + 0.5 + facing.getStepZ() * 0.6;

        RandomSource rng = level.getRandom();

        double vx = facing.getStepX() * 0.1 + (rng.nextDouble() - 0.5) * 0.05;
        double vy = 0.10 + rng.nextDouble() * 0.05;
        double vz = facing.getStepZ() * 0.1 + (rng.nextDouble() - 0.5) * 0.05;

        ItemEntity drop = new ItemEntity(
                level, spawnX, spawnY, spawnZ,
                new ItemStack(chain.t0(), remainder),
                vx, vy, vz
        );
        level.addFreshEntity(drop);
    }

    private StorageStats calculateStats() {
        int stored = computeStored();
        int capacity = computeCapacity();
        return new StorageStats(stored, capacity, fillLevelFor(stored, capacity));
    }

    private int computeStored() {
        return itemHandler.getStackInSlot(0).getCount();
    }

    private int capacityForUpgrades(int upgrades) {
        int stackSize = filterItem.isEmpty() ? ITEM_STACK_SIZE : filterItem.getMaxStackSize();
        int base = (BASE_CAPACITY << upgrades) * stackSize;
        if (compactingUpgrade && compactingChain != null) {
            return base * compactingChain.highestTierT0PerUnit();
        }
        return base;
    }

    private int computeCapacity() {
        return capacityForUpgrades(getCapacityUpgrades());
    }

    public boolean canRemoveCapacityUpgrade() {
        int upgrades = getCapacityUpgrades();
        if (upgrades <= 0) return true;
        return computeStored() <= capacityForUpgrades(upgrades - 1);
    }

    private static EnumProperties.StorageUsed fillLevelFor(int stored, int capacity) {
        if (stored >= capacity) return EnumProperties.StorageUsed.FULL;
        if (stored > 0) return EnumProperties.StorageUsed.HAS_ITEMS;
        return EnumProperties.StorageUsed.EMPTY;
    }

    public int getStoredAmount() {
        return computeStored();
    }

    public int getMaxItemCapacity() {
        this.maxItemCapacity = computeCapacity();
        return this.maxItemCapacity;
    }

    public int getDisplayedStoredAmount() {
        if (compactingUpgrade && compactingChain != null) {
            return computeStored() / compactingChain.highestTierT0PerUnit();
        }
        return getStoredAmount();
    }

    public int getDisplayedMaxCapacity() {
        if (compactingUpgrade && compactingChain != null) {
            return getMaxItemCapacity() / compactingChain.highestTierT0PerUnit();
        }
        return getMaxItemCapacity();
    }

    public void setUpgradeSlotChanged(boolean hasChanged) {
        this.upgradeSlotChanged = hasChanged;
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

    public ItemStack getFilterItem() {
        return this.filterItem;
    }

    public ItemStack getDisplayedItem() {
        if (compactingUpgrade && compactingChain != null) {
            int selected = Math.min(compactingSelectedTier, compactingChain.tiers() - 1);
            return compactingChain.itemForSlot(selected);
        }
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
        if (level instanceof ServerLevel)
            pendingLightFix = true; // Deferred to first tick so chunk is fully loaded
        if (compactingUpgrade) {
            if (CompactingRecipeHelper.isEmpty() && level != null) {
                CompactingRecipeHelper.rebuild(level.getRecipeManager(), level.registryAccess());
            }
            buildCompactingChain();
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
        List<ItemStack> stacks = new ArrayList<>(itemHandler.getSlots());
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
        tag.putBoolean("CompactingUpgrade", this.hasCompactingUpgrade());
        tag.putInt("CompactingSelectedTier", this.compactingSelectedTier);
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
        this.compactingUpgrade = tag.getBoolean("CompactingUpgrade");
        this.compactingSelectedTier = tag.getInt("CompactingSelectedTier");
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

        if (compactingUpgrade) buildCompactingChain();
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
        BlockState newState = getBlockState()
                .setValue(SimpleStorageBox.STORAGE_USED, calculateStats().fillLevel())
                .setValue(SimpleStorageBox.COMPACTING, hasCompactingUpgrade())
                .setValue(SimpleStorageBox.VOID_UPGRADE, hasVoidUpgrade());
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, newState, newState, Block.UPDATE_ALL);
    }

    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide) return;

        if (pendingLightFix && level instanceof ServerLevel serverLevel) {
            pendingLightFix = false;
            serverLevel.getLightEngine().checkBlock(blockPos);
            for (Direction dir : Direction.values()) {
                serverLevel.getLightEngine().checkBlock(blockPos.relative(dir));
            }
        }

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
        EnumProperties.StorageUsed status = fillLevelFor(storedAmount, maxItemCapacity);
        boolean compacting = hasCompactingUpgrade();

        BlockState newState = blockState
                .setValue(SimpleStorageBox.STORAGE_USED, status)
                .setValue(SimpleStorageBox.COMPACTING, compacting)
                .setValue(SimpleStorageBox.VOID_UPGRADE, hasVoidUpgrade());

        level.setBlock(blockPos, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(blockPos, blockState, newState, Block.UPDATE_ALL);
    }

    public void transferToStorage(Player player, boolean transferAll) {
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (itemInHand.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {
            if (itemInHand.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())
                    || itemInHand.is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE.get())) {
                // Void and compacting upgrades both live in VOID_UPGRADE_SLOT; toggle logic is identical.
                toggleUpgradeInSlot(player, itemInHand);
            } else if (itemInHand.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                boolean canAddUpgrade = false;
                for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
                    if (this.itemHandler.getStackInSlot(i).isEmpty()) {
                        this.itemHandler.setStackInSlot(i, itemInHand.copyWithCount(1));
                        canAddUpgrade = true;
                        break;
                    }
                }
                if (!player.isCreative() && canAddUpgrade) {
                    itemInHand.shrink(1);
                    player.getInventory().setChanged();
                } else if (!canAddUpgrade) {
                    player.displayClientMessage(Component.translatable("fxntstorage.storage_box_capacity_upgrade_max"), true);
                }
            }
        }

        /*
            Single-click: add entire stack in main hand
            double-click: add every stack in player inventory matching item filter
         */
        if (compactingUpgrade && compactingChain != null) {
            CompactingItemHandler compHandler = new CompactingItemHandler(this, compactingChain);
            if (transferAll) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack playerStack = player.getInventory().getItem(i);
                    if (playerStack.isEmpty()) continue;
                    int slot = compactingSlotFor(playerStack);
                    if (slot < 0) continue;
                    player.getInventory().setItem(i, compHandler.insertItem(slot, playerStack, false));
                }
            } else {
                if (itemInHand.isEmpty()) return;
                int slot = compactingSlotFor(itemInHand);
                if (slot < 0) return;
                player.setItemInHand(InteractionHand.MAIN_HAND, compHandler.insertItem(slot, itemInHand, false));
            }
        } else {
            if (transferAll) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack playerStack = player.getInventory().getItem(i);
                    if (playerStack.isEmpty() || !ItemStack.isSameItemSameComponents(filterItem, playerStack)) continue;
                    player.getInventory().setItem(i, itemHandler.insertItem(0, playerStack, false));
                }
            } else {
                if (itemInHand.isEmpty() || !filterTest(itemInHand)) return;

                int availableSpace = getMaxItemCapacity() - getStoredAmount();
                int srcAmount = itemInHand.getCount();

                if (availableSpace <= 0 && hasVoidUpgrade()) {
                    itemInHand.shrink(srcAmount);
                    return;
                }

                int moveAmount = Math.min(srcAmount, availableSpace);
                if (moveAmount > 0) {
                    if (filterItem.isEmpty()) setFilter(itemInHand);
                    player.setItemInHand(InteractionHand.MAIN_HAND, itemHandler.insertItem(0, itemInHand, false));
                }
            }
        }
        setChanged();
    }

    private void toggleUpgradeInSlot(Player player, ItemStack itemInHand) {
        if (this.itemHandler.getStackInSlot(VOID_UPGRADE_SLOT).isEmpty()) {
            this.itemHandler.setStackInSlot(VOID_UPGRADE_SLOT, itemInHand.copyWithCount(1));
            if (!player.isCreative()) {
                itemInHand.shrink(1);
                player.getInventory().setChanged();
            }
        } else {
            ItemStack existing = this.itemHandler.getStackInSlot(VOID_UPGRADE_SLOT);
            int slot = player.getInventory().getSlotWithRemainingSpace(existing);
            if (slot > -1) {
                player.getInventory().getItem(slot).grow(1);
            } else {
                slot = player.getInventory().getFreeSlot();
                if (slot > -1) player.getInventory().setItem(slot, existing);
                else player.drop(existing, false);
            }
            player.getInventory().setChanged();
            this.itemHandler.setStackInSlot(VOID_UPGRADE_SLOT, ItemStack.EMPTY);
        }
    }

    public void transferFromStorage(Player pPlayer) {
        ItemStack slot0 = itemHandler.getStackInSlot(0);
        if (!slot0.isEmpty()) {
            int maxStack = Math.min(slot0.getMaxStackSize(), slot0.getCount());
            int amountToExtract = (pPlayer.isShiftKeyDown()) ? maxStack : 1;
            ItemStack extracted = itemHandler.extractItem(0, amountToExtract, false);
            if (!extracted.isEmpty()) ItemHandlerHelper.giveItemToPlayer(pPlayer, extracted);
        }
    }

    public void transferFromStorage(Player pPlayer, int compactingSlot) {
        if (!compactingUpgrade || compactingChain == null) {
            transferFromStorage(pPlayer);
            return;
        }
        CompactingItemHandler compHandler = new CompactingItemHandler(this, compactingChain);
        compactingSlot = Math.clamp(compactingSlot, 0, compHandler.getSlots() - 1);
        ItemStack preview = compHandler.getStackInSlot(compactingSlot);
        if (preview.isEmpty()) return;
        int maxAmount = preview.getMaxStackSize();
        int amount = pPlayer.isShiftKeyDown() ? Math.min(maxAmount, preview.getCount()) : 1;
        ItemStack extracted = compHandler.extractItem(compactingSlot, amount, false);
        if (!extracted.isEmpty()) ItemHandlerHelper.giveItemToPlayer(pPlayer, extracted);
    }

    public void removeFilter() {
        this.filterItem = ItemStack.EMPTY;
        this.compactingChain = null;
        storageSlotChanged = true; // Trigger block update
    }

    public void setFilter(ItemStack itemStack) {
        this.filterItem = itemStack.copyWithCount(1);
        if (compactingUpgrade) buildCompactingChain();
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
        return computeCapacity();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return computeStored();
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
        // When a Compacting Upgrade is installed, show the currently displayed item instead of the underlying filter item
        ItemStack displayItem = getDisplayedItem();
        if (displayItem.isEmpty() || ConfigManager.ClientConfig.SIMPLE_STORAGE_GOGGLE_INFO.get() == ConfigManager.ClientConfig.SimpleStorageGoggleOverlay.OFF)
            return false;

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK)
            return false;
        if (blockHit.getDirection() != getBlockState().getValue(SimpleStorageBox.FACING)) return false;

        boolean hasPotion = displayItem.has(DataComponents.POTION_CONTENTS);
        var ench = displayItem.get(DataComponents.ENCHANTMENTS);
        boolean hasEnchantments = (ench != null && !ench.isEmpty()) || displayItem.has(DataComponents.STORED_ENCHANTMENTS);
        boolean hasTrim = displayItem.has(DataComponents.TRIM);

        if ((!hasPotion && !hasEnchantments && !hasTrim) && ConfigManager.ClientConfig.SIMPLE_STORAGE_GOGGLE_INFO.get() == ConfigManager.ClientConfig.SimpleStorageGoggleOverlay.ONLY_TAGGED)
            return false;

        List<Component> vanillaTooltip = displayItem.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);

        for (Component component : vanillaTooltip) {
            tooltip.add(Component.literal("    ").append(component.copy()));
        }

        return true;
    }
}
