package net.fxnt.fxntstorage.config;

import net.fxnt.fxntstorage.network.packet.SyncClientSettingsPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ConfigManager {
    public static final String FXNTSTORAGE_SETTINGS_TAG = "fxntstorageSettings";

    public static class CommonConfig {
        public static final ModConfigSpec COMMON_SPEC;
        public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

        public static ModConfigSpec.ConfigValue<Integer> STORAGE_BOX_UPDATE_TIME;
        public static ModConfigSpec.BooleanValue SIMPLE_STORAGE_NETWORK_FILL_EMPTY;
        public static ModConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_RANGE;
        public static ModConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_UPDATE_TIME;
        public static ModConfigSpec.BooleanValue CURIOS_KEEP_BACKPACK;

        // BACKPACK UPGRADES
        public static ModConfigSpec.ConfigValue<Integer> BACKPACK_MAGNET_RANGE;
        public static ModConfigSpec.BooleanValue ELYTRA_BOOST_ENABLED;
        public static ModConfigSpec.IntValue ELYTRA_BOOST_MULTIPLIER;
        public static ModConfigSpec.DoubleValue ELYTRA_BOOST_SPEED_MULTIPLIER;
        public static ModConfigSpec.BooleanValue OREMINE_ORES_ONLY;
        public static ModConfigSpec.BooleanValue JETPACK_MINING_PENALTY;
        public static ModConfigSpec.BooleanValue JETPACK_ALLOW_VOID_FLIGHT;

        static {
            // BACKPACK
            COMMON_BUILDER.comment("Backpack").push("backpack");
            COMMON_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            BACKPACK_MAGNET_RANGE = COMMON_BUILDER
                    .comment("The range (in blocks) around the backpack within which the Magnet Upgrade will pull items.")
                    .defineInRange("backpackMagnetRange", 5, 2, 16);
            COMMON_BUILDER.pop();
            COMMON_BUILDER.comment("Ore Mining Upgrade").push("ore_mining_upgrade");
            OREMINE_ORES_ONLY = COMMON_BUILDER
                    .comment("Limits the Ore Mining Upgrade to mine ores only. Disable to allow all blocks (up to 64).")
                    .define("mineOresOnly", true);
            COMMON_BUILDER.pop();
            COMMON_BUILDER.comment("Flight Upgrade").push("flight_upgrade");
            JETPACK_ALLOW_VOID_FLIGHT = COMMON_BUILDER
                    .comment("Allow the use of the flight upgrade over the void in The End dimension.")
                    .define("jetpackAllowVoidFlight", false);
            ELYTRA_BOOST_ENABLED = COMMON_BUILDER
                    .comment("Enable Jetpack boosting while gliding with an Elytra equipped.")
                    .define("elytraBoostEnabled", true);
            ELYTRA_BOOST_MULTIPLIER = COMMON_BUILDER
                    .comment("Multiplier for Jetpack fuel consumption while Elytra boosting.")
                    .defineInRange("elytraBoostMultiplier", 4, 1, 10);
            ELYTRA_BOOST_SPEED_MULTIPLIER = COMMON_BUILDER
                    .comment("Multiplier for Jetpack speed while Elytra boosting.")
                    .defineInRange("elytraBoostSpeedMultiplier", 1.5, 1.0, 5.0);
            JETPACK_MINING_PENALTY = COMMON_BUILDER
                    .comment("Apply mining speed penalty when flying with the Jetpack.")
                    .define("jetpackMiningPenalty", true);
            COMMON_BUILDER.pop();
            CURIOS_KEEP_BACKPACK = COMMON_BUILDER
                    .comment("Keep Backpack equipped in Curios slot upon death.")
                    .define("keepBackpackOnDeath", true);
            COMMON_BUILDER.pop();

            // STORAGE BOX
            COMMON_BUILDER.comment("Storage Box").push("storage_box");
            STORAGE_BOX_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks before Storage Boxes update their block count and block states. Higher value = better performance.")
                    .define("storageBoxUpdateTime", 20);
            COMMON_BUILDER.pop();

            // STORAGE NETWORK
            COMMON_BUILDER.comment("Storage Network").push("storage_network");
            SIMPLE_STORAGE_NETWORK_FILL_EMPTY = COMMON_BUILDER
                    .comment("When all existing boxes with matching filters are full, allow the Storage Network to insert items into empty boxes instead.")
                    .define("allowInsertIntoEmptyBoxes", true);
            SIMPLE_STORAGE_NETWORK_RANGE = COMMON_BUILDER
                    .comment("The maximum number of blocks the storage network will search from the controller to find connected components.")
                    .defineInRange("simpleStorageNetworkRange", 32, 8, 64);
            SIMPLE_STORAGE_NETWORK_UPDATE_TIME = COMMON_BUILDER
                    .comment("The number of ticks between each update check for controllers and interfaces to refresh their connection to the storage network.")
                    .define("simpleStorageNetworkUpdateTime", 20);
            COMMON_BUILDER.pop();

            COMMON_SPEC = COMMON_BUILDER.build();
        }

    }

    public static class ClientConfig {
        public static final ModConfigSpec CLIENT_SPEC;
        public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

        public static ModConfigSpec.BooleanValue DISPLAY_JETPACK_AIR_OVERLAY;
        public static ModConfigSpec.BooleanValue JETPACK_HOVER_BOBBING;
        public static ModConfigSpec.BooleanValue DISPLAY_FEEDER_MESSAGE;
        public static ModConfigSpec.BooleanValue ALLOW_CHORUS_FRUIT;
        public static ModConfigSpec.BooleanValue MAGNET_IGNORE_FAN_PROCESSING;
        public static ModConfigSpec.BooleanValue TOOLSWAP_PREFER_SILK_TOUCH;
        public static ModConfigSpec.ConfigValue<List<? extends String>> TOOLSWAP_PREFERS_SILK_TOUCH_LIST;
        public static ModConfigSpec.IntValue TORCH_DEPLOYER_COOLDOWN;
        public static ModConfigSpec.IntValue TORCH_DEPLOYER_LIGHT_LEVEL;
        public static ModConfigSpec.EnumValue<TorchDeployerLightSource> TORCH_DEPLOYER_LIGHT_SOURCE;
        public static ModConfigSpec.BooleanValue CHECK_BACKPACK_FOR_PROJECTILES;
        public static ModConfigSpec.BooleanValue CHECK_BACKPACK_FOR_TOOLBOX_ITEMS;

        public enum TorchDeployerLightSource {
            BLOCK_LIGHT,
            SKY_LIGHT
        }

        static {
            CLIENT_BUILDER.comment("Feeder Upgrade").push("feeder_upgrade");
            DISPLAY_FEEDER_MESSAGE = CLIENT_BUILDER
                    .comment("Display a message on screen when the Feeder Upgrade automatically feeds the player.")
                    .define("displayFeederMessage", true);
            ALLOW_CHORUS_FRUIT = CLIENT_BUILDER
                    .comment("Allow the Feeder Upgrade to use Chorus Fruit when feeding the player.")
                    .define("allowChorusFruit", false);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Jetpack Upgrade").push("jetpack_upgrade");
            DISPLAY_JETPACK_AIR_OVERLAY = CLIENT_BUILDER
                    .comment("Display how much air is left in the jetpack on the HUD.")
                    .define("displayJetpackAirOverlay", true);
            JETPACK_HOVER_BOBBING = CLIENT_BUILDER
                    .comment("Add a gentle up-and-down motion while hovering with the jetpack.")
                    .define("jetpackHoverBobbing", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            MAGNET_IGNORE_FAN_PROCESSING = CLIENT_BUILDER
                    .comment("Prevent the Magnet Upgrade from pulling items that are being processed by a Create Encased Fan.")
                    .define("ignoreFanProcessing", true);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Tool Swap Upgrade").push("tool_swap_upgrade");
            TOOLSWAP_PREFER_SILK_TOUCH = CLIENT_BUILDER
                    .comment("Prefer tools with Silk Touch when breaking blocks from the prefers Silk Touch list.")
                    .define("preferSilkTouch", false);
            TOOLSWAP_PREFERS_SILK_TOUCH_LIST = CLIENT_BUILDER
                    .comment("Blocks in this list will drop the block when broken with a Silk Touch tool.")
                    .defineListAllowEmpty("prefersSilkTouchList", List.of("minecraft:grass_block", "minecraft:mycelium", "minecraft:podzol", "minecraft:clay", "minecraft:gravel", "minecraft:snow",
                                    "minecraft:glowstone", "minecraft:stone", "minecraft:sea_lantern", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                                    "minecraft:gilded_blackstone", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                                    "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
                            () -> "minecraft:stone",
                            ConfigManager::validateBlock);
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Torch Deployer Upgrade").push("torch_deployer_upgrade");
            TORCH_DEPLOYER_LIGHT_LEVEL = CLIENT_BUILDER
                    .comment("Deploys a torch when light level is at or below this value. 0 = very dark, 15 = daylight")
                    .defineInRange("torchDeployerLightLevel", 2, 0, 15);
            TORCH_DEPLOYER_COOLDOWN = CLIENT_BUILDER
                    .comment("Delay between each torch placement attempt (in ticks). 20 ticks = 1 second.")
                    .defineInRange("torchDeployerCooldown", 20, 0, Integer.MAX_VALUE);
            TORCH_DEPLOYER_LIGHT_SOURCE = CLIENT_BUILDER
                    .comment("Choose the light source to check when determining block light levels.")
                    .defineEnum("torchDeployerLightSource", TorchDeployerLightSource.BLOCK_LIGHT);
            CLIENT_BUILDER.pop();

            CHECK_BACKPACK_FOR_PROJECTILES = CLIENT_BUILDER
                    .comment("Ranged weapons can use arrows and other ammo stored in equipped backpack.")
                    .define("checkBackpackForProjectiles", true);
            CHECK_BACKPACK_FOR_TOOLBOX_ITEMS = CLIENT_BUILDER
                    .comment("Toolbox integration, allowing items to be transferred directly from equipped backpack.")
                    .define("checkBackpackForToolboxItems", true);

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
            settings.put("prefersSilkTouchList", listTag);
            settings.putBoolean("preferSilkTouch", TOOLSWAP_PREFER_SILK_TOUCH.get());
            settings.putBoolean("ignoreFanProcessing", MAGNET_IGNORE_FAN_PROCESSING.get());
            settings.putBoolean("displayFeederMessage", DISPLAY_FEEDER_MESSAGE.get());
            settings.putBoolean("allowChorusFruit", ALLOW_CHORUS_FRUIT.get());
            settings.putInt("torchDeployerCooldown", TORCH_DEPLOYER_COOLDOWN.get());
            settings.putInt("torchDeployerLightLevel", TORCH_DEPLOYER_LIGHT_LEVEL.get());
            settings.putString("torchDeployerLightSource", TORCH_DEPLOYER_LIGHT_SOURCE.get().name());
            settings.putBoolean("jetpackHoverBobbing", JETPACK_HOVER_BOBBING.get());
            settings.putBoolean("checkBackpackForProjectiles", CHECK_BACKPACK_FOR_PROJECTILES.get());
            settings.putBoolean("checkBackpackForToolboxItems", CHECK_BACKPACK_FOR_TOOLBOX_ITEMS.get());

            PacketDistributor.sendToServer(new SyncClientSettingsPacket(settings));
        }

    }

    private static boolean validateBlock(final Object obj) {
        if (!(obj instanceof String block) || block.isBlank()) {
            return false;
        }

        try {
            ResourceLocation id = ResourceLocation.parse(block);
            return BuiltInRegistries.BLOCK.containsKey(id);
        } catch (ResourceLocationException e) {
            return false;
        }
    }

}
