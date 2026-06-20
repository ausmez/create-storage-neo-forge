package net.fxnt.fxntstorage.reserve_storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.STORAGE_SLOTS;

public class ReserveStorageBoxGhostSlot extends SlotItemHandler {
    static final int MAX_COUNT = STORAGE_SLOTS * 64;

    public ReserveStorageBoxGhostSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    public ReserveStorageBoxGhostSlot(Container container, int index, int xPosition, int yPosition) {
        super(new InvWrapper(container), index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public boolean isHighlightable() {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return MAX_COUNT;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return MAX_COUNT;
    }
}
