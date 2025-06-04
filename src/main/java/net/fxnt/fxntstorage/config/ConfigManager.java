package net.fxnt.fxntstorage.config;

import com.google.common.collect.ImmutableList;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigManager {
    public static final String FXNTSTORAGE_SETTINGS_TAG = "fxntstorageSettings";

    public static class CommonConfig {
        public static final ForgeConfigSpec COMMON_SPEC;
        public static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.ConfigValue<Integer> STORAGE_BOX_UPDATE_TIME;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_RANGE;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_UPDATE_TIME;
        public static ForgeConfigSpec.BooleanValue CURIOS_KEEP_BACKPACK;

        // BACKPACK UPGRADES
        public static ForgeConfigSpec.ConfigValue<Integer> BACKPACK_MAGNET_RANGE;
        public static ForgeConfigSpec.BooleanValue ELYTRA_BOOST_ENABLED;
        public static ForgeConfigSpec.IntValue ELYTRA_BOOST_MULTIPLIER;

        static {
            // STORAGE BOX
            COMMON_BUILDER.comment("Storage Box").push("storage_box");
            STORAGE_BOX_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks before Storage Boxes update their block count and block states. Higher value = better performance")
                    .define("storageBoxUpdateTime", 20);
            COMMON_BUILDER.pop();

            // STORAGE NETWORK
            COMMON_BUILDER.comment("Storage Network").push("storage_network");
            SIMPLE_STORAGE_NETWORK_RANGE = COMMON_BUILDER
                    .comment("The number of blocks from a controller that should be checked as part of a Storage Network")
                    .defineInRange("simpleStorageNetworkRange", 32, 8, 64);
            SIMPLE_STORAGE_NETWORK_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks Storage Controllers & Interfaces wait before updating their connection to the Storage Network")
                    .define("simpleStorageNetworkUpdateTime", 20);
            COMMON_BUILDER.pop();

            // BACKPACK
            COMMON_BUILDER.comment("Backpack").push("backpack");
            CURIOS_KEEP_BACKPACK = COMMON_BUILDER
                    .comment("Keep Backpacks equipped on death")
                    .define("keepBackpackOnDeath", true);
            BACKPACK_MAGNET_RANGE = COMMON_BUILDER
                    .comment("Backpack Magnet Range (in Blocks)")
                    .defineInRange("backpackMagnetRange", 5, 2, 16);
            ELYTRA_BOOST_ENABLED = COMMON_BUILDER
                    .comment("Enable Jetpack boosting while gliding with an Elytra equipped")
                    .define("elytraBoostEnabled", true);
            ELYTRA_BOOST_MULTIPLIER = COMMON_BUILDER
                    .comment("The Jetpack air consumption multiplier to use while Elytra boosting")
                    .defineInRange("elytraBoostMultiplier", 4, 1, 10);
            COMMON_BUILDER.pop();

            COMMON_SPEC = COMMON_BUILDER.build();
        }

    }

    public static class ClientConfig {
        public static final ForgeConfigSpec CLIENT_SPEC;
        public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.BooleanValue DISPLAY_JETPACK_AIR_OVERLAY;
        public static ForgeConfigSpec.BooleanValue DISPLAY_FEEDER_MESSAGE;
        public static ForgeConfigSpec.BooleanValue MAGNET_IGNORE_FAN_PROCESSING;
        public static ForgeConfigSpec.BooleanValue TOOLSWAP_PREFER_SILK_TOUCH;
        public static ForgeConfigSpec.ConfigValue<List<? extends String>> TOOLSWAP_PREFERS_SILK_TOUCH_LIST;

        static {
            CLIENT_BUILDER.comment("Feeder Upgrade").push("feeder_upgrade");
            DISPLAY_FEEDER_MESSAGE = CLIENT_BUILDER
                    .comment("Display a message when the feeder upgrade feeds food to the player")
                    .define("displayFeederMessage", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Jetpack Upgrade").push("jetpack_upgrade");
            DISPLAY_JETPACK_AIR_OVERLAY = CLIENT_BUILDER
                    .comment("Display remaining amount of Jetpack air on HUD")
                    .define("displayJetpackAirOverlay", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            MAGNET_IGNORE_FAN_PROCESSING = CLIENT_BUILDER
                    .comment("Ignore items being processed by a create fan")
                    .define("ignoreFanProcessing", true);
            CLIENT_BUILDER.pop();

            // BACKPACK > BACKPACK UPGRADES > TOOL SWAP UPGRADE
            CLIENT_BUILDER.comment("Tool Swap Upgrade").push("tool_swap_upgrade");
            TOOLSWAP_PREFER_SILK_TOUCH = CLIENT_BUILDER
                    .comment("Prefer tools with the Silk Touch enchantment when used on blocks from the Silk Touch list")
                    .define("preferSilkTouch", false);
            TOOLSWAP_PREFERS_SILK_TOUCH_LIST = CLIENT_BUILDER
                    .comment("List of blocks that prefer the Silk Touch enchantment in order to drop the block instead of loot (i.e. grass_block instead of dirt)")
                    .defineListAllowEmpty("prefersSilkTouchList", ImmutableList.of("minecraft:grass_block", "minecraft:mycelium", "minecraft:podzol", "minecraft:clay", "minecraft:gravel", "minecraft:snow",
                                    "minecraft:glowstone", "minecraft:stone", "minecraft:sea_lantern", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                                    "minecraft:gilded_blackstone", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                                    "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
                            o -> true);
            CLIENT_BUILDER.pop();

            CLIENT_SPEC = CLIENT_BUILDER.build();
        }

    }

}
