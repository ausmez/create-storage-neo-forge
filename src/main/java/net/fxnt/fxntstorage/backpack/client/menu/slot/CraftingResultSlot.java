package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.upgrade.crafting.BackpackCraftingContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class CraftingResultSlot extends ResultSlot {
    private final CraftingContainer craftGrid;
    private final BooleanSupplier hasUpgrade;
    private final BooleanSupplier isPanelExpanded;

    public CraftingResultSlot(Player player, CraftingContainer craftSlots, Container resultContainer,
                              int index, int xPosition, int yPosition,
                              BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded) {
        super(player, craftSlots, resultContainer, index, xPosition, yPosition);
        this.craftGrid = craftSlots;
        this.hasUpgrade = hasUpgrade;
        this.isPanelExpanded = isPanelExpanded;
    }

    @Override
    public boolean isActive() {
        return hasUpgrade.getAsBoolean() && isPanelExpanded.getAsBoolean();
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return isActive() && super.mayPickup(player);
    }

    public void accountQuickCraft(@NotNull ItemStack crafted, int amount) {
        this.onQuickCraft(crafted, amount);
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        if (craftGrid instanceof BackpackCraftingContainer grid) {
            grid.setSuppressNotify(true);
            try {
                super.onTake(player, stack);
            } finally {
                grid.setSuppressNotify(false);
            }
            grid.setChanged();
        } else {
            super.onTake(player, stack);
        }
    }
}
