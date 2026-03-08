package net.fxnt.fxntstorage.compat.constructionstick;

import mrbysco.constructionstick.api.IContainerHandler;
import mrbysco.constructionstick.basics.StickUtil;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

public class HandlerStorageBox implements IContainerHandler {
    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getCount() == 1
                && (Block.byItem(inventoryStack.getItem()) instanceof StorageBox
                    || Block.byItem(inventoryStack.getItem()) instanceof SimpleStorageBox);
    }

    @Override
    public int countItems(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        int count = 0;

        for (ItemStack stack : getItemList(inventoryStack)) {
            if (StickUtil.stackEquals(stack, itemStack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    @Override
    public int useItems(Player player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        NonNullList<ItemStack> itemList = getItemList(inventoryStack);
        boolean changed = false;

        for (ItemStack stack : itemList) {
            if (StickUtil.stackEquals(stack, itemStack)) {
                int toTake = Math.min(count, stack.getCount());
                stack.shrink(toTake);
                count -= toTake;
                changed = true;
                if (count == 0) {
                    break;
                }
            }
        }

        if (changed) {
            this.setItemList(inventoryStack, itemList);
            player.getInventory().setChanged();
        }

        return count;
    }

    private NonNullList<ItemStack> getItemList(ItemStack itemStack) {
        ItemContainerContents contents = itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> itemStacks = NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);

        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack stack = contents.getStackInSlot(i);
            if (!stack.isEmpty()) {
                itemStacks.set(i, stack);
            }
        }
        return itemStacks;
    }

    private void setItemList(ItemStack itemStack, NonNullList<ItemStack> itemStacks) {
        itemStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemStacks));
    }
}
