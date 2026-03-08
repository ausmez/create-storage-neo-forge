package net.fxnt.fxntstorage.backpack;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.network.packet.UpgradeDataPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class BackpackEntity extends BlockEntity implements IBackpackContainer, MenuProvider, Nameable {
    private int slotCount;

    private final BlockPos pos;
    private Component customName;

    private final Block block;
    private int stackMultiplier;
    private SortOrder sortOrder;

    public final NonNullList<String> upgrades = NonNullList.create();
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler itemHandler;
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
                    return (stack.has(DataComponents.JUKEBOX_PLAYABLE)
                            && itemHandler.getStackInSlot(musicDiscSlot).isEmpty()
                            && slot == musicDiscSlot
                    );
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
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
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

    public void setData(int slotCount, int stackMultiplier) {
        // Called when Block creates entity
        this.slotCount = slotCount;
        this.stackMultiplier = stackMultiplier;
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
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        if (this.level != null && this.level.isClientSide)
            PacketDistributor.sendToServer(new SetSortOrderPacket(sortOrder));
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
    public boolean getUpgradeSetting(UpgradeDataSync.Field setting) {
        return upgradeData.getSetting(setting);
    }

    @Override
    public void setUpgradeSetting(UpgradeDataSync.Field setting, boolean value) {
        if (getUpgradeSetting(setting) == value) return;

        upgradeData.setSetting(setting, value);
        if (this.level != null && this.level.isClientSide)
            PacketDistributor.sendToServer(new UpgradeDataPacket(setting.getIndex(), value));
        setChanged();
    }

    @Override
    public void saveSettings() {
        setChanged();
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

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            stacks.add(itemHandler.getStackInSlot(i));
        }
        return stacks;
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    private boolean hasEmptyOrNonMaxSlot(ItemStack pStack) {
        for (int i : layout.items().range()) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);

            if (stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, pStack) && stack.getCount() < this.stackMultiplier * pStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    public static boolean filterTest(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
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
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        componentInput.get(ModDataComponents.BACKPACK_STACK_MULTIPLIER);
        componentInput.get(ModDataComponents.BACKPACK_UPGRADES);
        componentInput.getOrDefault(ModDataComponents.INVENTORY_SORT_ORDER, SortOrder.COUNT);
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

        upgradeData.setExpandedPanelsBitmask(Math.max(0, componentInput.getOrDefault(ModDataComponents.BACKPACK_ACTIVE_PANELS, 0)));
        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            DataComponentType<Boolean> component = ModDataComponents.getComponentForField(field);
            if (component != null) {
                boolean defaultSetting = UpgradeRegistry.getDefaultSetting(field);
                upgradeData.setSetting(field, componentInput.getOrDefault(component, defaultSetting));
            }
        }
        populateDefaultsForInstalledUpgrades();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, this.stackMultiplier);
        components.set(ModDataComponents.BACKPACK_UPGRADES, this.upgrades);
        components.set(ModDataComponents.INVENTORY_SORT_ORDER, this.sortOrder);
        components.set(ModDataComponents.BACKPACK_ACTIVE_PANELS, upgradeData.getExpandedPanelsBitmask());
        components.set(DataComponents.CUSTOM_NAME, this.customName);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getStacks()));

        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            DataComponentType<Boolean> component = ModDataComponents.getComponentForField(field);
            if (component != null) {
                boolean defaultSetting = UpgradeRegistry.getDefaultSetting(field);
                components.set(component, upgradeData.getSetting(field, defaultSetting));
            }
        }
    }

    public ItemStack saveToItemStack(ItemStack stack) {
        stack.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, this.stackMultiplier);
        stack.set(ModDataComponents.BACKPACK_UPGRADES, this.upgrades);
        stack.set(ModDataComponents.INVENTORY_SORT_ORDER, this.sortOrder);
        stack.set(DataComponents.CUSTOM_NAME, this.customName);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getStacks()));

        upgradeData.saveToItem(stack);
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        ListTag upgradesList = new ListTag();
        for (int i = 0; i < upgrades.size(); i++) {
            upgradesList.add(i, StringTag.valueOf(upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("StackMultiplier", stackMultiplier);
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        tag.putString("SortOrder", sortOrder.name());

        // Only persist settings for upgrades that are currently installed, so that
        // removing an upgrade cleanly strips its settings from the saved data
        Set<UpgradeType> installedForSave = new HashSet<>(UpgradeHelper.getInstalledUpgrades(itemHandler));
        upgradeData.saveToNBT(tag, installedForSave);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
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
        stackMultiplier = (tag.contains("StackMultiplier")) ? tag.getInt("StackMultiplier") : this.stackMultiplier;
        if (tag.contains("CustomName", Tag.TAG_STRING))
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
        sortOrder = (tag.contains("SortOrder", CompoundTag.TAG_STRING)) ? SortOrder.valueOf(tag.getString("SortOrder")) : SortOrder.COUNT;

        UpgradeDataManager loadedData = UpgradeDataManager.loadFromNBT(tag);
        upgradeData.copyFrom(loadedData);
        populateDefaultsForInstalledUpgrades();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putString("SortOrder", sortOrder.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        itemHandler.deserializeNBT(lookupProvider, tag.getCompound("Items"));
        if (tag.contains("SortOrder", CompoundTag.TAG_STRING))
            this.sortOrder = SortOrder.valueOf(tag.getString("SortOrder"));
    }


    public void serverTick(Level level) {
        if (!level.isClientSide) {
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
    public boolean stillValid(Player player) {
        return !this.isRemoved()
                && Container.stillValidBlockEntity(this, player, 0);
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
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BackpackMenu(ModMenuTypes.BACKPACK_MENU.get(), containerId, inventory, this, BackpackMenu.BackpackType.BLOCK, this.pos);
    }
}
