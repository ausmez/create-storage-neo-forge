package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackScreen;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.cache.PasserShapeCache;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.BackpackHotkeyPacket;
import net.fxnt.fxntstorage.network.packet.JetpackFlyPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    public static final String KEY_CATEGORY_FXNTSTORAGE = "hotKey.fxntstorage.category";
    public static final String KEY_TOGGLE_BACKPACK = "hotKey.fxntstorage.toggle_backpack";
    public static final String KEY_TOGGLE_JETPACK_HOVER = "hotKey.fxntstorage.toggle_jetpack_hover";
    public static final String KEY_CLEAR_BACKPACK_SHAPE_CACHE = "hotKey.fxntstorage.clear_backpack_shape_cache";
    public static final String KEY_FLY_JETPACK = "hotKey.fxntstorage.fly_jetpack";
    public static final String KEY_SORT_INVENTORY = "hotKey.fxntstorage.sort_inventory";

    public static final KeyMapping TOGGLE_BACKPACK_KEY = new KeyMapping(KEY_TOGGLE_BACKPACK, GLFW.GLFW_KEY_B, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping TOGGLE_JETPACK_HOVER_KEY = new KeyMapping(KEY_TOGGLE_JETPACK_HOVER, GLFW.GLFW_KEY_H, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping CLEAR_BACKPACK_SHAPE_CACHE = new KeyMapping(KEY_CLEAR_BACKPACK_SHAPE_CACHE, GLFW.GLFW_KEY_F10, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping FLY_JETPACK = new KeyMapping(KEY_FLY_JETPACK, GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_FXNTSTORAGE) {
        private boolean isDownOld = false;

        @Override
        public void setDown(boolean pValue) {
            super.setDown(pValue);
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                if (this.isDownOld != pValue && pValue) {
                    PacketDistributor.sendToServer(new JetpackFlyPacket(Util.JETPACK_KEY_PRESS));
                    if (new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE)) {
                        player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackFlying", true);
                    }

                } else if (this.isDownOld != pValue && !pValue) {
                    PacketDistributor.sendToServer(new JetpackFlyPacket(Util.JETPACK_KEY_RELEASE));
                    player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackFlying", false);
                }

                this.isDownOld = pValue;
            }
        }
    };

    @EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(ClientTickEvent.Post event) {
            Player player = Minecraft.getInstance().player;

            if (player != null) {
                while (KeybindHandler.TOGGLE_BACKPACK_KEY.consumeClick()) {
                    if (BackpackHelper.isWearingBackpack(player)) handleOpenCloseBackpack();
                }
                while (KeybindHandler.TOGGLE_JETPACK_HOVER_KEY.consumeClick()) {
                    handleActivateDeactivateHover(player);
                }
                while (KeybindHandler.CLEAR_BACKPACK_SHAPE_CACHE.consumeClick()) {
                    BackpackShapeCache.clearCache();
                    PasserShapeCache.clearCache();
                    player.sendSystemMessage(Component.translatable("fxntstorage.shape_cache.cleared"));
                }
            }
        }
    }

    private static void handleActivateDeactivateHover(Player player) {
        if (new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE))
            PacketDistributor.sendToServer(new BackpackHotkeyPacket(Util.TOGGLE_HOVER));
    }

    public static void handleOpenCloseBackpack() {
        PacketDistributor.sendToServer(new BackpackHotkeyPacket(
                (Minecraft.getInstance().screen instanceof BackpackScreen) ? Util.CLOSE_BACKPACK : Util.OPEN_BACKPACK)
        );
    }

}
