package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class BackpackSlot extends SlotItemHandler {
    private final IBackpackContainer backpack;

    public BackpackSlot(IBackpackContainer backpack, int slot, int x, int y) {
        super(backpack.getItemHandler(), slot, x, y);
        this.backpack = backpack;
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        if ((pStack.getItem() instanceof BackpackItem)) return false;
        return super.mayPlace(pStack);
    }

    @Override
    public int getMaxStackSize() {
        return backpack.getStackMultiplier() * 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.max(backpack.getStackMultiplier() * stack.getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        backpack.setDataChanged();
    }
}
