package net.fxnt.fxntstorage;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.ponder.foundation.PonderIndex;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackScreen;
import net.fxnt.fxntstorage.backpack.renderer.BackpackRenderPlayer;
import net.fxnt.fxntstorage.backpack.tooltip.BackpackClientTooltip;
import net.fxnt.fxntstorage.backpack.tooltip.BackpackTooltip;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackAirOverlay;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.compat.CuriosCompat;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBoxEntityRenderer;
import net.fxnt.fxntstorage.container.StorageBoxScreen;
import net.fxnt.fxntstorage.init.*;
import net.fxnt.fxntstorage.network.PacketHandler;
import net.fxnt.fxntstorage.passer.PasserEntityRenderer;
import net.fxnt.fxntstorage.ponder.CsPonderPlugin;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntityRenderer;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxScreen;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
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
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod(FXNTStorage.MOD_ID)
public class FXNTStorage {

    public static final String MOD_ID = "fxntstorage";
    public static final Logger LOGGER = LogManager.getLogger(FXNTStorage.class);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public static boolean curiosLoaded;
    public static boolean invSorterLoaded;

    public FXNTStorage(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigManager.ClientConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigManager.CommonConfig.COMMON_SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(FXNTStorage::registerTooltipComponent);
        }

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.register(new PacketHandler());

        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModDataComponents.register(modEventBus);

        REGISTRATE.registerEventListeners(modEventBus);

        curiosLoaded = ModList.get().isLoaded(ModCompats.CURIOS);
        invSorterLoaded = ModList.get().isLoaded(ModCompats.INVENTORY_SORTER);

        if (curiosLoaded) loadCuriosCompat(modEventBus);
    }

    private static void loadCuriosCompat(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(CuriosCompat::keepBackpack);
        bus.addListener(CuriosCompat::registerCapabilities);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModUnpackers::registerHandlers);
    }

    private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BackpackTooltip.class, BackpackClientTooltip::new);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_BOX_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), (e, d) -> e.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BACKPACK_ENTITY.get(), (e, d) -> e.getItemHandler());
        // TODO: Rewrite Storage Network to use Capabilities natively instead of NeoForge wrapper
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get(), (e, d) -> new InvWrapper(e));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STORAGE_INTERFACE_ENTITY.get(), (e, d) -> new InvWrapper(e));
        event.registerItem(Capabilities.ItemHandler.ITEM,
                (itemStack, context) -> new BackpackContainer(itemStack, null).getItemHandler(),
                ModBlocks.BACKPACK.get(),
                ModBlocks.ANDESITE_BACKPACK.get(),
                ModBlocks.COPPER_BACKPACK.get(),
                ModBlocks.BRASS_BACKPACK.get(),
                ModBlocks.HARDENED_BACKPACK.get()
        );
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(BuiltInRegistries.RECIPE_SERIALIZER.key(), ModRecipes::register);
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onConfigReload(final ModConfigEvent.Reloading event) {
            if (Objects.equals(event.getConfig().getModId(), MOD_ID) && event.getConfig().getType().equals(ModConfig.Type.CLIENT)) {
                if (Minecraft.getInstance().getConnection() != null) {
                    BackpackNetworkHelper.sendClientSettings();
                }
            }
        }

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            PonderIndex.addPlugin(new CsPonderPlugin());
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
            event.registerBlockEntityRenderer(ModBlockEntities.SMART_PASSER_ENTITY.get(), PasserEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.SIMPLE_STORAGE_BOX_MENU.get(), SimpleStorageBoxScreen::createScreen);
            event.register(ModMenuTypes.STORAGE_BOX_MENU.get(), StorageBoxScreen::createScreen);
            event.register(ModMenuTypes.BACKPACK_ITEM_MENU.get(), BackpackScreen::new);
            event.register(ModMenuTypes.BACKPACK_BLOCK_MENU.get(), BackpackScreen::new);
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeybindHandler.TOGGLE_BACKPACK_KEY);
            event.register(KeybindHandler.TOGGLE_JETPACK_HOVER_KEY);
            event.register(KeybindHandler.CLEAR_BACKPACK_SHAPE_CACHE);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ResourceLocation.fromNamespaceAndPath(MOD_ID, "remaining_air"), JetpackAirOverlay.INSTANCE);
        }
    }

}
