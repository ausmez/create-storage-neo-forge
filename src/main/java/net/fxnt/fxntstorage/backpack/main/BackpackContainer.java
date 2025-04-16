package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BackpackContainer implements IBackpackContainer, IItemHandlerModifiable {
    private final int CONTAINER_SIZE = BackpackBlock.getSlotCount();
    private final Player player;

    private int stackMultiplier;
    private final ItemStack stack;

    private final ItemStackHandler itemHandler = new ItemStackHandler(CONTAINER_SIZE);
    private final NonNullList<String> upgrades = NonNullList.create();

    private ItemStackHandler cachedHandler;

    public BackpackContainer(ItemStack itemStack, @Nullable Player player) {
        this.player = player;
        this.stack = itemStack;

        if (itemStack.getItem() instanceof BackpackItem backpackItem &&
                backpackItem.getBlock() instanceof BackpackBlock backpackBlock) {
            this.stackMultiplier = backpackBlock.getStackMultiplier();
        }
        loadItemsFromStack(itemStack);
    }

    public ItemStackHandler getItemHandler() {
//        return this.itemHandler;
        if (cachedHandler == null) {
            cachedHandler = itemHandler;
        }
        return cachedHandler;
    }

    public void setContents(DataComponentPatch componentPatch) {
        if (player == null || !player.level().isClientSide) return;
        this.stack.applyComponents(componentPatch);
    }

    public void readInventory(ItemContainerContents contents) {
        List<ItemStack> itemStacks = contents.stream().toList();

        for (int i = 0; i < itemStacks.size(); i++) {
            itemHandler.setStackInSlot(i, itemStacks.get(i));
        }
    }

    public void loadItemsFromStack(ItemStack itemStack) {
        if (itemStack.getComponents().has(DataComponents.CONTAINER)) {
            readInventory(itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        }

        if (itemStack.getComponents().has(ModDataComponents.BACKPACK_UPGRADES)) {
            this.upgrades.clear();
            List<String> upgradeList = itemStack.getComponents().get(ModDataComponents.BACKPACK_UPGRADES);

            for (int i = 0; i < upgradeList.size(); ++i) {
                this.upgrades.add(i, upgradeList.get(i));
            }
        }

        if (itemStack.getComponents().has(ModDataComponents.BACKPACK_STACK_MULTIPLIER)) {
            this.stackMultiplier = itemStack.getComponents().get(ModDataComponents.BACKPACK_STACK_MULTIPLIER);
        }
    }

    public void saveItemsToStack() {
        this.stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
        this.stack.set(ModDataComponents.BACKPACK_UPGRADES, this.upgrades);
        this.stack.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, this.stackMultiplier);
    }

    public List<ItemStack> getItems() {
        List<ItemStack> itemList = new ArrayList<>();
        for (int i = 0; i < this.itemHandler.getSlots(); ++i) {
            itemList.add(this.itemHandler.getStackInSlot(i));
        }
        return itemList;
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
    public int getStackMultiplier() {
        return this.stackMultiplier;
    }

    @Override
    public void setPlayerInteraction(boolean isPlayer) {
        // noop
    }

    @Override
    public void setDataChanged() {
        if (this.player == null || this.player.level().isClientSide) return;
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
        if (this.player == null || this.player.level().isClientSide) return;

        var cap = this.stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (cap != null) {

            NonNullList<ItemStack> oldInventory = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
            NonNullList<ItemStack> newInventory = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

            for (int i = 0; i < cap.getSlots(); ++i) {
                oldInventory.set(i, cap.getStackInSlot(i));
            }

            for (int j = 0; j < this.itemHandler.getSlots(); ++j) {
                newInventory.set(j, this.itemHandler.getStackInSlot(j));
            }

            if (!newInventory.equals(oldInventory)) {
                refreshUpgrades();
                saveItemsToStack();
//                FXNTStorage.LOGGER.debug("Saving stack in BackpackContainer");
//                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncDataComponentPacket(this.stack.getComponentsPatch()));
            }
        }
    }

}
