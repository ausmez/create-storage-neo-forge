package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.main.BackpackItem;
import net.fxnt.fxntstorage.containers.StorageBoxItem;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FXNTStorage.MOD_ID);

    // Backpack upgrade items
    public static final RegistryObject<Item> BACK_PACK_BLANK_UPGRADE = ITEMS.register("back_pack_blank_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(16), Util.BLANK_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_MAGNET_UPGRADE = ITEMS.register("back_pack_magnet_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.MAGNET_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_MAGNET_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_magnet_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.MAGNET_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_PICKBLOCK_UPGRADE = ITEMS.register("back_pack_pickblock_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.PICKBLOCK_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_PICKBLOCK_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_pickblock_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.PICKBLOCK_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_ITEMPICKUP_UPGRADE = ITEMS.register("back_pack_itempickup_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.ITEMPICKUP_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_ITEMPICKUP_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_itempickup_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.ITEMPICKUP_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_FLIGHT_UPGRADE = ITEMS.register("back_pack_flight_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FLIGHT_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_FLIGHT_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_flight_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FLIGHT_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_REFILL_UPGRADE = ITEMS.register("back_pack_refill_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.REFILL_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_REFILL_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_refill_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.REFILL_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_FEEDER_UPGRADE = ITEMS.register("back_pack_feeder_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FEEDER_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_FEEDER_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_feeder_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FEEDER_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_TOOLSWAP_UPGRADE = ITEMS.register("back_pack_toolswap_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.TOOLSWAP_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_TOOLSWAP_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_toolswap_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.TOOLSWAP_UPGRADE_DEACTIVATED));
    public static final RegistryObject<Item> BACK_PACK_FALLDAMAGE_UPGRADE = ITEMS.register("back_pack_falldamage_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FALLDAMAGE_UPGRADE));
    public static final RegistryObject<Item> BACK_PACK_FALLDAMAGE_UPGRADE_DEACTIVATED = ITEMS.register("back_pack_falldamage_upgrade_deactivated", () -> new UpgradeItem(new Item.Properties().stacksTo(1), Util.FALLDAMAGE_UPGRADE_DEACTIVATED));

    // Simple storage box upgrade items
    public static final RegistryObject<Item> STORAGE_BOX_CAPACITY_UPGRADE = ITEMS.register("storage_box_capacity_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(16), Util.STORAGE_BOX_CAPACITY_UPGRADE));
    public static final RegistryObject<Item> STORAGE_BOX_VOID_UPGRADE = ITEMS.register("storage_box_void_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(16), Util.STORAGE_BOX_VOID_UPGRADE));

    // Backpacks (as Items)
    public static final RegistryObject<Item> BACK_PACK = ITEMS.register("back_pack", () -> new BackpackItem(ModBlocks.BACK_PACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANDESITE_BACK_PACK = ITEMS.register("andesite_back_pack", () -> new BackpackItem(ModBlocks.ANDESITE_BACK_PACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> COPPER_BACK_PACK = ITEMS.register("copper_back_pack", () -> new BackpackItem(ModBlocks.COPPER_BACK_PACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> BRASS_BACK_PACK = ITEMS.register("brass_back_pack", () -> new BackpackItem(ModBlocks.BRASS_BACK_PACK.get(), new Item.Properties()));
    public static final RegistryObject<Item> HARDENED_BACK_PACK = ITEMS.register("hardened_back_pack", () -> new BackpackItem(ModBlocks.HARDENED_BACK_PACK.get(), new Item.Properties().fireResistant()));

    // Storage Boxes (as Items)
    public static final RegistryObject<Item> STORAGE_BOX = ITEMS.register("storage_box", () -> new StorageBoxItem(ModBlocks.STORAGE_BOX.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> ANDESITE_STORAGE_BOX = ITEMS.register("andesite_storage_box", () -> new StorageBoxItem(ModBlocks.ANDESITE_STORAGE_BOX.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> COPPER_STORAGE_BOX = ITEMS.register("copper_storage_box", () -> new StorageBoxItem(ModBlocks.COPPER_STORAGE_BOX.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> BRASS_STORAGE_BOX = ITEMS.register("brass_storage_box", () -> new StorageBoxItem(ModBlocks.BRASS_STORAGE_BOX.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> HARDENED_STORAGE_BOX = ITEMS.register("hardened_storage_box", () -> new StorageBoxItem(ModBlocks.HARDENED_STORAGE_BOX.get(), new Item.Properties().fireResistant()));

    // Simple Storage Boxes (as Items)
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX = ITEMS.register("simple_storage_box", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_SPRUCE = ITEMS.register("simple_storage_box_spruce", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_BIRCH = ITEMS.register("simple_storage_box_birch", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_JUNGLE = ITEMS.register("simple_storage_box_jungle", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_ACACIA = ITEMS.register("simple_storage_box_acacia", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_ACACIA.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_DARK_OAK = ITEMS.register("simple_storage_box_dark_oak", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_MANGROVE = ITEMS.register("simple_storage_box_mangrove", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_CHERRY = ITEMS.register("simple_storage_box_cherry", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_CHERRY.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_BAMBOO = ITEMS.register("simple_storage_box_bamboo", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_CRIMSON = ITEMS.register("simple_storage_box_crimson", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON.get(), new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SIMPLE_STORAGE_BOX_WARPED = ITEMS.register("simple_storage_box_warped", () -> new SimpleStorageBoxItem(ModBlocks.SIMPLE_STORAGE_BOX_WARPED.get(), new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
