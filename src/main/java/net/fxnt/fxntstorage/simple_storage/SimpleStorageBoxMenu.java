package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.Objects;

import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.*;

public class SimpleStorageBoxMenu extends AbstractContainerMenu {
    public final SimpleStorageBoxEntity blockEntity;
    public final Player player;

    public SimpleStorageBoxMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }

    public SimpleStorageBoxMenu(int containerId, Inventory inventory, BlockEntity entity) {
        super(ModMenuTypes.SIMPLE_STORAGE_BOX_MENU.get(), containerId);
        this.player = inventory.player;
        this.blockEntity = ((SimpleStorageBoxEntity) entity);
        this.initSlots();
    }

    public void initSlots() {
        // Add Fake Main slot (Non-intractable)
        // Just render. Don't add slot
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(itemHandler -> {
            // Add Void slot
            this.addSlot(new SimpleStorageBoxVoidSlot(itemHandler, SimpleStorageBoxEntity.VOID_UPGRADE_SLOT, 8, 20));

            // Add Capacity Slots
            for (int i = 0; i < SimpleStorageBoxEntity.MAX_CAPACITY_UPGRADES; i++) {
                int slot = i + SimpleStorageBoxEntity.CAPACITY_UPGRADE_SLOT_START;
                int y = 58;
                int x = 8;
                this.addSlot(new SimpleStorageBoxUpgradeSlot(itemHandler, slot, x + (Util.SLOT_SIZE * i), y));
            }
        });

        Inventory playerInventory = player.getInventory();
        // Add Inventory Slots
        int xOffset = 8;
        int yOffset = 94;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInventory, y * 9 + x + 9, xOffset + Util.SLOT_SIZE * x, yOffset + y * Util.SLOT_SIZE));
            }
        }
        // Add Hot bar Slots
        yOffset = yOffset + (Util.SLOT_SIZE * 3) + 4;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, xOffset + i * Util.SLOT_SIZE, yOffset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved()
                && Container.stillValidBlockEntity(blockEntity, player, 7);
    }


    @Override
    public void removed(Player pPlayer) {
        super.removed(player);
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        blockEntity.setPlayerInteraction(true);
        int playerStartSlot = 1 + MAX_CAPACITY_UPGRADES;
        if (pSlotId >= 0 && pSlotId < playerStartSlot) {
            ItemStack itemStack = slots.get(pSlotId).getItem();
            if (itemStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                // Calculate new capacity
                int upgrades = blockEntity.getCapacityUpgrades();
                if (upgrades > 0) {
                    int storedAmount = blockEntity.getStoredAmount();
                    int stackSize = ITEM_STACK_SIZE;
                    if (!blockEntity.filterItem.isEmpty()) {
                        stackSize = blockEntity.filterItem.getMaxStackSize();
                    }
                    int capacityCheck = (BASE_CAPACITY << upgrades - 1) * stackSize;
                    if (capacityCheck < storedAmount) {
                        return;
                    }
                }
                blockEntity.setUpgradeSlotChanged(true);
            }
        }
        super.clicked(pSlotId, pButton, pClickType, player);
        blockEntity.setPlayerInteraction(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        ItemStack slotStack = slots.get(index).getItem();

        // As not adding container slots, void upgrade (container slot 1) is actually index 0 as it's the first added
        // So upgrade slot 1 (container slot 2) is index 1;
        // First player slot is maxUpgradeSlots (9) + voidSlot = 10
        int playerStartSlot = 1 + MAX_CAPACITY_UPGRADES;

        // If click player slot, if upgrade then move to upgrade slot, otherwise, don't allow inserting items
        if (index < playerStartSlot) {
            // Clicked on upgrade slot
            int playerSlot = player.getInventory().getSlotWithRemainingSpace(slotStack);
            if (playerSlot == -1) {
                playerSlot = player.getInventory().getFreeSlot();
            }
            if (playerSlot > -1) {
                ItemStack playerStack = player.getInventory().getItem(playerSlot);
                if (playerStack.isEmpty()) {
                    player.getInventory().setItem(playerSlot, slotStack.copyWithCount(1));
                } else {
                    playerStack.grow(1);
                }
                slotStack.shrink(1);
                blockEntity.setChanged();
                player.getInventory().setChanged();
                return ItemStack.EMPTY;
            }

        } else {
            // Clicked Player Slot
            if (slotStack.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())) {
                // Move to void slot
                if (!slots.get(0).hasItem()) {
                    slots.get(0).set(slotStack.copyWithCount(1));
                    slotStack.shrink(1);
                    blockEntity.setChanged();
                    player.getInventory().setChanged();
                    return slotStack;
                }
            } else if (slotStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                // Move to upgrade slot
                for (int i = 1; i <= MAX_CAPACITY_UPGRADES; i++) {
                    if (!slots.get(i).hasItem()) {
                        slots.get(i).set(slotStack.copyWithCount(1));
                        slotStack.shrink(1);
                        blockEntity.setChanged();
                        player.getInventory().setChanged();
                        return slotStack;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        int playerStartSlot = 1 + MAX_CAPACITY_UPGRADES;
        if (slot.index < playerStartSlot) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

}
