package net.fxnt.fxntstorage.config;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SyncClientSettingsPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ConfigManager {
    public static final String FXNTSTORAGE_SETTINGS_TAG = "fxntstorageSettings";

    public static class CommonConfig {
        public static final ForgeConfigSpec COMMON_SPEC;
        public static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.ConfigValue<Integer> STORAGE_BOX_UPDATE_TIME;
        public static ForgeConfigSpec.BooleanValue SIMPLE_STORAGE_NETWORK_FILL_EMPTY;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_RANGE;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_UPDATE_TIME;
        public static ForgeConfigSpec.BooleanValue CURIOS_KEEP_BACKPACK;

        // BACKPACK UPGRADES
        public static ForgeConfigSpec.ConfigValue<Integer> BACKPACK_MAGNET_RANGE;
        public static ForgeConfigSpec.BooleanValue ELYTRA_BOOST_ENABLED;
        public static ForgeConfigSpec.IntValue ELYTRA_BOOST_MULTIPLIER;
        public static ForgeConfigSpec.DoubleValue ELYTRA_BOOST_SPEED_MULTIPLIER;
        public static ForgeConfigSpec.BooleanValue OREMINE_ORES_ONLY;
        public static ForgeConfigSpec.BooleanValue JETPACK_MINING_PENALTY;
        public static ForgeConfigSpec.BooleanValue JETPACK_ALLOW_VOID_FLIGHT;
        public static ForgeConfigSpec.ConfigValue<List<? extends String>> REFILL_BLACKLIST;

        static {
            // BACKPACK
            COMMON_BUILDER.comment("Backpack Upgrades").translation("fxntstorage.configuration.backpack").push("backpack");
            COMMON_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            BACKPACK_MAGNET_RANGE = COMMON_BUILDER
                    .comment("The range (in blocks) around the backpack within which the Magnet Upgrade will pull items.")
                    .translation("fxntstorage.configuration.backpackMagnetRange")
                    .defineInRange("backpackMagnetRange", 5, 2, 16);
            COMMON_BUILDER.pop();
            COMMON_BUILDER.comment("Ore Mining Upgrade").push("ore_mining_upgrade");
            OREMINE_ORES_ONLY = COMMON_BUILDER
                    .comment("Limits the Ore Mining Upgrade to mine ores only. Disable to allow any block (up to 64).")
                    .translation("fxntstorage.configuration.mineOresOnly")
                    .define("mineOresOnly", true);
            COMMON_BUILDER.pop();
            COMMON_BUILDER.comment("Flight Upgrade").push("flight_upgrade");
            JETPACK_ALLOW_VOID_FLIGHT = COMMON_BUILDER
                    .comment("Allow the use of the flight upgrade over the void in The End dimension.")
                    .translation("fxntstorage.configuration.jetpackAllowVoidFlight")
                    .define("jetpackAllowVoidFlight", false);
            ELYTRA_BOOST_ENABLED = COMMON_BUILDER
                    .comment("Enable Jetpack boosting while gliding with an Elytra equipped.")
                    .translation("fxntstorage.configuration.elytraBoostEnabled")
                    .define("elytraBoostEnabled", true);
            ELYTRA_BOOST_MULTIPLIER = COMMON_BUILDER
                    .comment("Multiplier for Jetpack fuel consumption while Elytra boosting.")
                    .translation("fxntstorage.configuration.elytraBoostMultiplier")
                    .defineInRange("elytraBoostMultiplier", 4, 1, 10);
            ELYTRA_BOOST_SPEED_MULTIPLIER = COMMON_BUILDER
                    .comment("Multiplier for Jetpack speed while Elytra boosting.")
                    .translation("fxntstorage.configuration.elytraBoostSpeedMultiplier")
                    .defineInRange("elytraBoostSpeedMultiplier", 1.5, 1.0, 5.0);
            JETPACK_MINING_PENALTY = COMMON_BUILDER
                    .comment("Apply mining speed penalty when flying with the Jetpack.")
                    .translation("fxntstorage.configuration.jetpackMiningPenalty")
                    .define("jetpackMiningPenalty", true);
            COMMON_BUILDER.pop();
            COMMON_BUILDER.comment("Refill Upgrade").push("refill_upgrade");
            REFILL_BLACKLIST = COMMON_BUILDER
                    .comment("Blocks in this list will be ignored by the Refill Upgrade. A wildcard (*) can be used to blacklist all blocks within a namespace. (e.g. minecraft:*)")
                    .translation("fxntstorage.configuration.refillBlacklist")
                    .defineListAllowEmpty("refillBlacklist", List.of(), ConfigManager::validateBlacklist);
            COMMON_BUILDER.pop();
            CURIOS_KEEP_BACKPACK = COMMON_BUILDER
                    .comment("Keep Backpack equipped in Curios slot upon death.")
                    .translation("fxntstorage.configuration.keepBackpackOnDeath")
                    .define("keepBackpackOnDeath", true);
            COMMON_BUILDER.pop();

            // STORAGE BOX
            COMMON_BUILDER.comment("Storage Box").push("storage_box");
            STORAGE_BOX_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks before Storage Boxes update their block count and block states. Higher value = better performance.")
                    .translation("fxntstorage.configuration.storageBoxUpdateTime")
                    .define("storageBoxUpdateTime", 20);
            COMMON_BUILDER.pop();

            // STORAGE NETWORK
            COMMON_BUILDER.comment("Storage Network").push("storage_network");
            SIMPLE_STORAGE_NETWORK_FILL_EMPTY = COMMON_BUILDER
                    .comment("When all existing boxes with matching filters are full, allow the Storage Network to insert into empty boxes instead.")
                    .translation("fxntstorage.configuration.allowInsertIntoEmptyBoxes")
                    .define("allowInsertIntoEmptyBoxes", true);
            SIMPLE_STORAGE_NETWORK_RANGE = COMMON_BUILDER
                    .comment("The maximum number of blocks the storage network will search from the controller to find connected components.")
                    .translation("fxntstorage.configuration.simpleStorageNetworkRange")
                    .defineInRange("simpleStorageNetworkRange", 32, 8, 64);
            SIMPLE_STORAGE_NETWORK_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks between each update check for controllers and interfaces to refresh their connection to the storage network.")
                    .translation("fxntstorage.configuration.simpleStorageNetworkUpdateTime")
                    .define("simpleStorageNetworkUpdateTime", 20);
            COMMON_BUILDER.pop();

            COMMON_SPEC = COMMON_BUILDER.build();
        }

    }

    public static class ClientConfig {
        public static final ForgeConfigSpec CLIENT_SPEC;
        public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.BooleanValue DISPLAY_JETPACK_AIR_OVERLAY;
        public static ForgeConfigSpec.BooleanValue JETPACK_HOVER_BOBBING;
        public static ForgeConfigSpec.BooleanValue DISPLAY_FEEDER_MESSAGE;
        public static ForgeConfigSpec.BooleanValue ALLOW_CHORUS_FRUIT;
        public static ForgeConfigSpec.BooleanValue MAGNET_IGNORE_FAN_PROCESSING;
        public static ForgeConfigSpec.BooleanValue TOOLSWAP_PREFER_SILK_TOUCH;
        public static ForgeConfigSpec.ConfigValue<List<? extends String>> TOOLSWAP_PREFERS_SILK_TOUCH_LIST;
        public static ForgeConfigSpec.IntValue TORCH_DEPLOYER_COOLDOWN;
        public static ForgeConfigSpec.IntValue TORCH_DEPLOYER_LIGHT_LEVEL;
        public static ForgeConfigSpec.EnumValue<TorchDeployerLightSource> TORCH_DEPLOYER_LIGHT_SOURCE;
        public static ForgeConfigSpec.BooleanValue CHECK_BACKPACK_FOR_PROJECTILES;
        public static ForgeConfigSpec.BooleanValue CHECK_BACKPACK_FOR_TOOLBOX_ITEMS;
        public static ForgeConfigSpec.IntValue FEEDER_HUNGER_LEVEL;
        public static ForgeConfigSpec.IntValue FEEDER_HEALTH_THRESHOLD;
        public static ForgeConfigSpec.EnumValue<SimpleStorageGoggleOverlay> SIMPLE_STORAGE_GOGGLE_INFO;

        public enum TorchDeployerLightSource {
            BLOCK_LIGHT,
            SKY_LIGHT
        }

        public enum SimpleStorageGoggleOverlay {
            OFF,
            ONLY_TAGGED,
            ALL_ITEMS
        }

        static {
            CLIENT_BUILDER.comment("Feeder Upgrade").push("feeder_upgrade");
            DISPLAY_FEEDER_MESSAGE = CLIENT_BUILDER
                    .comment("Display a message on screen when the Feeder Upgrade automatically feeds the player.")
                    .translation("fxntstorage.configuration.displayFeederMessage")
                    .define("displayFeederMessage", true);
            ALLOW_CHORUS_FRUIT = CLIENT_BUILDER
                    .comment("Allow the Feeder Upgrade to use Chorus Fruit when feeding the player.")
                    .translation("fxntstorage.configuration.allowChorusFruit")
                    .define("allowChorusFruit", false);
            FEEDER_HEALTH_THRESHOLD = CLIENT_BUILDER
                    .comment("Feeder Upgrade activates when health percentage falls below this value, regardless of Hunger Level.")
                    .translation("fxntstorage.configuration.feederHealthThreshold")
                    .defineInRange("feederHealthThreshold", 80, 1, 100);
            FEEDER_HUNGER_LEVEL = CLIENT_BUILDER
                    .comment("Feeder Upgrade activates when hunger falls below this value. One point = half a drumstick")
                    .translation("fxntstorage.configuration.feederHungerLevel")
                    .defineInRange("feederHungerLevel", 18, 1, 20);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Flight Upgrade").push("flight_upgrade");
            DISPLAY_JETPACK_AIR_OVERLAY = CLIENT_BUILDER
                    .comment("Display how much air is left in the jetpack on the HUD.")
                    .translation("fxntstorage.configuration.displayJetpackAirOverlay")
                    .define("displayJetpackAirOverlay", true);
            JETPACK_HOVER_BOBBING = CLIENT_BUILDER
                    .comment("Add a gentle up-and-down motion while hovering with the jetpack.")
                    .translation("fxntstorage.configuration.jetpackHoverBobbing")
                    .define("jetpackHoverBobbing", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            MAGNET_IGNORE_FAN_PROCESSING = CLIENT_BUILDER
                    .comment("Prevent the Magnet Upgrade from pulling items that are being processed by a Create Encased Fan.")
                    .translation("fxntstorage.configuration.ignoreFanProcessing")
                    .define("ignoreFanProcessing", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Tool Swap Upgrade").push("tool_swap_upgrade");
            TOOLSWAP_PREFER_SILK_TOUCH = CLIENT_BUILDER
                    .comment("Prefer tools with Silk Touch when breaking blocks from the prefers Silk Touch list.")
                    .translation("fxntstorage.configuration.preferSilkTouch")
                    .define("preferSilkTouch", false);
            TOOLSWAP_PREFERS_SILK_TOUCH_LIST = CLIENT_BUILDER
                    .comment("Blocks in this list will drop the block when broken with a Silk Touch tool.")
                    .translation("fxntstorage.configuration.prefersSilkTouchList")
                    .defineListAllowEmpty("prefersSilkTouchList", List.of("minecraft:grass_block", "minecraft:mycelium", "minecraft:podzol", "minecraft:clay", "minecraft:gravel", "minecraft:snow",
                                    "minecraft:glowstone", "minecraft:stone", "minecraft:sea_lantern", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                                    "minecraft:gilded_blackstone", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                                    "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
                            ConfigManager::validateBlock);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Torch Deployer Upgrade").push("torch_deployer_upgrade");
            TORCH_DEPLOYER_LIGHT_LEVEL = CLIENT_BUILDER
                    .comment("Deploys a torch when light level is at or below this level. 0 = very dark, 15 = daylight")
                    .translation("fxntstorage.configuration.torchDeployerLightLevel")
                    .defineInRange("torchDeployerLightLevel", 2, 0, 15);
            TORCH_DEPLOYER_COOLDOWN = CLIENT_BUILDER
                    .comment("Delay between each torch placement attempt (in ticks). 20 ticks = 1 second.")
                    .translation("fxntstorage.configuration.torchDeployerCooldown")
                    .defineInRange("torchDeployerCooldown", 20, 0, Integer.MAX_VALUE);
            TORCH_DEPLOYER_LIGHT_SOURCE = CLIENT_BUILDER
                    .comment("Choose the light source to check when determining block light levels.")
                    .translation("fxntstorage.configuration.torchDeployerLightSource")
                    .defineEnum("torchDeployerLightSource", TorchDeployerLightSource.BLOCK_LIGHT);
            CLIENT_BUILDER.pop();

            CHECK_BACKPACK_FOR_PROJECTILES = CLIENT_BUILDER
                    .comment("Ranged weapons can use arrows and other ammo stored in equipped backpack.")
                    .translation("fxntstorage.configuration.checkBackpackForProjectiles")
                    .define("checkBackpackForProjectiles", true);
            CHECK_BACKPACK_FOR_TOOLBOX_ITEMS = CLIENT_BUILDER
                    .comment("Toolbox integration, allowing items to be transferred directly from equipped backpack.")
                    .translation("fxntstorage.configuration.checkBackpackForToolboxItems")
                    .define("checkBackpackForToolboxItems", true);

            SIMPLE_STORAGE_GOGGLE_INFO = CLIENT_BUILDER
                    .comment("Display goggle overlay for items with tag data in Simple Storage Boxes. (e.g. enchanted items, potions, tipped arrows or trimmed armor)")
                    .translation("fxntstorage.configuration.simpleStorageGoggleInfo")
                    .defineEnum("simpleStorageGoggleInfo", SimpleStorageGoggleOverlay.ONLY_TAGGED);

            CLIENT_SPEC = CLIENT_BUILDER.build();
        }

        public static void sendSettings(Player player) {
            // Save client settings
            CompoundTag playerData = player.getPersistentData();
            CompoundTag fxntSettingsTag = Util.getOrCreateSubTag(playerData, FXNTSTORAGE_SETTINGS_TAG);
            fxntSettingsTag.putBoolean("JetpackHoverBobbing", JETPACK_HOVER_BOBBING.get()); // Needed for clientside

            // Serialize and send client settings to server
            ListTag listTag = new ListTag();
            for (String entry : TOOLSWAP_PREFERS_SILK_TOUCH_LIST.get()) {
                listTag.add(StringTag.valueOf(entry));
            }

            CompoundTag settings = new CompoundTag();
            settings.put("PrefersSilkTouchList", listTag);
            settings.putBoolean("PreferSilkTouch", TOOLSWAP_PREFER_SILK_TOUCH.get());
            settings.putBoolean("IgnoreFanProcessing", MAGNET_IGNORE_FAN_PROCESSING.get());
            settings.putBoolean("DisplayFeederMessage", DISPLAY_FEEDER_MESSAGE.get());
            settings.putBoolean("AllowChorusFruit", ALLOW_CHORUS_FRUIT.get());
            settings.putInt("TorchDeployerCooldown", TORCH_DEPLOYER_COOLDOWN.get());
            settings.putInt("TorchDeployerLightLevel", TORCH_DEPLOYER_LIGHT_LEVEL.get());
            settings.putString("TorchDeployerLightSource", TORCH_DEPLOYER_LIGHT_SOURCE.get().name());
            settings.putBoolean("JetpackHoverBobbing", JETPACK_HOVER_BOBBING.get());
            settings.putBoolean("CheckBackpackForProjectiles", CHECK_BACKPACK_FOR_PROJECTILES.get());
            settings.putBoolean("CheckBackpackForToolboxItems", CHECK_BACKPACK_FOR_TOOLBOX_ITEMS.get());
            settings.putInt("FeederHungerLevel", FEEDER_HUNGER_LEVEL.get());
            settings.putInt("FeederHealthThreshold", FEEDER_HEALTH_THRESHOLD.get());

            ModNetwork.sendToServer(new SyncClientSettingsPacket(settings));
        }

    }

    private static boolean validateBlock(final Object obj) {
        if (!(obj instanceof String block) || block.isBlank()) {
            return false;
        }

        try {
            ResourceLocation id = ResourceLocation.parse(block);
            return ForgeRegistries.BLOCKS.containsKey(id);
        } catch (ResourceLocationException e) {
            return false;
        }
    }

    private static boolean validateBlacklist(final Object obj) {
        if (!(obj instanceof String item)) return false;

        // Check if item is a valid block entry
        if (ConfigManager.validateBlock(obj)) return true;

        // Check for wildcard entry
        if (item.endsWith(":*")) {
            String namespace = item.substring(0, item.length() - 2);

            // Check namespace is a valid format
            if (namespace.isEmpty()) return false;
            if (!namespace.matches("^[a-z0-9_.-]+$")) return false;

            // Check if namespace exists in registry
            return ForgeRegistries.ITEMS.getKeys().stream()
                    .anyMatch(key -> key.getNamespace().equals(namespace));
        }

        return false;
    }

}
