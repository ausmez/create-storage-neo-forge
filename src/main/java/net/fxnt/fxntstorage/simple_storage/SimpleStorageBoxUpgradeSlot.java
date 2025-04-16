package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.init.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SimpleStorageBoxUpgradeSlot extends SlotItemHandler {

    public SimpleStorageBoxUpgradeSlot(IItemHandler itemHandler, int slot, int x, int y) {
        super(itemHandler, slot, x, y);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        if (this.hasItem()) return false;
        return stack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }
}
