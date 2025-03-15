package net.fxnt.fxntstorage.init;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.util.Util;

import static net.fxnt.fxntstorage.FXNTStorage.REGISTRATE;

public class ModItems {

    // Backpack upgrade items
    public static final ItemEntry<UpgradeItem> BACK_PACK_BLANK_UPGRADE = REGISTRATE
            .item("back_pack_blank_upgrade", properties -> new UpgradeItem(properties, Util.BLANK_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_MAGNET_UPGRADE = REGISTRATE
            .item("back_pack_magnet_upgrade", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_MAGNET_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_magnet_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_PICKBLOCK_UPGRADE = REGISTRATE
            .item("back_pack_pickblock_upgrade", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_PICKBLOCK_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_pickblock_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_ITEMPICKUP_UPGRADE = REGISTRATE
            .item("back_pack_itempickup_upgrade", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_ITEMPICKUP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_itempickup_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FLIGHT_UPGRADE = REGISTRATE
            .item("back_pack_flight_upgrade", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FLIGHT_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_flight_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_REFILL_UPGRADE = REGISTRATE
            .item("back_pack_refill_upgrade", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_REFILL_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_refill_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FEEDER_UPGRADE = REGISTRATE
            .item("back_pack_feeder_upgrade", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FEEDER_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_feeder_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_TOOLSWAP_UPGRADE = REGISTRATE
            .item("back_pack_toolswap_upgrade", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_TOOLSWAP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_toolswap_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FALLDAMAGE_UPGRADE = REGISTRATE
            .item("back_pack_falldamage_upgrade", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .register();

    public static final ItemEntry<UpgradeItem> BACK_PACK_FALLDAMAGE_UPGRADE_DEACTIVATED = REGISTRATE
            .item("back_pack_falldamage_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .register();


    // Simple storage box upgrade items
    public static final ItemEntry<UpgradeItem> STORAGE_BOX_CAPACITY_UPGRADE = REGISTRATE
            .item("storage_box_capacity_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_CAPACITY_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();

    public static final ItemEntry<UpgradeItem> STORAGE_BOX_VOID_UPGRADE = REGISTRATE
            .item("storage_box_void_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_VOID_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .register();

    public static void register() {
    }
}
