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
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.JetpackFlyingPacket;
import net.fxnt.fxntstorage.network.packet.KeyPressedPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    public static final String KEY_CATEGORY_FXNTSTORAGE = "key.fxntstorage.category";

    public static final KeyMapping TOGGLE_BACKPACK_KEY = new KeyMapping("key.fxntstorage.toggle_backpack", GLFW.GLFW_KEY_B, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping TOGGLE_JETPACK_HOVER_KEY = new KeyMapping("key.fxntstorage.toggle_jetpack_hover", GLFW.GLFW_KEY_H, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping ORE_MINE_ANY_BLOCK = new KeyMapping("key.fxntstorage.oremine_any_block", GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping FLY_JETPACK = new KeyMapping("key.fxntstorage.fly_jetpack", GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_FXNTSTORAGE);

    private static boolean flykeyWasDown = false;
    private static boolean minekeyWasDown = false;

    public static void resetFlyKeyState() {
        flykeyWasDown = false;
    }

    @Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(TickEvent.@NotNull ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                if (mc.level == null || player == null) return;

                boolean isSurvival = !player.isCreative() && !player.isSpectator();
                boolean isWearingBackpack = BackpackHelper.isWearingBackpack(player);
                ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
                IBackpackContainer backpackContainer = isWearingBackpack
                        ? new BackpackContainer(player, backpack)
                        : null;

                while (TOGGLE_BACKPACK_KEY.consumeClick()) {
                    if (!player.isSpectator() && isWearingBackpack) handleOpenCloseBackpack();
                }

                while (TOGGLE_JETPACK_HOVER_KEY.consumeClick()) {
                    if (!isSurvival || !isWearingBackpack) break;
                    if (!UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.FLIGHT)) break;

                    ModNetwork.sendToServer(new KeyPressedPacket(Util.TOGGLE_HOVER, true, null));

                    JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                    if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                        jetpackHandler.toggleHover();
                    }
                }

                // === ORE MINING KEY ===
                boolean minekeyIsDown = ORE_MINE_ANY_BLOCK.isDown();

                if (minekeyIsDown != minekeyWasDown && isSurvival && isWearingBackpack && isToolItem(player.getMainHandItem())
                        && UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.OREMINING)) {
                    BlockPos pos = null;
                    if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType().equals(HitResult.Type.BLOCK)) {
                        pos = blockHit.getBlockPos();
                    }
                    ModNetwork.sendToServer(new KeyPressedPacket(Util.MINE_ALL_BLOCKS, minekeyIsDown, pos));
                }
                minekeyWasDown = minekeyIsDown;

                // === FLY JETPACK KEY ===
                boolean flykeyIsDown = FLY_JETPACK.isDown();
                boolean shiftIsDown = player.isShiftKeyDown();

                if (flykeyIsDown != flykeyWasDown && isSurvival && isWearingBackpack
                        && UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.FLIGHT)) {
                    ModNetwork.sendToServer(new JetpackFlyingPacket(flykeyIsDown, shiftIsDown));
                    JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(flykeyIsDown, shiftIsDown);
                }

                flykeyWasDown = flykeyIsDown;
            }
        }
    }

    private static boolean isToolItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.PICKAXES)
                || itemStack.is(ItemTags.AXES)
                || itemStack.is(ItemTags.SHOVELS)
                || itemStack.is(ItemTags.HOES)
                || itemStack.is(Tags.Items.SHEARS);
    }

    public static void handleOpenCloseBackpack() {
        ModNetwork.sendToServer(new KeyPressedPacket(
                (Minecraft.getInstance().screen instanceof BackpackScreen)
                        ? Util.CLOSE_BACKPACK
                        : Util.OPEN_BACKPACK,
                true, null)
        );
    }

}
