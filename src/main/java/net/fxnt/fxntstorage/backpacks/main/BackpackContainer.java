package net.fxnt.fxntstorage.backpacks.main;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.SyncNBTDataPacket;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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

    private int maxStackSize;
    private final ItemStack stack;

    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE);
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private final NonNullList<String> upgrades = NonNullList.create();

    public BackpackContainer(ItemStack itemStack, Player player) {
        this.player = player;
        this.stack = itemStack;

        if (itemStack.getItem() instanceof BackpackItem backpackItem &&
                backpackItem.getBlock() instanceof BackpackBlock backpackBlock) {
            this.maxStackSize = backpackBlock.getMaxStackSize();
        }
        loadItemsFromStack(itemStack);
    }

    public void setTag(CompoundTag tag) {
        if (!player.level().isClientSide) return;
        this.stack.setTag(tag);
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(itemStack);

        if (blockEntityTag != null && blockEntityTag.contains("Items", Tag.TAG_LIST)) {

            ListTag listTag = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
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

            if (blockEntityTag.contains("maxStackSize", Tag.TAG_INT)) {
                this.maxStackSize = blockEntityTag.getInt("maxStackSize");
            }
        }
    }

    public CompoundTag saveItemsToStack() {
        CompoundTag tag = new CompoundTag();

        // Save items
        ListTag itemsList = new ListTag();
        for (int i = 0; i < this.itemHandler.getSlots(); ++i) {
            ItemStack tagStack = this.itemHandler.getStackInSlot(i);
            if (!tagStack.isEmpty()) {
                CompoundTag tag1 = new CompoundTag();
                tag1.putByte("Slot", (byte) i);
                tag1.putInt("ActualCount", tagStack.getCount());
                tagStack.save(tag1);
                itemsList.add(tag1);
            }
        }
        tag.put("Items", itemsList);

        // Save upgrades
        ListTag upgradesList = new ListTag();
        for (int i = 0; i < this.upgrades.size(); ++i) {
            upgradesList.add(i, StringTag.valueOf(this.upgrades.get(i)));
        }
        tag.put("Upgrades", upgradesList);
        tag.putInt("maxStackSize", this.maxStackSize);

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
        return this.maxStackSize;
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
        int UPGRADE_SLOT_START_INDEX = BackpackBlock.itemSlotCount + BackpackBlock.toolSlotCount;
        int UPGRADE_SLOT_END_INDEX = UPGRADE_SLOT_START_INDEX + BackpackBlock.upgradeSlotCount;

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

        player.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {

            NonNullList<ItemStack> oldItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
            NonNullList<ItemStack> newItemStacks = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

            CompoundTag blockEntityTag = BlockItem.getBlockEntityData(stack);

            if (blockEntityTag == null) {
                CompoundTag tag = new CompoundTag();
                ContainerHelper.saveAllItems(tag, NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY));
                ItemStack itemStack1 = stack;
                itemStack1.addTagElement("BlockEntityTag", tag);
                blockEntityTag = BlockItem.getBlockEntityData(itemStack1);
            }

            ContainerHelper.loadAllItems(blockEntityTag, oldItemStacks);

            for (int i = 0; i < iItemHandler.getSlots(); i++) {
                newItemStacks.set(i, iItemHandler.getStackInSlot(i));
            }

            if (!newItemStacks.equals(oldItemStacks)) {
                refreshUpgrades();
                CompoundTag tag = saveItemsToStack();
                stack.getOrCreateTag().put("BlockEntityTag", tag);
                // TODO: Find a more efficient way of syncing data to client
                ModNetwork.sendToPlayer((ServerPlayer) player, new SyncNBTDataPacket(stack));
            }

        });
    }
}
