package net.fxnt.fxntstorage.util;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.main.BackpackScreen;
import net.fxnt.fxntstorage.backpacks.upgrades.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.fxnt.fxntstorage.backpacks.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.cache.PasserShapeCache;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    public static final String KEY_CATEGORY_FXNTSTORAGE = "key.fxntstorage.category";
    public static final String KEY_TOGGLE_BACKPACK = "key.fxntstorage.toggle_backpack";
    public static final String KEY_TOGGLE_JETPACK_HOVER = "key.fxntstorage.toggle_jetpack_hover";
    public static final String KEY_CLEAR_BACKPACK_SHAPE_CACHE = "key.fxntstorage.clear_backpack_shape_cache";
    public static final String KEY_FLY_JETPACK = "key.fxntstorage.fly_jetpack";
    public static final String KEY_SORT_INVENTORY = "key.fxntstorage.sort_inventory";

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
                    ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.JETPACK_FLY, new FriendlyByteBuf(Unpooled.buffer().writeByte(Util.JETPACK_KEY_PRESS))));
                    if (new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE)) {
                        player.getPersistentData().putBoolean("fxntJetpackFlying", true);
                    }

                } else if (this.isDownOld != pValue && !pValue) {
                    ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.JETPACK_FLY, new FriendlyByteBuf(Unpooled.buffer().writeByte(Util.JETPACK_KEY_RELEASE))));
                    player.getPersistentData().putBoolean("fxntJetpackFlying", false);
                }

                this.isDownOld = pValue;
            }
        }
    };

    @Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(TickEvent.@NotNull ClientTickEvent event) {

            if (event.phase == TickEvent.Phase.END) {
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

    }

    private static void handleActivateDeactivateHover(Player player) {
        if (new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE))
            ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.TOGGLE_HOVER, new FriendlyByteBuf(Unpooled.buffer())));
    }

    public static void handleOpenCloseBackpack() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte((Minecraft.getInstance().screen instanceof BackpackScreen) ? Util.CLOSE_BACKPACK : Util.OPEN_BACKPACK);
        ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.BACKPACK_KEY_OPEN_CLOSE, buf));
    }

}
