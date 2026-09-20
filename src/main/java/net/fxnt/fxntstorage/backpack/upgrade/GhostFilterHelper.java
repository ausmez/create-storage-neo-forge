package net.fxnt.fxntstorage.backpack.upgrade;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class GhostFilterHelper {

    private static final Predicate<ItemStack> IS_CREATE_FILTER = stack -> stack.getItem() instanceof FilterItem;
    private static final Predicate<ItemStack> IS_ATTRIBUTE_LIST_FILTER =
            stack -> stack.getItem() instanceof FilterItem && !(stack.getItem() instanceof PackageFilterItem);

    private GhostFilterHelper() {
    }

    @Nullable
    public static IUpgrade upgradeForSlot(BackpackSlotLayout layout, int slotIndex) {
        if (slotIndex < 0) return null;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (upgrade.getFilterSlotIndex(layout) == slotIndex) return upgrade;
        }
        return null;
    }

    public static boolean isRealFilterItem(ItemStack stack) {
        return !stack.isEmpty() && IS_CREATE_FILTER.test(stack);
    }

    public static void returnFilterItem(UpgradeContext context, int filterSlot) {
        if (context.isClientSide()) return;
        if (!(context.menu() instanceof BackpackMenu menu)) return;

        IItemHandlerModifiable itemHandler = context.itemHandler();
        ItemStack filterStack = itemHandler.getStackInSlot(filterSlot);
        if (!isRealFilterItem(filterStack)) return;

        ItemStack toReturn = filterStack.copy();
        itemHandler.setStackInSlot(filterSlot, ItemStack.EMPTY);

        Player player = context.player();
        if (player != null) {
            menu.moveStackToPlayerInventory(toReturn);
            if (!toReturn.isEmpty()) player.drop(toReturn, false);
        }

        // Removal contexts are built without a container, so go through the menu for the save
        menu.container.setDataChanged();
    }

    public static boolean accepts(BackpackMenu menu, int slotIndex, ItemStack stack) {
        IUpgrade upgrade = upgradeForSlot(BackpackSlotLayout.createLayout(), slotIndex);
        if (upgrade == null) return false;

        UpgradeContext context = UpgradeContext.forMenuWithSlot(
                menu, menu.player, menu.container, menu.getBackpackType(), null,
                slotIndex, 0, ClickType.PICKUP);
        return upgrade.filterAccepts(context, stack);
    }

    public static boolean handleClick(UpgradeContext context, Predicate<ItemStack> accepts) {
        if (context.clickType() != ClickType.PICKUP) return true;

        Slot slot = context.player().containerMenu.slots.get(context.slotId());
        ItemStack carried = context.player().containerMenu.getCarried();
        ItemStack existing = slot.getItem();

        boolean carriedIsFilter = !carried.isEmpty() && IS_CREATE_FILTER.test(carried);
        boolean existingIsFilter = !existing.isEmpty() && IS_CREATE_FILTER.test(existing);

        // RIGHT CLICK: clear slot
        if (context.button() == 1 && !existingIsFilter) {
            slot.set(ItemStack.EMPTY);
            context.container().setDataChanged();
            return true;
        }

        // PICK UP existing filter
        if (existingIsFilter && carried.isEmpty()) {
            context.player().containerMenu.setCarried(existing);
            slot.set(ItemStack.EMPTY);
            context.container().setDataChanged();
            return true;
        }

        if (existingIsFilter && carriedIsFilter) {
            if (carried.getCount() > 1) {
                context.player().drop(existing, true);
                slot.set(carried.copyWithCount(1));
                carried.shrink(1);
            } else {
                context.player().containerMenu.setCarried(existing);
                slot.set(carried);
            }
            context.container().setDataChanged();
            return true;
        }

        if (!accepts.test(carried) && !IS_ATTRIBUTE_LIST_FILTER.test(carried)) return true;
        if (existingIsFilter || carried.isEmpty()) return true;

        ItemStack ghost;
        if (carriedIsFilter) {
            if (carried.getCount() == 1) {
                ghost = carried;
                context.player().containerMenu.setCarried(ItemStack.EMPTY);
            } else {
                ghost = carried.copyWithCount(1);
                carried.shrink(1);
                context.player().containerMenu.setCarried(carried);
            }
        } else {
            if (carried.has(DataComponents.POTION_CONTENTS)) {
                ghost = carried.copyWithCount(1);
            } else {
                ghost = new ItemStack(carried.getItem(), 1);
            }
        }

        slot.set(ghost);
        context.container().setDataChanged();
        return true;
    }
}
