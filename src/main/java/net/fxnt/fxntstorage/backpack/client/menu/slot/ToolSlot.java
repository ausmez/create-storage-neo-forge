package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ToolSlot extends SlotItemHandler {
    private final IBackpackContainer backpack;

    public ToolSlot(IBackpackContainer backpack, int slot, int x, int y) {
        super(backpack.getItemHandler(), slot, x, y);
        this.backpack = backpack;
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        if ((pStack.getItem() instanceof BackpackItem)) return false;
        return super.mayPlace(pStack);
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return Math.min(super.getMaxStackSize(stack), stack.getMaxStackSize());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        backpack.setDataChanged();
    }
}
