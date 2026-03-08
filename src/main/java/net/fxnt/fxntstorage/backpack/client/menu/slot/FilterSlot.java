package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class FilterSlot extends SlotItemHandler {
    private final BooleanSupplier hasUpgrade;
    private final BooleanSupplier isPanelExpanded;

    public FilterSlot(IBackpackContainer backpack, int index, int xPosition, int yPosition, BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded) {
        super(backpack.getItemHandler(), index, xPosition, yPosition);
        this.hasUpgrade = hasUpgrade;
        this.isPanelExpanded = isPanelExpanded;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return true;
    }

    @Override
    public boolean isActive() {
        return hasUpgrade.getAsBoolean() && isPanelExpanded.getAsBoolean();
    }
}
