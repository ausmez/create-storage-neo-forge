package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.cache.PasserShapeCache;
import net.fxnt.fxntstorage.network.packet.JetpackFlyingPacket;
import net.fxnt.fxntstorage.network.packet.KeyPressedPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class KeybindHandler {

    public static final String KEY_CATEGORY_FXNTSTORAGE = "hotKey.fxntstorage.category";

    public static final KeyMapping TOGGLE_BACKPACK_KEY = new KeyMapping("hotKey.fxntstorage.toggle_backpack", GLFW.GLFW_KEY_B, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping TOGGLE_JETPACK_HOVER_KEY = new KeyMapping("hotKey.fxntstorage.toggle_jetpack_hover", GLFW.GLFW_KEY_H, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping CLEAR_BACKPACK_SHAPE_CACHE = new KeyMapping("hotKey.fxntstorage.clear_backpack_shape_cache", GLFW.GLFW_KEY_F10, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping ORE_MINE_ANY_BLOCK = new KeyMapping("hotKey.fxntstorage.oremine_any_block", GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping FLY_JETPACK = new KeyMapping("hotKey.fxntstorage.fly_jetpack", GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_FXNTSTORAGE);

    private static boolean flykeyWasDown = false;
    private static boolean minekeyWasDown = false;

    @EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (mc.level == null || player == null) return;

            while (TOGGLE_BACKPACK_KEY.consumeClick()) {
                if (!player.isSpectator())
                    if (BackpackHelper.isWearingBackpack(player)) handleOpenCloseBackpack();
            }

            while (TOGGLE_JETPACK_HOVER_KEY.consumeClick()) {
                if (!player.isCreative() && !player.isSpectator()) {
                    ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
                    IBackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
                    boolean upgradePresent = UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), UpgradeType.FLIGHT);

                    if (upgradePresent) {
                        PacketDistributor.sendToServer(new KeyPressedPacket(Util.TOGGLE_HOVER, true, Optional.empty()));
                    }

                    JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                    if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                        jetpackHandler.toggleHover();
                    }
                }
            }

            while (CLEAR_BACKPACK_SHAPE_CACHE.consumeClick()) {
                BackpackShapeCache.clearCache();
                PasserShapeCache.clearCache();
                player.sendSystemMessage(Component.translatable("fxntstorage.shape_cache.cleared"));
            }

            // === ORE MINING KEY ===
            boolean minekeyIsDown = KeybindHandler.ORE_MINE_ANY_BLOCK.isDown();
            if (minekeyIsDown != minekeyWasDown && !player.isCreative() && !player.isSpectator()) {
                Optional<BlockPos> pos =
                        mc.hitResult instanceof BlockHitResult hit
                                && hit.getType() == HitResult.Type.BLOCK
                                ? Optional.of(hit.getBlockPos())
                                : Optional.empty();

                PacketDistributor.sendToServer(new KeyPressedPacket(Util.MINE_ALL_BLOCKS, minekeyIsDown, pos));
                minekeyWasDown = minekeyIsDown;
            }

            // === FLY JETPACK KEY ===
            boolean flykeyIsDown = KeybindHandler.FLY_JETPACK.isDown();
            boolean shiftIsDown = player.isShiftKeyDown();

            if (flykeyIsDown != flykeyWasDown && !player.isCreative() && !player.isSpectator()) {
                PacketDistributor.sendToServer(new JetpackFlyingPacket(flykeyIsDown, shiftIsDown));
                JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(flykeyIsDown, shiftIsDown);
            }

            flykeyWasDown = flykeyIsDown;
        }
    }

    public static void handleOpenCloseBackpack() {
        PacketDistributor.sendToServer(new KeyPressedPacket(
                (Minecraft.getInstance().screen instanceof BackpackScreen)
                        ? Util.CLOSE_BACKPACK
                        : Util.OPEN_BACKPACK,
                true, Optional.empty())
        );
    }
}
