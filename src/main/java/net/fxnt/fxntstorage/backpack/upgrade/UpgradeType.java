package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public enum UpgradeType {
    // Upgrades with slots (must be in BackpackLayout order)
    JUKEBOX("jukebox", ModItems.BACKPACK_JUKEBOX_UPGRADE::get, ModItems.BACKPACK_JUKEBOX_UPGRADE_DEACTIVATED::get, true, 1),
    MAGNET("magnet", ModItems.BACKPACK_MAGNET_UPGRADE::get, ModItems.BACKPACK_MAGNET_UPGRADE_DEACTIVATED::get, true, 2),
    FEEDER("feeder", ModItems.BACKPACK_FEEDER_UPGRADE::get, ModItems.BACKPACK_FEEDER_UPGRADE_DEACTIVATED::get, true, 4),

    // Upgrades with panels but no slots
    FLIGHT("flight", ModItems.BACKPACK_FLIGHT_UPGRADE::get, ModItems.BACKPACK_FLIGHT_UPGRADE_DEACTIVATED::get, true, 8),
    OREMINING("oremining", ModItems.BACKPACK_OREMINING_UPGRADE::get, ModItems.BACKPACK_OREMINING_UPGRADE_DEACTIVATED::get, true, 16),
    TOOLSWAP("toolswap", ModItems.BACKPACK_TOOLSWAP_UPGRADE::get, ModItems.BACKPACK_TOOLSWAP_UPGRADE_DEACTIVATED::get, true, 32),

    // Upgrade without panels or slots
    FALLDAMAGE("falldamage", ModItems.BACKPACK_FALLDAMAGE_UPGRADE::get, ModItems.BACKPACK_FALLDAMAGE_UPGRADE_DEACTIVATED::get, false, 0),
    HEALTH("health", ModItems.BACKPACK_HEALTH_UPGRADE::get, ModItems.BACKPACK_HEALTH_UPGRADE_DEACTIVATED::get, false, 0),
    ITEMPICKUP("itempickup", ModItems.BACKPACK_ITEMPICKUP_UPGRADE::get, ModItems.BACKPACK_ITEMPICKUP_UPGRADE_DEACTIVATED::get, false, 0),
    PICKBLOCK("pickblock", ModItems.BACKPACK_PICKBLOCK_UPGRADE::get, ModItems.BACKPACK_PICKBLOCK_UPGRADE_DEACTIVATED::get, false, 0),
    REFILL("refill", ModItems.BACKPACK_REFILL_UPGRADE::get, ModItems.BACKPACK_REFILL_UPGRADE_DEACTIVATED::get, false, 0),
    TORCHDEPLOYER("torchdeployer", ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE::get, ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE_DEACTIVATED::get, false, 0);

    private final String id;
    private final Supplier<Item> activeItem;
    private final Supplier<Item> deactivatedItem;
    private final boolean hasPanel;
    private final int panelBit;

    UpgradeType(String id, Supplier<Item> activeItem, Supplier<Item> deactivatedItem, boolean hasPanel, int panelBit) {
        this.id = id;
        this.activeItem = activeItem;
        this.deactivatedItem = deactivatedItem;
        this.hasPanel = hasPanel;
        this.panelBit = panelBit;
    }

    public String getId() {
        return id;
    }

    public Item getActiveItem() {
        return activeItem.get();
    }

    public Item getDeactivatedItem() {
        return deactivatedItem.get();
    }

    public ItemStack getActiveStack() {
        return new ItemStack(getActiveItem());
    }

    public ItemStack getDeactivatedStack() {
        return new ItemStack(getDeactivatedItem());
    }

    public boolean hasPanel() {
        return hasPanel;
    }

    public boolean isPanelExpandedInMask(int mask) {
        return panelBit != 0 && (mask & panelBit) != 0;
    }

    public int toggleInMask(int mask) {
        return panelBit != 0 ? (mask ^ panelBit) : mask;
    }

    public int clearInMask(int mask) {
        return panelBit != 0 ? (mask & ~panelBit) : mask;
    }

    public static UpgradeType fromBaseName(String baseName) {
        for (UpgradeType type : values()) {
            if (type.id.equals(baseName)) {
                return type;
            }
        }
        return null;
    }

    public static UpgradeType fromItem(Item item) {
        for (UpgradeType type : values()) {
            if (type.getActiveItem() == item || type.getDeactivatedItem() == item) {
                return type;
            }
        }
        return null;
    }

    public boolean isThisUpgrade(Item item) {
        return getActiveItem() == item || getDeactivatedItem() == item;
    }

    public boolean isInStack(ItemStack stack) {
        return isThisUpgrade(stack.getItem());
    }
}
