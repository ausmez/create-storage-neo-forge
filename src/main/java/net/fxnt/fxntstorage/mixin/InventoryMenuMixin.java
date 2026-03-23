package net.fxnt.fxntstorage.mixin;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class)
public abstract class InventoryMenuMixin {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    protected abstract boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseOrder);

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void fxnt$interceptBackpackShiftClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType != ClickType.QUICK_MOVE) return;
        if (slotId < 0 || slotId >= this.slots.size()) return;

        Slot slot = this.slots.get(slotId);
        if (!slot.hasItem()) return;
        if (!(slot.getItem().getItem() instanceof BackpackItem)) return;
        if (!BackpackHelper.isWearingBackpack(player)) return;
        // Only intercept from player inventory or hotbar — not armor/crafting/curio slots
        if (!(slot.container instanceof Inventory)) return;

        ItemStack item = slot.getItem();
        ItemStack original = item.copy();

        boolean fromHotbar = slot.getContainerSlot() < 9;

        // Dynamically find the menu slot range for the target area so we don't
        // need to hardcode indices (which vary when Curios adds extra slots).
        int targetStart = Integer.MAX_VALUE;
        int targetEnd = -1;
        for (int i = 0; i < this.slots.size(); i++) {
            Slot s = this.slots.get(i);
            if (!(s.container instanceof Inventory)) continue;
            boolean sIsHotbar = s.getContainerSlot() < 9;
            if (fromHotbar != sIsHotbar) {
                if (i < targetStart) targetStart = i;
                if (i + 1 > targetEnd) targetEnd = i + 1;
            }
        }

        ci.cancel();

        if (targetStart == Integer.MAX_VALUE) return;

        moveItemStackTo(item, targetStart, targetEnd, false);

        if (item.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (item.getCount() != original.getCount()) {
            slot.onTake(player, item);
        }
    }
}
