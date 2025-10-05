package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

public class BackpackHelper {

    public static boolean isBackpackCuriosSlotVisible(Player player) {
        if (player == null) return false;

        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof BackpackItem))
                .map(slotResult -> slotResult.slotContext().visible())
                .orElse(false);
    }

    public static boolean isWearingBackpack(Player player, boolean checkVisibility) {
        ItemStack itemStack = getEquippedBackpackStack(player);
        if (FXNTStorage.curiosLoaded && checkVisibility
                && !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem)) {
            return BackpackHelper.isBackpackCuriosSlotVisible(player);
        }
        return !itemStack.isEmpty();
    }

    public static boolean isWearingBackpack(Player player) {
        return isWearingBackpack(player, false);
    }

    public static ItemStack getEquippedBackpackStack(LivingEntity player) {
        if (player == null) return ItemStack.EMPTY;

        // If Curios is not loaded or not present, check the chest slot directly
        if (!FXNTStorage.curiosLoaded) return checkChestSlot(player);

        // Get the Curios item handler
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);

        // If Curios capability is present, check the "back" slot for a BackpackItem
        if (curios.isPresent()) {
            Optional<ICurioStacksHandler> stacksHandler = curios.get().getStacksHandler("back");

            if (stacksHandler.isPresent()) {
                IDynamicStackHandler stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof BackpackItem) {
                        return stack;
                    }
                }
            }
        }

        // Fallback to chest slot if no backpack found
        return checkChestSlot(player);
    }

    private static ItemStack checkChestSlot(@NotNull LivingEntity player) {
        ItemStack chestSlotItem = player.getItemBySlot(EquipmentSlot.CHEST);
        return (chestSlotItem.getItem() instanceof BackpackItem) ? chestSlotItem : ItemStack.EMPTY;
    }

    public static boolean equipBackpack(Player player, ItemStack backpack) {
        if (player == null || backpack.isEmpty()) {
            return false; // Nothing to equip or player is null
        }

        // If Curios is not installed or the "back" slot is unavailable, try chest slot
        if (!FXNTStorage.curiosLoaded) return equipInChestSlot(player, backpack);

        // Get the Curios item handler
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);

        if (curios.isPresent()) {
            ICuriosItemHandler curiosInv = curios.get();
            Optional<ICurioStacksHandler> stacksHandler = curiosInv.getStacksHandler("back");

            if (stacksHandler.isPresent()) {
                IDynamicStackHandler stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).isEmpty()) {
                        curiosInv.setEquippedCurio("back", i, backpack);
                        return true;
                    }
                }
            }
        }

        return equipInChestSlot(player, backpack);
    }

    // Helper method to equip the backpack in the chest slot
    private static boolean equipInChestSlot(@NotNull Player player, ItemStack backpack) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chestStack.isEmpty()) {
            // Equip the backpack in the chest slot if it's empty
            player.setItemSlot(EquipmentSlot.CHEST, backpack);
            return true;
        }

        // Chest slot is full, try to place the backpack in the player's inventory
        return equipInPlayerInventory(player, backpack);
    }

    // Helper method to equip the backpack in the player's inventory if no other slots are available
    private static boolean equipInPlayerInventory(@NotNull Player player, ItemStack backpack) {
        // Try to find an empty slot in the player's main inventory (slots 0 to 35)
        int freeInventorySlot = player.getInventory().getFreeSlot();

        if (freeInventorySlot > -1) {
            player.getInventory().setItem(freeInventorySlot, backpack);
            return true;
        }

        // No space available in the player's inventory
        return false;
    }

    public int getItemSlotFromContainer(@NotNull IBackpackContainer container, Item itemToFind) {
        for (int i = 0; i < container.getItemHandler().getSlots(); i++) {
            ItemStack slotItem = container.getItemHandler().getStackInSlot(i);
            if (slotItem.is(itemToFind)) {
                return i;
            }
        }
        return -1;
    }

    public boolean itemEntityToBackpack(@NotNull IBackpackContainer container, @NotNull ItemEntity itemEntity, int startIndex, int endIndex) {
        ItemStack newStack = itemEntity.getItem();
        final IItemHandlerModifiable itemHandler = container.getItemHandler();

        if (endIndex == -1) endIndex = itemHandler.getSlots();

        boolean success = false;
        int i = startIndex;
        if (!newStack.isDamageableItem() && newStack.getComponentsPatch().isEmpty() && !newStack.isBarVisible()) {
            // If matching slot stack exists
            while (!newStack.isEmpty() && i < endIndex) {
                ItemStack itemStack = itemHandler.getStackInSlot(i);
                if (!itemStack.isEmpty() && ItemStack.isSameItemSameComponents(newStack, itemStack)) {
                    int totalCount = itemStack.getCount() + newStack.getCount();
                    int maxPutSize = Math.max(newStack.getMaxStackSize(), container.getStackMultiplier() * newStack.getMaxStackSize());
                    int availableSpace = maxPutSize - itemStack.getCount();

                    if (totalCount <= maxPutSize) {
                        newStack.setCount(0);
                        itemStack.setCount(totalCount);
                        success = true;
                    } else if (availableSpace < newStack.getMaxStackSize()) {
                        newStack.shrink(availableSpace);
                        itemStack.setCount(maxPutSize);
                        success = true;
                    }
                }
                ++i;
            }
        }
        if (!newStack.isEmpty()) {
            i = startIndex;
            // If matching slot doesn't exist
            while (i < endIndex) {
                ItemStack itemStack = container.getItemHandler().getStackInSlot(i);
                if (itemStack.isEmpty()) {

                    int maxPutSize = Math.max(newStack.getMaxStackSize(), container.getStackMultiplier() * newStack.getMaxStackSize());
                    int availableSpace = maxPutSize - itemStack.getCount();

                    if (newStack.getCount() > availableSpace) {
                        ItemStack inputStack = newStack.split(container.getStackMultiplier() * newStack.getMaxStackSize());
                        itemHandler.setStackInSlot(i, inputStack);
                    } else {
                        ItemStack inputStack = newStack.split(newStack.getCount());
                        itemHandler.setStackInSlot(i, inputStack);
                    }
                    success = true;
                    break;
                }
                ++i;
            }
        }
        container.setDataChanged();
        return success;
    }

}
