package net.fxnt.fxntstorage.config;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SyncClientSettingsPacket;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    public static final String FXNTSTORAGE_SETTINGS_TAG = "fxntstorageSettings";
    public static final int CURRENT_DATA_VERSION = 1;

    public record SyncableConfigEntry(String key, ForgeConfigSpec.ConfigValue<?> value) {

        public void writeToNBT(CompoundTag tag) {
            Object val = value.get();

            if (val instanceof Integer) {
                tag.putInt(key, (Integer) val);
            } else if (val instanceof Double) {
                tag.putDouble(key, (Double) val);
            } else if (val instanceof Float) {
                tag.putFloat(key, (Float) val);
            } else if (val instanceof Boolean) {
                tag.putBoolean(key, (Boolean) val);
            } else if (val instanceof String) {
                tag.putString(key, (String) val);
            } else if (val instanceof Enum<?>) {
                tag.putString(key, ((Enum<?>) val).name());
            } else if (val instanceof List<?>) {
                ListTag listTag = new ListTag();
                for (Object obj : (List<?>) val) {
                    listTag.add(StringTag.valueOf(String.valueOf(obj)));
                }
                tag.put(key, listTag);
            } else {
                throw new IllegalStateException("Unsupported config type for key: " + key);
            }
        }
    }

    private static final List<SyncableConfigEntry> SYNCED_CLIENT_SETTINGS = new ArrayList<>();

    public static class ServerConfig {
        public static final ForgeConfigSpec SERVER_SPEC;
        public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.BooleanValue CURIOS_KEEP_BACKPACK;
        public static ForgeConfigSpec.BooleanValue ELYTRA_BOOST_ENABLED;
        public static ForgeConfigSpec.IntValue ELYTRA_BOOST_MULTIPLIER;
        public static ForgeConfigSpec.DoubleValue ELYTRA_BOOST_SPEED_MULTIPLIER;
        public static ForgeConfigSpec.IntValue HEALTH_UPGRADE_BONUS;
        public static ForgeConfigSpec.BooleanValue JETPACK_MINING_PENALTY;
        public static ForgeConfigSpec.BooleanValue JETPACK_ALLOW_VOID_FLIGHT;
        public static ForgeConfigSpec.BooleanValue JUKEBOX_BUFFS_ENABLED;
        public static ForgeConfigSpec.IntValue JUKEBOX_BUFFS_RANGE;
        public static ForgeConfigSpec.BooleanValue JUKEBOX_NOTES_ENABLED;
        public static ForgeConfigSpec.ConfigValue<Integer> MAGNET_PULL_RANGE;
        public static ForgeConfigSpec.BooleanValue ORE_MINING_ORES_ONLY;
        public static ForgeConfigSpec.BooleanValue ORE_MINING_PREVIEW_ORE_VEIN;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_RANGE;
        public static ForgeConfigSpec.ConfigValue<Integer> SIMPLE_STORAGE_NETWORK_UPDATE_TIME;
        public static ForgeConfigSpec.ConfigValue<Integer> STORAGE_BOX_UPDATE_TIME;

        static {
            // BACKPACK UPGRADES
            SERVER_BUILDER.comment("Jetpack Upgrade").push("jetpack_upgrade");
            ELYTRA_BOOST_ENABLED = SERVER_BUILDER
                    .comment("Enable Jetpack boosting while gliding with an Elytra equipped.")
                    .translation("fxntstorage.configuration.elytraBoostEnabled")
                    .define("elytraBoostEnabled", true);
            ELYTRA_BOOST_MULTIPLIER = SERVER_BUILDER
                    .comment("Multiplier for Jetpack fuel consumption while Elytra boosting.")
                    .translation("fxntstorage.configuration.elytraBoostMultiplier")
                    .defineInRange("elytraBoostMultiplier", 4, 1, 10);
            ELYTRA_BOOST_SPEED_MULTIPLIER = SERVER_BUILDER
                    .comment("Multiplier for Jetpack speed while Elytra boosting.")
                    .translation("fxntstorage.configuration.elytraBoostSpeedMultiplier")
                    .defineInRange("elytraBoostSpeedMultiplier", 1.5, 1.0, 5.0);
            JETPACK_MINING_PENALTY = SERVER_BUILDER
                    .comment("Should the mining speed penalty be applied when flying with the Jetpack and mining?")
                    .translation("fxntstorage.configuration.jetpackMiningPenalty")
                    .define("jetpackMiningPenalty", true);
            JETPACK_ALLOW_VOID_FLIGHT = SERVER_BUILDER
                    .comment("Allow the use of the flight upgrade over the void in The End dimension.")
                    .translation("fxntstorage.configuration.jetpackAllowVoidFlight")
                    .define("jetpackAllowVoidFlight", false);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Jukebox Upgrade").push("jukebox_upgrade");
            JUKEBOX_BUFFS_ENABLED = SERVER_BUILDER
                    .comment("Enable the buff system to apply beneficial buffs to the player when a song is playing.")
                    .translation("fxntstorage.configuration.jukeboxBuffsEnabled")
                    .define("jukeboxBuffsEnabled", true);
            JUKEBOX_BUFFS_RANGE = SERVER_BUILDER
                    .comment("The range (in blocks) around the backpack within which the Jukebox Upgrade will apply buffs.")
                    .translation("fxntstorage.configuration.jukeboxBuffRange")
                    .defineInRange("jukeboxBuffRange", 16, 1, 64);
            JUKEBOX_NOTES_ENABLED = SERVER_BUILDER
                    .comment("Enable the music note particles on the BLOCK to be displayed when a music disc is playing.")
                    .translation("fxntstorage.configuration.jukeboxNotesEnabled")
                    .define("jukeboxNotesEnabled", true);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Magnet Upgrade").push("magnet_upgrade");
            MAGNET_PULL_RANGE = SERVER_BUILDER
                    .comment("The range (in blocks) around the backpack within which the Magnet Upgrade will pull items.")
                    .translation("fxntstorage.configuration.backpackMagnetRange")
                    .defineInRange("backpackMagnetRange", 5, 2, 16);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Mechanical Heart Upgrade").push("mechanical_heart_upgrade");
            HEALTH_UPGRADE_BONUS = SERVER_BUILDER
                    .comment("Specify the amount of bonus health (in hearts) the Mechanical Heart Upgrade should provider each player")
                    .translation("fxntstorage.configuration.healthBonusHealth")
                    .worldRestart()
                    .defineInRange("healthUpgradeBonus", 5, 0, 50);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Ore Mining Upgrade").push("ore_mining_upgrade");
            ORE_MINING_ORES_ONLY = SERVER_BUILDER
                    .comment("When enabled the Ore Mining upgrade will only consider ores when mining.")
                    .translation("fxntstorage.configuration.oreminingOresOnly")
                    .define("oreminingOresOnly", false);
            ORE_MINING_PREVIEW_ORE_VEIN = SERVER_BUILDER
                    .comment("Enable to allow the preview of ore veins by highlighting them with an outline.")
                    .translation("fxntstorage.configuration.oreminingPreviewOreVein")
                    .define("oreminingPreviewOreVein", true);
            SERVER_BUILDER.pop();

            // STORAGE BOX
            SERVER_BUILDER.comment("Storage Box").push("storage_box");
            STORAGE_BOX_UPDATE_TIME = SERVER_BUILDER
                    .comment("The number of ticks before Storage Boxes update their block count and block states. Higher value = better performance.")
                    .translation("fxntstorage.configuration.storageBoxUpdateTime")
                    .define("storageBoxUpdateTime", 20);
            SERVER_BUILDER.pop();

            // STORAGE NETWORK
            SERVER_BUILDER.comment("Storage Network").push("storage_network");
            SIMPLE_STORAGE_NETWORK_RANGE = SERVER_BUILDER
                    .comment("The maximum number of blocks the storage network will search from the controller to find connected components.")
                    .translation("fxntstorage.configuration.simpleStorageNetworkRange")
                    .defineInRange("simpleStorageNetworkRange", 32, 8, 64);
            SIMPLE_STORAGE_NETWORK_UPDATE_TIME = SERVER_BUILDER
                    .comment("The number of ticks between each update check for controllers and interfaces to refresh their connection to the storage network.")
                    .translation("fxntstorage.configuration.simpleStorageNetworkUpdateTime")
                    .define("simpleStorageNetworkUpdateTime", 20);
            SERVER_BUILDER.pop();

            CURIOS_KEEP_BACKPACK = SERVER_BUILDER
                    .comment("Keep Backpack equipped in Curios slot upon death.")
                    .translation("fxntstorage.configuration.keepBackpackOnDeath")
                    .define("keepBackpackOnDeath", true);

            SERVER_SPEC = SERVER_BUILDER.build();
        }
    }

    public static class ClientConfig {
        public static final ForgeConfigSpec CLIENT_SPEC;
        public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        public static ForgeConfigSpec.ConfigValue<List<? extends String>> TOOLSWAP_PREFERS_SILK_TOUCH_LIST;
        public static ForgeConfigSpec.IntValue TORCH_DEPLOYER_COOLDOWN;
        public static ForgeConfigSpec.IntValue TORCH_DEPLOYER_LIGHT_LEVEL;
        public static ForgeConfigSpec.EnumValue<TorchDeployerLightSource> TORCH_DEPLOYER_LIGHT_SOURCE;
        public static ForgeConfigSpec.BooleanValue CHECK_BACKPACK_FOR_PROJECTILES;
        public static ForgeConfigSpec.BooleanValue CHECK_BACKPACK_FOR_TOOLBOX_ITEMS;
        public static ForgeConfigSpec.IntValue FEEDER_HUNGER_LEVEL;
        public static ForgeConfigSpec.IntValue FEEDER_HEALTH_THRESHOLD;
        public static ForgeConfigSpec.EnumValue<SimpleStorageGoggleOverlay> SIMPLE_STORAGE_GOGGLE_INFO;
        public static ForgeConfigSpec.ConfigValue<List<? extends String>> REFILL_BLACKLIST;
        public static ForgeConfigSpec.BooleanValue JUKEBOX_NOTES_ENABLED;

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
            FEEDER_HEALTH_THRESHOLD = sync(
                    "FeederHealthThreshold",
                    CLIENT_BUILDER
                            .comment("Feeder Upgrade activates when health percentage falls below this value, regardless of Hunger Level.")
                            .translation("fxntstorage.configuration.feederHealthThreshold")
                            .defineInRange("feederHealthThreshold", 80, 1, 100)
            );
            FEEDER_HUNGER_LEVEL = sync(
                    "FeederHungerLevel",
                    CLIENT_BUILDER
                            .comment("Feeder Upgrade activates when hunger falls below this value. One point = half a drumstick")
                            .translation("fxntstorage.configuration.feederHungerLevel")
                            .defineInRange("feederHungerLevel", 18, 1, 20)
            );
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Jukebox Upgrade").push("jukebox_upgrade");
            JUKEBOX_NOTES_ENABLED = sync(
                    "JukeboxNotesEnabled",
                    CLIENT_BUILDER
                            .comment("Enable the music note particles on the PLAYER to be displayed when a music disc is playing.")
                            .translation("fxntstorage.configuration.jukeboxNotesEnabled")
                            .define("jukeboxNotesEnabled", true)
            );
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Refill Upgrade").push("refill_upgrade");
            REFILL_BLACKLIST = sync(
                    "RefillBlackList",
                    CLIENT_BUILDER
                            .comment("Blocks in this list will be ignored by the Refill Upgrade. A wildcard (*) can be used to blacklist all blocks within a namespace. (e.g. minecraft:*)")
                            .translation("fxntstorage.configuration.refillBlacklist")
                            .defineListAllowEmpty("refillBlacklist", List.of(), ConfigManager::validateBlacklist)
            );
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Tool Swap Upgrade").push("tool_swap_upgrade");
            TOOLSWAP_PREFERS_SILK_TOUCH_LIST = sync(
                    "PrefersSilkTouchList",
                    CLIENT_BUILDER
                            .comment("When breaking a block from this list, the Tool Swap upgrade will prefer a Silk Touch tool if available.")
                            .translation("fxntstorage.configuration.prefersSilkTouchList")
                            .defineListAllowEmpty("prefersSilkTouchList", List.of("minecraft:grass_block", "minecraft:mycelium", "minecraft:podzol", "minecraft:clay", "minecraft:gravel", "minecraft:snow",
                                            "minecraft:glowstone", "minecraft:stone", "minecraft:sea_lantern", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                                            "minecraft:gilded_blackstone", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                                            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
                                    ConfigManager::validateBlock)
            );
            CLIENT_BUILDER.pop();

            CLIENT_BUILDER.comment("Torch Deployer Upgrade").push("torch_deployer_upgrade");
            TORCH_DEPLOYER_COOLDOWN = sync(
                    "TorchDeployerCooldown",
                    CLIENT_BUILDER
                            .comment("Delay between each torch placement attempt (in ticks). 20 ticks = 1 second.")
                            .translation("fxntstorage.configuration.torchDeployerCooldown")
                            .defineInRange("torchDeployerCooldown", 20, 0, Integer.MAX_VALUE)
            );
            TORCH_DEPLOYER_LIGHT_LEVEL = sync(
                    "TorchDeployerLightLevel",
                    CLIENT_BUILDER
                            .comment("Deploys a torch when light level is at or below this level. 0 = very dark, 15 = daylight")
                            .translation("fxntstorage.configuration.torchDeployerLightLevel")
                            .defineInRange("torchDeployerLightLevel", 2, 0, 15)
            );
            TORCH_DEPLOYER_LIGHT_SOURCE = sync(
                    "TorchDeployerLightSource",
                    CLIENT_BUILDER
                            .comment("Choose the light source to check when determining block light levels.")
                            .translation("fxntstorage.configuration.torchDeployerLightSource")
                            .defineEnum("torchDeployerLightSource", TorchDeployerLightSource.BLOCK_LIGHT)
            );
            CLIENT_BUILDER.pop();

            CHECK_BACKPACK_FOR_PROJECTILES = sync(
                    "CheckBackpackForProjectiles",
                    CLIENT_BUILDER
                            .comment("Ranged weapons can use arrows and other ammo stored in equipped backpack.")
                            .translation("fxntstorage.configuration.checkBackpackForProjectiles")
                            .define("checkBackpackForProjectiles", true)
            );
            CHECK_BACKPACK_FOR_TOOLBOX_ITEMS = sync(
                    "CheckBackpackForToolboxItems",
                    CLIENT_BUILDER
                            .comment("Toolbox integration, allowing items to be transferred directly from equipped backpack.")
                            .translation("fxntstorage.configuration.checkBackpackForToolboxItems")
                            .define("checkBackpackForToolboxItems", true)
            );
            SIMPLE_STORAGE_GOGGLE_INFO = CLIENT_BUILDER
                    .comment("Display goggle overlay for items with tag data in Simple Storage Boxes. (e.g. enchanted items, potions, tipped arrows or trimmed armor)")
                    .translation("fxntstorage.configuration.simpleStorageGoggleInfo")
                    .defineEnum("simpleStorageGoggleInfo", SimpleStorageGoggleOverlay.ONLY_TAGGED);

            CLIENT_SPEC = CLIENT_BUILDER.build();
        }

        private static <T extends ForgeConfigSpec.ConfigValue<?>> T sync(String key, T value) {
            SYNCED_CLIENT_SETTINGS.add(new SyncableConfigEntry(key, value));
            return value;
        }

        public static void sendClientSettings() {
            CompoundTag settings = new CompoundTag();
            for (SyncableConfigEntry entry : SYNCED_CLIENT_SETTINGS) {
                entry.writeToNBT(settings);
            }
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
        if (!(obj instanceof String item) || item.isBlank()) return false;

        if (item.endsWith(":*")) {
            String namespace = item.substring(0, item.length() - 2);
            return !namespace.isEmpty() && namespace.matches("^[a-z0-9_.-]+$");
        }

        return true;
    }

    public static void migrateSettings(CompoundTag settings, int oldVersion) {
        CompoundTag tag = settings.getCompound(FXNTSTORAGE_SETTINGS_TAG);

        if (oldVersion < 1) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                if (tag.contains("CheckBackpackForProjectiles"))
                    ClientConfig.CHECK_BACKPACK_FOR_PROJECTILES.set(tag.getBoolean("CheckBackpackForProjectiles"));
                if (tag.contains("CheckBackpackForToolboxItems"))
                    ClientConfig.CHECK_BACKPACK_FOR_TOOLBOX_ITEMS.set(tag.getBoolean("CheckBackpackForToolboxItems"));
                if (tag.contains("FeederHungerLevel"))
                    ClientConfig.FEEDER_HUNGER_LEVEL.set(tag.getInt("FeederHungerLevel"));
                if (tag.contains("FeederHealthThreshold"))
                    ClientConfig.FEEDER_HEALTH_THRESHOLD.set(tag.getInt("FeederHealthThreshold"));
                if (tag.contains("TorchDeployerLightLevel"))
                    ClientConfig.TORCH_DEPLOYER_LIGHT_LEVEL.set(tag.getInt("TorchDeployerLightLevel"));
                if (tag.contains("TorchDeployerLightSource"))
                    ClientConfig.TORCH_DEPLOYER_LIGHT_SOURCE.set(ClientConfig.TorchDeployerLightSource.valueOf(tag.getString("TorchDeployerLightSource")));
                if (tag.contains("TorchDeployerCooldown"))
                    ClientConfig.TORCH_DEPLOYER_COOLDOWN.set(tag.getInt("TorchDeployerCooldown"));

                if (tag.contains("PrefersSilkTouchList")) {
                    List<String> oldSilkTouchList = new ArrayList<>();
                    for (Tag tag1 : tag.getList("PrefersSilkTouchList", Tag.TAG_STRING)) {
                        oldSilkTouchList.add(tag1.getAsString());
                    }
                    ClientConfig.TOOLSWAP_PREFERS_SILK_TOUCH_LIST.set(oldSilkTouchList);
                }
            }

            tag.remove("AllowChorusFruit");
            tag.remove("CheckBackpackForProjectiles");
            tag.remove("CheckBackpackForToolboxItems");
            tag.remove("DisplayFeederMessage");
            tag.remove("FeederHealthThreshold");
            tag.remove("FeederHungerLevel");
            tag.remove("FeederSaturationLevel");
            tag.remove("IgnoreFanProcessing");
            tag.remove("JetpackFlying");
            tag.remove("Jetpackforward");
            tag.remove("JetpackForward");
            tag.remove("JetpackHoverBobbing");
            tag.remove("JetpackHoverStartTime");
            tag.remove("Jetpackleft");
            tag.remove("JetpackLeft");
            tag.remove("MainWeaponPreference");
            tag.remove("PreferSilkTouch");
            tag.remove("PrefersSilkTouchList");
            tag.remove("TorchDeployerCooldown");
            tag.remove("TorchDeployerLightLevel");
            tag.remove("TorchDeployerLightSource");
            tag.remove("JetpackHover");

            settings.remove("fxntstorage:last_attack_time");
            settings.remove("fxntstorage:last_block_pos");
            settings.remove("fxntstorage:last_click_time");
            settings.remove("fxntstorage:last_click_type");
            settings.remove("fxntJetpackFlying");
            settings.remove("fxntJetpackforward");
            settings.remove("fxntJetpackleft");
            settings.remove("fxntLastClickTime");
            settings.remove("fxntLastClickType");
            settings.remove("fxntIgnoreFanProcessing");
            settings.remove("fxntPrefersSilkTouchList");
            settings.remove("fxntPreferSilkTouch");
            settings.remove("fxntDisplayFeederMessage");
            settings.remove("fxntJetpackHover");
        }
    }
}
