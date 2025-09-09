package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PickBlockHandler {
    public static void pickBlockHandler(@NotNull Player player, IBackpackContainer container, @NotNull ItemStack pickedItem) {

        //FXNTStorage.LOGGER.info("{} Do Pick Block", player.level());
        Inventory inventory = player.getInventory();
        int matchingBackpackSlot = new BackpackHelper().getItemSlotFromContainer(container, pickedItem.getItem());

        if (matchingBackpackSlot != -1) {

            ItemStack backpackStack = container.getItemHandler().getStackInSlot(matchingBackpackSlot);

            //int hotbarSlot = inventory.getSuitableHotbarSlot();
            int hotbarSlot = player.getInventory().selected;

            if (!inventory.contains(pickedItem)) {

                // Logic when the item is not in the inventory
                // Find free hotbar slot
                if (!inventory.getItem(hotbarSlot).isEmpty()) { // Hotbar slot full
                    // Get inventory slot stack
                    ItemStack hotbarStack = inventory.getItem(hotbarSlot);
                    int hotbarStackSize = hotbarStack.getCount();

                    // Is there room to move selected hotbar slot into inventory?
                    int freeSlot = inventory.getFreeSlot();
                    if (freeSlot != -1) { // Room in inventory
                        // Move selected hotbar slot to free slot in inventory
                        ItemStack hotbarStackCopy = hotbarStack.copyWithCount(hotbarStack.getCount());
                        player.getInventory().setItem(freeSlot, hotbarStackCopy);

                        // Move backpack slot to hotbar slot
                        int amountToMove = Math.min(backpackStack.getCount(), backpackStack.getItem().getMaxStackSize(backpackStack));

                        ItemStack backPackStackCopy = backpackStack.copyWithCount(amountToMove);
                        player.getInventory().setItem(hotbarSlot, backPackStackCopy);

                        backpackStack.shrink(amountToMove);
                        container.setDataChanged();
                        inventory.selected = hotbarSlot;

                    } else { // No Room in inventory
                        for (int i = 0; i < inventory.getContainerSize(); i++) {
                            if (i != hotbarSlot) {
                                ItemStack thisStack = inventory.getItem(i);
                                int thisStackSize = thisStack.getCount();
                                int maxStackSize = thisStack.getMaxStackSize();
                                int freeSpace = maxStackSize - thisStackSize;
                                if (ItemStack.isSameItemSameTags(hotbarStack, thisStack) && freeSpace >= hotbarStackSize) { // Can merge

                                    // Merge Hotbar Slot with Inventory Slot
                                    // Move backpack slot to hotbar slot

                                    // Get inventory slot amount
                                    // Set to inventory slot amount + hotbar slot amount
                                    ItemStack newInventoryStack = thisStack.copyWithCount(thisStackSize + hotbarStackSize);
                                    player.getInventory().setItem(i, newInventoryStack);

                                    int amountToMove = Math.min(backpackStack.getCount(), backpackStack.getItem().getMaxStackSize(backpackStack));

                                    ItemStack backPackStackCopy = backpackStack.copyWithCount(amountToMove);
                                    player.getInventory().setItem(hotbarSlot, backPackStackCopy);

                                    backpackStack.shrink(amountToMove);
                                    container.setDataChanged();
                                    inventory.selected = hotbarSlot;
                                    break;
                                }
                            }
                        }
                    }
                } else { // Hotbar Slot is empty
                    // Move backpack slot to hotbar slot
                    int amountToMove = Math.min(backpackStack.getCount(), backpackStack.getItem().getMaxStackSize(backpackStack));

                    ItemStack backPackStackCopy = backpackStack.copyWithCount(amountToMove);
                    player.getInventory().setItem(hotbarSlot, backPackStackCopy);

                    // Update backpack
                    backpackStack.shrink(amountToMove);
                    container.setDataChanged();
                    inventory.selected = hotbarSlot;
                }

            } else {
                // If selected hotbar slot matches picked item then top up from backpack
                // Check if hotbar stack matches item stack

                ItemStack hotbarStack = inventory.getItem(hotbarSlot);
                int hotbarStackSize = hotbarStack.getCount();
                int maxHotBarStackSize = hotbarStack.getMaxStackSize();

                if (ItemStack.isSameItem(hotbarStack, pickedItem)) {

                    // If hotbar slot only partially full, top up from backpack
                    if (maxHotBarStackSize > hotbarStackSize) {
                        int freeHotBarStackSpace = maxHotBarStackSize - hotbarStackSize;

                        // Move partial stack from backpack to hotbar

                        // Has backpack got enough items?
                        int backPackStackSize = backpackStack.getCount();

                        int amountToMove;
                        ItemStack newInventoryStack;
                        if (backPackStackSize > freeHotBarStackSpace) {
                            // Take partial amount from backpack stack
                            amountToMove = hotbarStack.getMaxStackSize() - hotbarStackSize;
                            // Update hotbar stack with new amount
                            newInventoryStack = pickedItem.copyWithCount(hotbarStack.getMaxStackSize());
                        } else {
                            // Take entire amount from backpack stack
                            amountToMove = Math.min(backpackStack.getCount(), backpackStack.getItem().getMaxStackSize(backpackStack));
                            // Send backpack stack to hotbar
                            newInventoryStack = backpackStack.copyWithCount(amountToMove - hotbarStackSize);
                        }

                        player.getInventory().setItem(hotbarSlot, newInventoryStack);
                        // Update backpack
                        backpackStack.shrink(amountToMove);
                        container.setDataChanged();
                        inventory.selected = hotbarSlot;
                    }
                }
            }
        }
    }

}
