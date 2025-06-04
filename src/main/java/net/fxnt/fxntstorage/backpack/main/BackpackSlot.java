package net.fxnt.fxntstorage.backpack.main;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class BackpackSlot extends SlotItemHandler {
    public BackpackSlot(IItemHandler container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
}
