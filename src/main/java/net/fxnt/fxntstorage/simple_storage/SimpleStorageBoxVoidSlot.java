package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.init.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SimpleStorageBoxVoidSlot extends SlotItemHandler {

    public SimpleStorageBoxVoidSlot(IItemHandler itemHandler, int slot, int x, int y) {
        super(itemHandler, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (this.hasItem()) return false;
        return stack.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
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