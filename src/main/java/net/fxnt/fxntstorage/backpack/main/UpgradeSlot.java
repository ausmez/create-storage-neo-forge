package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class UpgradeSlot extends SlotItemHandler {
    public UpgradeSlot(IItemHandler container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    public static int getMaxStackSizeStatic() {
        return 1;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        // Only allow upgrades through
        return stack.getItem() instanceof UpgradeItem;
    }

}
