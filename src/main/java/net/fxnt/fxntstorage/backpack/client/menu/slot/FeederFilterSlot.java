package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;

import java.util.function.BooleanSupplier;

public class FeederFilterSlot extends FilterSlot {

    public FeederFilterSlot(IBackpackContainer backpack, int index, int xPosition, int yPosition, BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded) {
        super(backpack, index, xPosition, yPosition, hasUpgrade, isPanelExpanded);
    }
}
