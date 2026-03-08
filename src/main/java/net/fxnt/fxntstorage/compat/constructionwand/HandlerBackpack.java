package net.fxnt.fxntstorage.compat.constructionwand;

import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thetadev.constructionwand.api.IContainerHandler;
import thetadev.constructionwand.basics.WandUtil;
import net.minecraftforge.items.IItemHandlerModifiable;

public class HandlerBackpack implements IContainerHandler {
    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getCount() == 1 && Block.byItem(inventoryStack.getItem()) instanceof BackpackBlock;
    }

    @Override
    public int countItems(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        BackpackContainer container = new BackpackContainer(player, inventoryStack);
        int count = 0;

        for (ItemStack stack : container.getItems()) {
            if (WandUtil.stackEquals(stack, itemStack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    @Override
    public int useItems(Player player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        BackpackContainer container = new BackpackContainer(player, inventoryStack);
        boolean changed = false;

        for (ItemStack stack : container.getItems()) {
            if (WandUtil.stackEquals(stack, itemStack)) {
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
            setItems(container);
            container.setDataChanged();
        }

        return count;
    }

    private void setItems(BackpackContainer container) {
        IItemHandlerModifiable handler = container.getItemHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, container.getStackInSlot(i));
        }
    }
}
