package net.fxnt.fxntstorage;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.ponder.foundation.PonderIndex;
import net.fxnt.fxntstorage.backpacks.main.BackpackClientTooltip;
import net.fxnt.fxntstorage.backpacks.main.BackpackScreen;
import net.fxnt.fxntstorage.backpacks.main.BackpackTooltip;
import net.fxnt.fxntstorage.backpacks.renderer.BackpackRenderPlayer;
import net.fxnt.fxntstorage.backpacks.upgrades.JetpackAirOverlay;
import net.fxnt.fxntstorage.backpacks.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.containers.StorageBoxScreen;
import net.fxnt.fxntstorage.init.*;
import net.fxnt.fxntstorage.ponder.CsPonderPlugin;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxScreen;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod(FXNTStorage.MOD_ID)
public class FXNTStorage {

    public static final String MOD_ID = "fxntstorage";
    public static final Logger LOGGER = LogManager.getLogger(FXNTStorage.class);
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public static boolean curiosLoaded;
    public static boolean invSorterLoaded;

    public FXNTStorage(final FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, ConfigManager.ClientConfig.CLIENT_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ConfigManager.CommonConfig.COMMON_SPEC);

        IEventBus modEventBus = context.getModEventBus();

        if (FMLEnvironment.dist == Dist.CLIENT) modEventBus.addListener(FXNTStorage::registerTooltipComponent);

        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModRecipes.register(modEventBus);
        ModTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        REGISTRATE.registerEventListeners(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(FXNTStorage::onCommonSetup);

        curiosLoaded = ModList.get().isLoaded(ModCompats.CURIOS);
        invSorterLoaded = ModList.get().isLoaded(ModCompats.INVENTORY_SORTER);

    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.registerCommonPackets();
            ModUnpackers.registerHandlers();
        });
    }

    private static void registerTooltipComponent(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BackpackTooltip.class, BackpackClientTooltip::new);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
            MenuScreens.register(ModMenuTypes.SIMPLE_STORAGE_BOX_MENU.get(), SimpleStorageBoxScreen::createScreen);
            MenuScreens.register(ModMenuTypes.STORAGE_BOX_MENU.get(), StorageBoxScreen::createScreen);
            MenuScreens.register(ModMenuTypes.BACK_PACK_ITEM_MENU.get(), BackpackScreen::new);
            MenuScreens.register(ModMenuTypes.BACK_PACK_BLOCK_MENU.get(), BackpackScreen::new);

            event.enqueueWork(ModNetwork::registerClientPackets);
            PonderIndex.addPlugin(new CsPonderPlugin());
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            addPlayerLayer(event, "default");
            addPlayerLayer(event, "slim");
        }

        private static void addPlayerLayer(EntityRenderersEvent.AddLayers event, String skinType) {
            PlayerRenderer playerRenderer = event.getSkin(skinType);

            if (playerRenderer != null) {
                playerRenderer.addLayer(new BackpackRenderPlayer(playerRenderer));
            }
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeybindHandler.TOGGLE_BACKPACK_KEY);
            event.register(KeybindHandler.TOGGLE_JETPACK_HOVER_KEY);
            event.register(KeybindHandler.CLEAR_BACKPACK_SHAPE_CACHE);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "remaining_air", JetpackAirOverlay.INSTANCE);
        }
    }

}
