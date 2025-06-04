package net.fxnt.fxntstorage.backpack.main;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.network.SyncNBTDataPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BackpackContainer implements IBackpackContainer, ICapabilityProvider, IItemHandlerModifiable {
    private final int CONTAINER_SIZE = BackpackBlock.getSlotCount();
    private final Player player;
    private int stackMultiplier;
    private final ItemStack stack;

    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE);
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private final NonNullList<String> upgrades = NonNullList.create();

    private SortOrder sortOrder;

    public BackpackContainer(ItemStack itemStack, Player player) {
        this.player = player;
        this.stack = itemStack;

        if (itemStack.getItem() instanceof BackpackItem backpackItem &&
                backpackItem.getBlock() instanceof BackpackBlock backpackBlock) {
            this.stackMultiplier = backpackBlock.getStackMultiplier();
        }
        loadItemsFromStack(itemStack);
    }

    public void setTag(CompoundTag tag) {
        if (!player.level().isClientSide) return;
        this.stack.setTag(tag);
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        CompoundTag blockEntityTag = itemStack.getTagElement("BlockEntityTag");

        if (blockEntityTag != null && blockEntityTag.contains("Items")) {
            ListTag listTag = blockEntityTag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag tag = listTag.getCompound(i);
                int slot = tag.getByte("Slot") & 255;

                // Create an ItemStack
                ItemStack slotStack = ItemStack.of(tag);

                // Check for ActualCount and set count accordingly
                if (tag.contains("ActualCount", Tag.TAG_INT)) {
                    int actualCount = tag.getInt("ActualCount");
                    slotStack.setCount(Math.max(actualCount, 0));
                }

                if (slot < this.itemHandler.getSlots()) {
                    this.itemHandler.setStackInSlot(slot, slotStack);
                }
            }

            this.upgrades.clear();
            ListTag upgradesList = blockEntityTag.getList("Upgrades", Tag.TAG_STRING);
            for (int i = 0; i < upgradesList.size(); i++) {
                this.upgrades.add(i, upgradesList.getString(i));
            }

            if (blockEntityTag.contains("StackMultiplier", Tag.TAG_INT)) {
                this.stackMultiplier = blockEntityTag.getInt("StackMultiplier");
            }

            sortOrder = (blockEntityTag.contains("SortOrder", Tag.TAG_STRING))
                    ? SortOrder.valueOf(blockEntityTag.getString("SortOrder"))
                    : SortOrder.COUNT;
        }
    }

    public CompoundTag saveItemsToStack() {
        CompoundTag tag = new CompoundTag();

        // Save items
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                itemTag.putInt("ActualCount", itemHandler.getStackInSlot(i).getCount());
                itemHandler.getStackInSlot(i).save(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", itemHandler.getSlots());
        tag.put("Items", nbt);

        // Save upgrades
        ListTag upgradesList = new ListTag();
        for (int i = 0; i < this.upgrades.size(); ++i) {
            upgradesList.add(i, StringTag.valueOf(this.upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("MaxStackSize", this.stackMultiplier);
        tag.putString("SortOrder", this.sortOrder.name());

        return tag;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(capability, lazyItemHandler);
    }

    public NonNullList<String> getUpgrades() {
        return this.upgrades;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        if (this.player.level().isClientSide)
            ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.SET_SORT_ORDER, new FriendlyByteBuf(Unpooled.buffer()).writeEnum(sortOrder)));
        setDataChanged();
    }

    @Override
    public int getSlots() {
        return this.itemHandler.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int i) {
        return this.itemHandler.getStackInSlot(i);
    }

    @Override
    public void setStackInSlot(int i, @NotNull ItemStack itemStack) {
        this.itemHandler.setStackInSlot(i, itemStack);
    }

    @Override
    public @NotNull ItemStack insertItem(int i, @NotNull ItemStack itemStack, boolean b) {
        return this.itemHandler.insertItem(i, itemStack, b);
    }

    @Override
    public @NotNull ItemStack extractItem(int i, int i1, boolean b) {
        return this.itemHandler.extractItem(i, i1, b);
    }

    @Override
    public int getSlotLimit(int i) {
        return this.itemHandler.getSlotLimit(i);
    }

    @Override
    public boolean isItemValid(int i, @NotNull ItemStack itemStack) {
        return true;
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public int getStackMultiplier() {
        return this.stackMultiplier;
    }

    @Override
    public void setPlayerInteraction(boolean isPlayer) {
        // noop
    }

    @Override
    public void setDataChanged() {
        if (this.player.level().isClientSide) return;
        this.setChanged();
    }

    public void refreshUpgrades() {
        this.upgrades.clear();
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.ITEM_SLOT_COUNT + BackpackBlock.TOOL_SLOT_COUNT;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.UPGRADE_SLOT_COUNT;

        for (int i = UPGRADE_SLOT_START_INDEX; i < UPGRADE_SLOT_END_INDEX; i++) {
            ItemStack itemStack = this.itemHandler.getStackInSlot(i);
            if (itemStack.getItem() instanceof UpgradeItem upgradeItem) {
                String upgradeName = upgradeItem.getUpgradeName();
                if (!this.upgrades.contains(upgradeName)) {
                    this.upgrades.add(upgradeName);
                }
            }
        }
    }

    public void setChanged() {
        if (this.player.level().isClientSide()) return;

        NonNullList<ItemStack> oldItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        NonNullList<ItemStack> newItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");

        if (blockEntityTag == null) {
            CompoundTag tag = new CompoundTag();
            ContainerHelper.saveAllItems(tag, NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY));
            ItemStack itemStack1 = stack;
            itemStack1.addTagElement("BlockEntityTag", tag);
            blockEntityTag = BlockItem.getBlockEntityData(itemStack1);
        } else {
            ListTag listTag = blockEntityTag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag tag = listTag.getCompound(i);
                int slot = tag.getByte("Slot") & 255;

                ItemStack slotStack = ItemStack.of(tag);

                if (tag.contains("ActualCount", Tag.TAG_INT)) {
                    int actualCount = tag.getInt("ActualCount");
                    slotStack.setCount(Math.max(actualCount, 0));
                }

                if (slot < oldItemStacks.size()) {
                    oldItemStacks.set(slot, slotStack);
                }
            }
        }

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            newItemStacks.set(i, itemHandler.getStackInSlot(i));
        }

        SortOrder oldSort = (blockEntityTag.contains("SortOrder")) ? SortOrder.valueOf(blockEntityTag.getString("SortOrder")) : SortOrder.COUNT;
        SortOrder newSort = (sortOrder != null) ? sortOrder : SortOrder.COUNT;

        if (!itemStacksAreSame(oldItemStacks, newItemStacks) || !newSort.equals(oldSort)) {
            refreshUpgrades();
            CompoundTag tag = saveItemsToStack();
            stack.getOrCreateTag().put("BlockEntityTag", tag);
            ModNetwork.sendToPlayer((ServerPlayer) player, new SyncNBTDataPacket(stack));
        }
    }

    private static boolean itemStacksAreSame(NonNullList<ItemStack> oldStack, NonNullList<ItemStack> newStack) {
        if (oldStack.size() != newStack.size()) return false;

        for (int i = 0; i < oldStack.size(); ++i) {
            if (!ItemStack.matches(oldStack.get(i), newStack.get(i))) {
                return false;
            }
        }
        return true;
    }

}
