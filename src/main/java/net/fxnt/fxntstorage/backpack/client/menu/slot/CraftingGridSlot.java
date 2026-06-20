package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class CraftingGridSlot extends Slot {
    private final BooleanSupplier hasUpgrade;
    private final BooleanSupplier isPanelExpanded;

    public CraftingGridSlot(Container container, int index, int xPosition, int yPosition,
                            BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded) {
        super(container, index, xPosition, yPosition);
        this.hasUpgrade = hasUpgrade;
        this.isPanelExpanded = isPanelExpanded;
    }

    @Override
    public boolean isActive() {
        return hasUpgrade.getAsBoolean() && isPanelExpanded.getAsBoolean();
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return isActive();
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return isActive();
    }
}
