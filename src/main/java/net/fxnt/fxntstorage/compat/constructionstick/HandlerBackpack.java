package net.fxnt.fxntstorage.compat.constructionstick;

import mrbysco.constructionstick.api.IContainerHandler;
import mrbysco.constructionstick.basics.StickUtil;
import mrbysco.constructionstick.containers.ContainerTrace;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class HandlerBackpack implements IContainerHandler {
    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getCount() == 1 && Block.byItem(inventoryStack.getItem()) instanceof BackpackBlock;
    }

    @Override
    public int getSignature(Player player, ItemStack itemStack) {
        return itemStack.hashCode();
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        BackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, inventoryStack);
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        int count = 0;

        for (int i : layout.items().range()) {
            ItemStack stack = container.getStackInSlot(i);
            if (StickUtil.stackEquals(stack, itemStack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        BackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, inventoryStack);
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        boolean changed = false;

        for (int i : layout.items().range()) {
            ItemStack stack = container.getStackInSlot(i);
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
