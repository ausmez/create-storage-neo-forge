package net.fxnt.fxntstorage.backpack.upgrade.pickblock;

import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PickBlockHandler {

    public static void pickBlockHandler(@NotNull Player player, IBackpackContainer container, @NotNull ItemStack pickedItem) {
        Inventory inventory = player.getInventory();
        int matchingSlot = getItemSlotFromContainer(container, pickedItem.getItem());
        if (matchingSlot == -1) return;

        ItemStack backpackStack = container.getItemHandler().getStackInSlot(matchingSlot);
        int hotbarSlot = inventory.selected;

        if (!inventory.contains(pickedItem)) {
            placeFromBackpackToHotbar(inventory, container, backpackStack, hotbarSlot);
        } else {
            topUpHotbarFromBackpack(inventory, container, backpackStack, pickedItem, hotbarSlot);
        }
    }

    public static int getItemSlotFromContainer(@NotNull IBackpackContainer container, Item itemToFind) {
        for (int i = 0; i < container.getItemHandler().getSlots(); i++) {
            ItemStack slotItem = container.getItemHandler().getStackInSlot(i);
            if (slotItem.is(itemToFind)) {
                return i;
            }
        }
        return -1;
    }

    private static void placeFromBackpackToHotbar(Inventory inventory, IBackpackContainer container, ItemStack backpackStack, int hotbarSlot) {
        ItemStack currentHotbar = inventory.getItem(hotbarSlot);

        if (currentHotbar.isEmpty()) {
            moveToHotbar(inventory, container, backpackStack, hotbarSlot);
            return;
        }

        // Try a free inventory slot first
        int freeSlot = inventory.getFreeSlot();
        if (freeSlot != -1) {
            inventory.setItem(freeSlot, currentHotbar.copyWithCount(currentHotbar.getCount()));
            moveToHotbar(inventory, container, backpackStack, hotbarSlot);
            return;
        }

        // No free slot — try merging hotbar stack into an existing inventory stack
        int hotbarCount = currentHotbar.getCount();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i == hotbarSlot) continue;
            ItemStack candidate = inventory.getItem(i);
            int freeSpace = candidate.getMaxStackSize() - candidate.getCount();
            if (ItemStack.isSameItemSameTags(currentHotbar, candidate) && freeSpace >= hotbarCount) {
                inventory.setItem(i, candidate.copyWithCount(candidate.getCount() + hotbarCount));
                moveToHotbar(inventory, container, backpackStack, hotbarSlot);
                return;
            }
        }
        // No room anywhere — leave hotbar as-is
    }

    private static void topUpHotbarFromBackpack(Inventory inventory, IBackpackContainer container, ItemStack backpackStack, ItemStack pickedItem, int hotbarSlot) {
        ItemStack hotbarStack = inventory.getItem(hotbarSlot);
        if (!ItemStack.isSameItem(hotbarStack, pickedItem)) return;

        int freeSpace = hotbarStack.getMaxStackSize() - hotbarStack.getCount();
        if (freeSpace <= 0) return;

        int amountToMove = Math.min(backpackStack.getCount(), freeSpace);
        int newHotbarCount = hotbarStack.getCount() + amountToMove;

        inventory.setItem(hotbarSlot, pickedItem.copyWithCount(newHotbarCount));
        finalize(inventory, container, backpackStack, amountToMove, hotbarSlot);
    }

    private static void moveToHotbar(Inventory inventory, IBackpackContainer container, ItemStack backpackStack, int hotbarSlot) {
        int amountToMove = Math.min(backpackStack.getCount(), backpackStack.getItem().getMaxStackSize(backpackStack));
        inventory.setItem(hotbarSlot, backpackStack.copyWithCount(amountToMove));
        finalize(inventory, container, backpackStack, amountToMove, hotbarSlot);
    }

    private static void finalize(Inventory inventory, IBackpackContainer container,
                                 ItemStack backpackStack, int amountToMove, int hotbarSlot) {
        backpackStack.shrink(amountToMove);
        container.setDataChanged();
        inventory.selected = hotbarSlot;
    }
}
