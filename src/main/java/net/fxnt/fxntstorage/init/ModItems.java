package net.fxnt.fxntstorage.init;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.util.Util;

public class ModItems {
    private static final CreateRegistrate REGISTRATE = FXNTStorage.REGISTRATE;

    static {
        REGISTRATE.setCreativeTab(ModTabs.CREATIVE_MODE_TAB);
    }

    // Simple storage box upgrade items
    public static final ItemEntry<UpgradeItem> STORAGE_BOX_CAPACITY_UPGRADE = REGISTRATE
            .item("storage_box_capacity_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_CAPACITY_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();

    public static final ItemEntry<UpgradeItem> STORAGE_BOX_VOID_UPGRADE = REGISTRATE
            .item("storage_box_void_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_VOID_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();


    // Backpack upgrade items
    public static final ItemEntry<UpgradeItem> BACKPACK_BLANK_UPGRADE = REGISTRATE
            .item("backpack_blank_upgrade", properties -> new UpgradeItem(properties, Util.BLANK_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_MAGNET_UPGRADE = REGISTRATE
            .item("backpack_magnet_upgrade", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_MAGNET_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_magnet_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_PICKBLOCK_UPGRADE = REGISTRATE
            .item("backpack_pickblock_upgrade", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_PICKBLOCK_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_pickblock_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_ITEMPICKUP_UPGRADE = REGISTRATE
            .item("backpack_itempickup_upgrade", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_ITEMPICKUP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_itempickup_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FLIGHT_UPGRADE = REGISTRATE
            .item("backpack_flight_upgrade", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FLIGHT_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_flight_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_REFILL_UPGRADE = REGISTRATE
            .item("backpack_refill_upgrade", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_REFILL_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_refill_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FEEDER_UPGRADE = REGISTRATE
            .item("backpack_feeder_upgrade", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FEEDER_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_feeder_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TOOLSWAP_UPGRADE = REGISTRATE
            .item("backpack_toolswap_upgrade", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TOOLSWAP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_toolswap_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FALLDAMAGE_UPGRADE = REGISTRATE
            .item("backpack_falldamage_upgrade", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FALLDAMAGE_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_falldamage_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_OREMINING_UPGRADE = REGISTRATE
            .item("backpack_oremining_upgrade", properties -> new UpgradeItem(properties, Util.OREMINING_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_OREMINING_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_oremining_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.OREMINING_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TORCHDEPLOYER_UPGRADE = REGISTRATE
            .item("backpack_torchdeployer_upgrade", properties -> new UpgradeItem(properties, Util.TORCHDEPLOYER_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TORCHDEPLOYER_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_torchdeployer_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.TORCHDEPLOYER_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static void register() {
    }
}
