package net.fxnt.fxntstorage.backpack;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
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
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
public class BackpackEntity extends BlockEntity implements IBackpackContainer, MenuProvider, Nameable {
    private ArmorStand pendingStand;
    private int standDiscardTimer = 0;

    private int slotCount;

    private final BlockPos pos;
    private Component customName;

    private final Block block;
    private int stackMultiplier;
    private SortOrder sortOrder;

    public NonNullList<String> upgrades = NonNullList.create();
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler;
    private LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.empty();
    private final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

    private Set<UpgradeType> cachedInstalledUpgradeTypes = new HashSet<>();
    private final UpgradeDataManager upgradeData = new UpgradeDataManager();

    public BackpackEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.pos = pPos;
        this.block = pBlockState.getBlock();
        this.sortOrder = SortOrder.COUNT;

        if (this.block instanceof BackpackBlock backpackBlock) {
            this.stackMultiplier = backpackBlock.getStackMultiplier();
            this.slotCount = layout.getTotalSlots();
        }

        this.itemHandler = createItemHandler();
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(slotCount) {
            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isPlayerInteraction || slot < layout.items().getEndIndex())
                    return super.extractItem(slot, amount, simulate);
                return ItemStack.EMPTY;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (filterTest(stack))
                    return false;
                if (isPlayerInteraction)
                    return true;
                if (slot == layout.jukeboxDiscs().getStartIndex() && upgrades.contains(Util.JUKEBOX_UPGRADE)) {
                    int musicDiscSlot = layout.jukeboxDiscs().getStartIndex();
                    return stack.getItem() instanceof RecordItem
                            && itemHandler.getStackInSlot(musicDiscSlot).isEmpty()
                            && slot == musicDiscSlot;
                }
                return hasEmptyOrNonMaxSlot(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64 * stackMultiplier;
            }

            @Override
            protected int getStackLimit(int slot, ItemStack stack) {
                return Math.min(getSlotLimit(slot), stack.getMaxStackSize() * stackMultiplier);
            }

            @Override
            public CompoundTag serializeNBT() {
                ListTag nbtTagList = new ListTag();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putInt("Slot", i);
                        itemTag.putInt("ActualCount", stacks.get(i).getCount());
                        // Save stack with a count of 1 to prevent byte overflow
                        // Value will be ignored by deserializeNBT anyway
                        stacks.get(i).copyWithCount(1).save(itemTag);
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

                    // Ensure ItemStack.of always sees a valid byte count (1) so it never
                    // returns EMPTY due to overflow. Real count is ActualCount.
                    CompoundTag loadTag = itemTags;
                    if (itemTags.contains("ActualCount", Tag.TAG_INT)) {
                        loadTag = itemTags.copy();
                        loadTag.putByte("Count", (byte) 1);
                    }

                    ItemStack slotStack = ItemStack.of(loadTag);
                    if (!slotStack.isEmpty()) {
                        if (itemTags.contains("ActualCount", Tag.TAG_INT)) {
                            slotStack.setCount(itemTags.getInt("ActualCount"));
                        }
                        stacks.set(slot, slotStack);
                    }
                }
                onLoad();
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public void setSize(int size) {
                if (size == stacks.size()) return;
                FXNTStorage.LOGGER.debug("Backpack itemHandler slot count at {} is {}, but should be {}", worldPosition, stacks.size(), size);

                NonNullList<ItemStack> oldStacks = stacks;
                NonNullList<ItemStack> newStacks = NonNullList.withSize(size, ItemStack.EMPTY);

                int copySize = Math.min(oldStacks.size(), size);
                for (int i = 0; i < copySize; i++) {
                    newStacks.set(i, oldStacks.get(i));
                }

                this.stacks = newStacks;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
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

    public void setCustomName(Component hoverName) {
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
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && this.upgrades.contains(Util.JUKEBOX_UPGRADE)) {
            JukeboxHandler.stopBlock(level, pos);
        }
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder order) {
        sortOrder = order;
        if (this.level != null && this.level.isClientSide)
            ModNetwork.sendToServer(new SetSortOrderPacket(sortOrder));
        setChanged();
    }

    @Override
    public boolean isPanelExpanded(UpgradeType type) {
        return upgradeData.isPanelExpanded(type);
    }

    @Override
    public void togglePanelExpanded(UpgradeType type) {
        upgradeData.togglePanel(type);
        setChanged();
    }

    @Override
    public void clearPanelExpanded(UpgradeType type) {
        upgradeData.clearPanel(type);
        setChanged();
    }

    @Override
    public int getExpandedPanelsBitmask() {
        return upgradeData.getExpandedPanelsBitmask();
    }

    @Override
    public void setExpandedPanelsBitmask(int mask) {
        upgradeData.setExpandedPanelsBitmask(mask);
        setChanged();
    }

    @Override
    public boolean getUpgradeSetting(UpgradeDataSync.Field upgrade) {
        return upgradeData.getSetting(upgrade);
    }

    @Override
    public void setUpgradeSetting(UpgradeDataSync.Field upgrade, boolean value) {
//        if (getUpgradeSetting(upgrade) == value) return;

        upgradeData.setSetting(upgrade, value);
//        if (this.level != null && this.level.isClientSide)
//            ModNetwork.sendToServer(new UpgradeDataPacket(upgrade.getIndex(), value));
        setChanged();
    }

    @Override
    public void saveSettings() {
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(tag.getCompound("Items"));
        }
        if (itemHandler.getSlots() < slotCount) {
            itemHandler.setSize(slotCount);
        }
        if (tag.contains("Upgrades")) {
            upgrades.clear();
            ListTag upgradesList = tag.getList("Upgrades", Tag.TAG_STRING);
            for (int i = 0; i < upgradesList.size(); i++) {
                upgrades.add(i, upgradesList.getString(i));
            }
        }
        if (tag.contains("StackMultiplier")) {
            stackMultiplier = tag.getInt("StackMultiplier");
        }
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        sortOrder = (tag.contains("SortOrder", Tag.TAG_STRING)) ? SortOrder.valueOf(tag.getString("SortOrder")) : SortOrder.COUNT;

        UpgradeDataManager loadedData = UpgradeDataManager.loadFromNBT(tag);
        upgradeData.copyFrom(loadedData);
        populateDefaultsForInstalledUpgrades();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
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

        // Only persist settings for upgrades that are currently installed, so that
        // removing an upgrade cleanly strips its settings from the saved data
        Set<UpgradeType> installedForSave = new HashSet<>(UpgradeHelper.getInstalledUpgrades(itemHandler));
        upgradeData.saveToNBT(tag, installedForSave);
    }

    public ItemStack saveToItemStack(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement("BlockEntityTag");
        saveAdditional(tag);
        // Save custom display name
        if (this.customName != null) {
            CompoundTag displayTag = stack.getOrCreateTagElement("display");
            displayTag.putString("Name", Component.Serializer.toJson(this.customName));
        }

        upgradeData.saveToItem(stack);
        return stack;
    }

    public void refreshUpgrades() {
        this.upgrades.clear();

        for (int i : layout.upgrades().range()) {
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
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        this.load(tag);
    }

    private boolean hasEmptyOrNonMaxSlot(ItemStack pStack) {
        for (int i : layout.items().range()) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);

            if (stack.isEmpty() || (ItemStack.isSameItemSameTags(stack, pStack) && stack.getCount() < this.stackMultiplier * pStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    public static boolean filterTest(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    public void setPendingStand(ArmorStand stand) {
        this.pendingStand = stand;
    }

    public void serverTick(Level level) {
        if (!level.isClientSide) {

            // Discard Magnet Pickup Entity after game has processed spawn and item pickup packets
            if (pendingStand != null) {
                if (standDiscardTimer++ >= 5) { // Discard after 5 ticks
                    pendingStand.discard();
                    pendingStand = null;
                    standDiscardTimer = 0;
                }
            }

            for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
                if (upgrade.getType().equals(UpgradeType.MAGNET) || upgrade.getType().equals(UpgradeType.JUKEBOX)) {
                    UpgradeContext ctx = UpgradeContext.forBlock(
                            this, level, BackpackMenu.BackpackType.BLOCK, worldPosition
                    );
                    upgrade.tick(ctx);
                }
            }
        }
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
    public boolean stillValid(Player player) {
        return !this.isRemoved()
                && Container.stillValidBlockEntity(this, player, 8);
    }

    private void populateDefaultsForInstalledUpgrades() {
        Set<UpgradeType> installedTypes = new HashSet<>(UpgradeHelper.getInstalledUpgrades(itemHandler));
        for (UpgradeType type : installedTypes) {
            IUpgrade upgrade = UpgradeRegistry.get(type);
            if (upgrade == null) continue;
            for (UpgradeDataSync.Field field : upgrade.getSettings()) {
                // Only insert the registry default if no value is already recorded
                // This preserves any value that was loaded from NBT or set by the player
                if (!upgradeData.hasSetting(field)) {
                    upgradeData.setSetting(field, UpgradeRegistry.getDefaultSetting(field));
                }
            }
        }
    }

    @Override
    public void setChanged() {
        refreshUpgrades();

        Set<UpgradeType> currentInstalledTypes = new HashSet<>(UpgradeHelper.getInstalledUpgrades(itemHandler));

        if (!currentInstalledTypes.equals(cachedInstalledUpgradeTypes)) {
            for (UpgradeType type : UpgradeType.values()) {
                if (currentInstalledTypes.contains(type)) continue;

                IUpgrade upgrade = UpgradeRegistry.get(type);
                if (upgrade != null) {
                    for (UpgradeDataSync.Field field : upgrade.getSettings()) {
                        upgradeData.clearSetting(field);
                    }
                }
                // Collapse the panel for this removed upgrade type
                upgradeData.clearPanel(type);
            }

            populateDefaultsForInstalledUpgrades();
            cachedInstalledUpgradeTypes = currentInstalledTypes;
        }
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

    public int calcRedstoneFromInventory() {
        int itemsFound = 0;
        float proportion = 0.0F;

        for (int i : layout.items().range()) {
            ItemStack itemstack = this.itemHandler.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                proportion += (float) itemstack.getCount() / (itemstack.getMaxStackSize() * this.stackMultiplier);
                itemsFound++;
            }
        }

        proportion /= layout.items().getEndIndex();
        return Mth.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new BackpackMenu(ModMenuTypes.BACKPACK_MENU.get(), pContainerId, pPlayerInventory, this, BackpackMenu.BackpackType.BLOCK, pos);
    }
}
