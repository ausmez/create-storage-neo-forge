package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class UpgradeHelper {
    private static final BackpackSlotLayout LAYOUT = BackpackSlotLayout.createLayout();

    private UpgradeHelper() {
    }

    public static ItemStack toggleUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) {
            return stack;
        }

        String upgradeName = upgradeItem.getUpgradeName();
        String baseUpgradeName = upgradeItem.getBaseUpgradeName();

        UpgradeType type = UpgradeType.fromBaseName(baseUpgradeName);
        if (type == null) {
            return stack;
        }

        return upgradeName.contains("_deactivated")
                ? type.getActiveStack()
                : type.getDeactivatedStack();
    }

    public static boolean hasUpgrade(IItemHandler handler, UpgradeType type) {
        for (int i : LAYOUT.upgrades().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (type.isInStack(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveUpgrade(IItemHandler handler, UpgradeType type) {
        for (int i : LAYOUT.upgrades().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() == type.getActiveItem()) {
                return true;
            }
        }
        return false;
    }

    public static List<UpgradeType> getInstalledUpgrades(IItemHandler handler) {
        List<UpgradeType> upgrades = new ArrayList<>();

        for (int i : LAYOUT.upgrades().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof UpgradeItem item) {
                UpgradeType type = UpgradeType.fromBaseName(item.getBaseUpgradeName());
                if (type != null && !upgrades.contains(type)) {
                    upgrades.add(type);
                }
            }
        }

        return upgrades;
    }

    public static List<String> refreshUpgradeList(IItemHandler handler) {
        List<String> upgradeNames = new ArrayList<>();

        for (int i : LAYOUT.upgrades().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof UpgradeItem upgradeItem) {
                String name = upgradeItem.getUpgradeName();
                if (!upgradeNames.contains(name)) {
                    upgradeNames.add(name);
                }
            }
        }

        return upgradeNames;
    }

    public static ItemStack ensureActivated(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) {
            return stack;
        }

        String upgradeName = upgradeItem.getUpgradeName();
        if (!upgradeName.contains("_deactivated")) {
            return stack; // Already active
        }

        String baseUpgradeName = upgradeItem.getBaseUpgradeName();
        UpgradeType type = UpgradeType.fromBaseName(baseUpgradeName);
        return type != null ? type.getActiveStack() : stack;
    }
}
