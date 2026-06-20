package net.fxnt.fxntstorage;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import net.createmod.ponder.foundation.PonderIndex;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.client.renderer.BackpackBlockEntityRenderer;
import net.fxnt.fxntstorage.backpack.client.renderer.BackpackItemModel;
import net.fxnt.fxntstorage.backpack.client.renderer.BackpackItemRenderer;
import net.fxnt.fxntstorage.backpack.client.renderer.BackpackRenderPlayer;
import net.fxnt.fxntstorage.backpack.client.tooltip.BackpackClientTooltip;
import net.fxnt.fxntstorage.backpack.client.tooltip.BackpackTooltip;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeRegistry;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackAirOverlay;
import net.fxnt.fxntstorage.compat.CuriosCompat;
import net.fxnt.fxntstorage.compat.constructionstick.ConstructionStickCompat;
import net.fxnt.fxntstorage.compat.everycomp.EveryCompCompat;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBoxEntityRenderer;
import net.fxnt.fxntstorage.container.StorageBoxScreen;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedScreen;
import net.fxnt.fxntstorage.init.*;
import net.fxnt.fxntstorage.network.PacketHandler;
import net.fxnt.fxntstorage.ponder.CsPonderPlugin;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntityRenderer;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxScreen;
import net.fxnt.fxntstorage.simple_storage.CompactingRecipeHelper;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntityRenderer;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxScreen;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedScreen;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Mod(FXNTStorage.MOD_ID)
public class FXNTStorage {

    public static final String MOD_ID = "fxntstorage";
    public static final Logger LOGGER = LogManager.getLogger(FXNTStorage.class);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .addDataGenerator(ProviderType.LANG, ModLangProvider::provide);
    public static final int MAX_EFFECTS_PER_SONG = 3;

    public static final boolean CURIOS_LOADED = ModList.get().isLoaded(ModCompats.CURIOS);
    public static final boolean EMI_LOADED = ModList.get().isLoaded(ModCompats.EMI);
    public static final boolean REI_LOADED = ModList.get().isLoaded(ModCompats.REI);

    public FXNTStorage(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigManager.ClientConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigManager.ServerConfig.SERVER_SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(FXNTStorage::registerTooltipComponent);
            if (!ModList.get().isLoaded("configured"))
                modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.register(new PacketHandler());

        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModTabs.register(modEventBus);
        ModEffects.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModLootConditionTypes.register(modEventBus);
        ModLootFunctionTypes.register(modEventBus);
        ModAttachmentTypes.register(modEventBus);

        UpgradeRegistry.register();

        REGISTRATE.registerEventListeners(modEventBus);

        if (CURIOS_LOADED) loadCuriosCompat(modEventBus);
        if (ModList.get().isLoaded(ModCompats.CONSTRUCTION_STICK)) ConstructionStickCompat.init();
        if (ModList.get().isLoaded(ModCompats.EVERY_COMPAT)) EveryCompCompat.init();
    }

    private static void loadCuriosCompat(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(CuriosCompat::keepBackpack);
        NeoForge.EVENT_BUS.addListener(CuriosCompat::onCurioChange);
        bus.addListener(CuriosCompat::registerCapabilities);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModUnpackers.registerHandlers();
            ModInventoryIdentifiers.registerHandlers();
        });
    }

    private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BackpackTooltip.class, BackpackClientTooltip::new);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RESERVE_STORAGE_BOX_ENTITY.get(), (e, d) -> e.getAutomationHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_BOX_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), (e, d) -> e.getCapabilityHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BACKPACK_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_INTERFACE_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_INTERFACE_FILTERED_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerItem(
                Capabilities.ItemHandler.ITEM,
                (itemStack, context) -> BackpackContainer.Cache.getForCapability(itemStack),
                ModBlocks.BACKPACK.get(),
                ModBlocks.ANDESITE_BACKPACK.get(),
                ModBlocks.COPPER_BACKPACK.get(),
                ModBlocks.BRASS_BACKPACK.get(),
                ModBlocks.HARDENED_BACKPACK.get()
        );
    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static class ModEvents {

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(BuiltInRegistries.RECIPE_SERIALIZER.key(), ModRecipes::register);
        }
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onConfigReload(final ModConfigEvent.Reloading event) {
            if (Objects.equals(event.getConfig().getModId(), MOD_ID) && event.getConfig().getType().equals(ModConfig.Type.CLIENT)) {
                if (Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().player != null) {
                    ConfigManager.ClientConfig.sendClientSettings();
                }
            }
        }

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            PonderIndex.addPlugin(new CsPonderPlugin());
        }

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                CompactingRecipeHelper.rebuild(mc.level.getRecipeManager(), mc.level.registryAccess());
            }
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            addPlayerLayer(event, PlayerSkin.Model.WIDE);
            addPlayerLayer(event, PlayerSkin.Model.SLIM);
        }

        private static void addPlayerLayer(EntityRenderersEvent.AddLayers event, PlayerSkin.Model skinType) {
            PlayerRenderer playerRenderer = event.getSkin(skinType);

            if (playerRenderer != null) {
                playerRenderer.addLayer(new BackpackRenderPlayer(playerRenderer));
            }
        }

        @SubscribeEvent
        public static void registerEntityRenderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.STORAGE_BOX_ENTITY.get(), StorageBoxEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), SimpleStorageBoxEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.SMART_PASSER_ENTITY.get(), SmartBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STORAGE_INTERFACE_FILTERED_ENTITY.get(), SmartBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.BACKPACK_ENTITY.get(), BackpackBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.RESERVE_STORAGE_BOX_ENTITY.get(), ReserveStorageBoxEntityRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MAGNET_PICKUP_ENTITY.get(), NoopRenderer::new);
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            IClientItemExtensions extension = new IClientItemExtensions() {
                private BackpackItemRenderer renderer;

                @Override
                public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) renderer = new BackpackItemRenderer();
                    return renderer;
                }
            };

            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BackpackItem) {
                    event.registerItem(extension, item);
                }
            }
        }

        @SubscribeEvent
        public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof BackpackItem)) continue;
                ModelResourceLocation key = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item));
                BakedModel original = event.getModels().get(key);
                if (original != null && !(original instanceof BackpackItemModel)) {
                    event.getModels().put(key, new BackpackItemModel(original));
                }
            }
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.SIMPLE_STORAGE_BOX_MENU.get(), SimpleStorageBoxScreen::createScreen);
            event.register(ModMenuTypes.SIMPLE_STORAGE_BOX_MOUNTED_MENU.get(), SimpleStorageBoxMountedScreen::createScreen);
            event.register(ModMenuTypes.STORAGE_BOX_MENU.get(), StorageBoxScreen::createScreen);
            event.register(ModMenuTypes.STORAGE_BOX_MOUNTED_MENU.get(), StorageBoxMountedScreen::createScreen);
            event.register(ModMenuTypes.BACKPACK_MENU.get(), BackpackScreen::new);
            event.register(ModMenuTypes.RESERVE_STORAGE_BOX_MENU.get(), ReserveStorageBoxScreen::new);
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeybindHandler.TOGGLE_BACKPACK_KEY);
            event.register(KeybindHandler.TOGGLE_JETPACK_HOVER_KEY);
            event.register(KeybindHandler.ORE_MINE_ANY_BLOCK);
            event.register(KeybindHandler.COMPACTING_WHEEL_KEY);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ResourceLocation.fromNamespaceAndPath(MOD_ID, "remaining_air"), JetpackAirOverlay.INSTANCE);
        }
    }
}
