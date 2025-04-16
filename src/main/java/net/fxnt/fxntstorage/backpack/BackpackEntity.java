package net.fxnt.fxntstorage.backpack;

import net.fxnt.fxntstorage.backpack.main.BackpackBlockMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackAsBlockUpgradeHandler;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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
    public int stackMultiplier;

    public NonNullList<String> upgrades = NonNullList.create();
    public boolean isPlayerInteraction = false;

    private final ItemStackHandler items = createItemHandler();
    private final Lazy<IItemHandlerModifiable> itemHandler = Lazy.of(() -> items);
    private final int GHOST_SLOT = items.getSlots() - 1;

    public BackpackEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.pos = pPos;
        this.block = pBlockState.getBlock();
        if (this.block instanceof BackpackBlock backpackBlock) {
            this.stackMultiplier = backpackBlock.getStackMultiplier();
        }
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
                if (slot != GHOST_SLOT || !items.getStackInSlot(GHOST_SLOT).isEmpty() || isGhostSlotLocked)
                    return false;
                return hasEmptyOrNonMaxSlot(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
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
            return new ItemStack(ModBlocks.BACKPACK.get()).getHoverName();
        }
    }

    public void setData(int slotCount, int stackMultiplier) {
        // Called when Block Creates Entity
        this.slotCount = slotCount;
        this.stackMultiplier = stackMultiplier;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>(List.of());
        for (int i = 0; i < items.getSlots(); ++i) {
            stacks.add(items.getStackInSlot(i));
        }
        return stacks;
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            items.setStackInSlot(i, itemStacks.get(i));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        componentInput.get(ModDataComponents.BACKPACK_STACK_MULTIPLIER);
        componentInput.get(ModDataComponents.BACKPACK_UPGRADES);
        readInventory(componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, this.stackMultiplier);
        components.set(ModDataComponents.BACKPACK_UPGRADES, this.upgrades);
        components.set(DataComponents.CUSTOM_NAME, this.customName);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getStacks()));
    }

    public ItemStack saveToItemStack(ItemStack stack) {
        stack.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, this.stackMultiplier);
        stack.set(ModDataComponents.BACKPACK_UPGRADES, this.upgrades);
        stack.set(DataComponents.CUSTOM_NAME, this.customName);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getStacks()));
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        ListTag upgradesList = new ListTag();
        for (int i = 0; i < upgrades.size(); i++) {
            upgradesList.add(i, StringTag.valueOf(upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("StackMultiplier", stackMultiplier);
        if (customName != null)
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        if (tag.contains("Upgrades")) {
            upgrades.clear();
            ListTag upgradesList = tag.getList("Upgrades", Tag.TAG_STRING);
            for (int i = 0; i < upgradesList.size(); i++) {
                upgrades.add(i, upgradesList.getString(i));
            }
        }
        stackMultiplier = (tag.contains("StackMultiplier")) ? tag.getInt("StackMultiplier") : this.stackMultiplier;
        if (tag.contains("CustomName", CompoundTag.TAG_STRING))
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
    }

    public void refreshUpgrades() {
        this.upgrades.clear();
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.itemSlotCount + BackpackBlock.toolSlotCount;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.upgradeSlotCount;

        for (int i = UPGRADE_SLOT_START_INDEX; i < UPGRADE_SLOT_END_INDEX; i++) {
            ItemStack itemStack = this.items.getStackInSlot(i);
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Items", items.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        items.deserializeNBT(lookupProvider, tag.getCompound("Items"));
    }

    private boolean hasEmptyOrNonMaxSlot(ItemStack pStack) {
        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; i++) {
            ItemStack stack = this.items.getStackInSlot(i);

            if (stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, pStack) && stack.getCount() < this.stackMultiplier * pStack.getMaxStackSize())) {
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

    public void moveItems() {
        ItemStack ghostSlot = this.items.getStackInSlot(GHOST_SLOT);

        // Incoming items are placed into GHOST_SLOT
        // Move items from GHOST_SLOT to first available slot in item storage
        if (ghostSlot.isEmpty()) return;

        // Lock the slot (prevent accepting new items)
        isGhostSlotLocked = true;

        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; i++) {
            ItemStack mergeSlot = this.items.getStackInSlot(i);

            // If an empty slot is found, break out of loop
            if (mergeSlot.isEmpty()) {
                doMove(i, mergeSlot, ghostSlot);
                break;
            }

            // If a slot with the same item is found, and the slot is < maxStackSize, break out of loop
            if (ItemStack.isSameItemSameComponents(mergeSlot, ghostSlot)) {
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
            this.items.setStackInSlot(mergeSlotId, ghostStack.copy());
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
    public int getContainerSize() {
        return items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return IntStream.range(0, items.getSlots())
                .mapToObj(items::getStackInSlot)
                .allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int pSlot) {
        return items.getStackInSlot(pSlot);
    }

    @Override
    public @NotNull ItemStack removeItem(int pSlot, int pAmount) {
        return items.extractItem(pSlot, pAmount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int pSlot) {
        return items.insertItem(pSlot, ItemStack.EMPTY, false);
    }

    @Override
    public void setItem(int pSlot, @NotNull ItemStack pStack) {
        items.setStackInSlot(pSlot, pStack);
        if (pStack.getCount() > this.getStackMultiplier() * pStack.getMaxStackSize()) {
            pStack.setCount(this.getStackMultiplier() * pStack.getMaxStackSize());
        }

        this.setChanged();
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

//    @Override
//    public boolean stillValid(@NotNull Player player) {
//        return true; // TODO determine player validity
//    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory) {
        return new BackpackBlockMenu(i, inventory, this);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return getDisplayName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> itemList = NonNullList.create();
        for (int i = 0; i < this.items.getSlots(); ++i) {
            itemList.add(i, this.items.getStackInSlot(i));
        }
        return itemList;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        for (int i = 0; i < nonNullList.size(); ++i) {
            this.items.setStackInSlot(i, nonNullList.get(i));
        }
    }

    @Override
    public void clearContent() {
        // noop
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return this.items;
    }

    @Override
    public void setContents(DataComponentPatch componentPatch) {
        // noop
    }

    public int calcRedstoneFromInventory() {
        int itemsFound = 0;
        float proportion = 0.0F;

        for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; ++i) {
            ItemStack itemstack = this.items.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                proportion += (float) itemstack.getCount() / (itemstack.getMaxStackSize() * this.stackMultiplier);
                ++itemsFound;
            }
        }

        proportion /= (float) Util.ITEM_SLOT_END_RANGE;
        return Mth.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }

}
