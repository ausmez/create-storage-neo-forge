package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class WorkshopSlot extends SlotItemHandler {
    private final IBackpackContainer backpack;
    private final BooleanSupplier hasUpgrade;
    private final BooleanSupplier isPanelExpanded;
    private final Predicate<ItemStack> placePredicate;

    public WorkshopSlot(IBackpackContainer backpack, int index, int xPosition, int yPosition,
                        BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded,
                        Predicate<ItemStack> placePredicate) {
        super(backpack.getItemHandler(), index, xPosition, yPosition);
        this.backpack = backpack;
        this.hasUpgrade = hasUpgrade;
        this.isPanelExpanded = isPanelExpanded;
        this.placePredicate = placePredicate;
    }

    @Override
    public boolean isActive() {
        return hasUpgrade.getAsBoolean() && isPanelExpanded.getAsBoolean();
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return isActive() && placePredicate != null && placePredicate.test(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return isActive();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        backpack.setDataChanged();
    }
}
