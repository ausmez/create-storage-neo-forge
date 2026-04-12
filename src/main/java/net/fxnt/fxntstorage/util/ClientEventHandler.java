package net.fxnt.fxntstorage.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.outliner.Outliner;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.ClientJukeboxHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.ISortableStorageBox;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.controller.StorageControllerHighlight;
import net.fxnt.fxntstorage.network.packet.PickBlockUpgradePacket;
import net.fxnt.fxntstorage.network.packet.PlayerInputPacket;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static double lastForwardImpulse = -99;
    private static double lastLeftImpulse = -99;

    private record SlotRange(int start, int end) {
    }

    private static SlotRange getSlotRange(int slotId, int slotCount) {
        if (slotId < slotCount) return new SlotRange(0, slotCount);
        if (slotId < slotCount + 27) return new SlotRange(slotCount, slotCount + 27);
        return new SlotRange(slotCount + 27, slotCount + 36);
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Vec2 movement = event.getInput().getMoveVector();
        float forwardImpulse = movement.y;
        float leftImpulse = movement.x;

        Player player = event.getEntity();

        if (forwardImpulse != lastForwardImpulse || leftImpulse != lastLeftImpulse) {
            lastForwardImpulse = forwardImpulse;
            lastLeftImpulse = leftImpulse;

            if (!BackpackHelper.isWearingBackpack(player)) return;
            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            if (!UpgradeHelper.hasActiveUpgrade(
                    BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack).getItemHandler(),
                    UpgradeType.FLIGHT)) return;

            PacketDistributor.sendToServer(new PlayerInputPacket(forwardImpulse, -leftImpulse));
            JetpackManager.getJetpackHandler(player).processPlayerInputPacket(forwardImpulse, -leftImpulse);
        }
    }

    /* PickBlockMixin */
    @SubscribeEvent
    public static void onMiddleClickBlock(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 2) return; // Only continue if MIDDLE button was clicked

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator() || !player.level().isClientSide ||
                !player.isAlive() || player.isSleeping() || player.isDeadOrDying() || event.getAction() != InputConstants.PRESS)
            return;

        if (mc.screen != null) return; // If player has a screen/menu open, return
        if (!BackpackHelper.isWearingBackpack(player)) return;

        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState state = player.level().getBlockState(blockPos);
            ItemStack stack = state.getCloneItemStack(hitResult, player.level(), blockPos, player);

            if (!stack.isEmpty()) {
                PacketDistributor.sendToServer(new PickBlockUpgradePacket(stack));
            } else {
                FXNTStorage.LOGGER.debug("No valid clone item for block: {}", state.getBlock());
            }
        }

    }

    /* BackpackMenu sort */
    @SubscribeEvent
    public static void onMiddleClickSort(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 2) return; // Only continue if MIDDLE button was clicked

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator() || !player.level().isClientSide || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
            return;

        if (event.getAction() == InputConstants.PRESS &&
                (player.containerMenu instanceof BackpackMenu
                        || player.containerMenu instanceof StorageBoxMenu
                        || player.containerMenu instanceof StorageBoxMountedMenu)) {
            event.setCanceled(true); // Prevent any further processing (might yield undesired results)

            final Screen screen = mc.screen;
            if (!(screen instanceof final AbstractContainerScreen<?> containerScreen && !(screen instanceof CreativeModeInventoryScreen)))
                return;

            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot == null) return;

            // InventorySorter "overrides" for Backpack and StorageBox
            if (player.containerMenu instanceof BackpackMenu menu) {
                BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
                BackpackSlotLayout.SortRange sortRange = layout.getSortRangeForSlot(slot.index);

                if (!sortRange.isValid()) return;

                PacketDistributor.sendToServer(new SortInventoryPacket(
                        Util.INV_TYPE_BACKPACK,
                        sortRange.getStartIndex(),
                        sortRange.getEndIndex(),
                        menu.getSortOrder()
                ));
            }
            if (player.containerMenu instanceof ISortableStorageBox menu) {
                SlotRange range = getSlotRange(slot.index, menu.getContainerSize());
                PacketDistributor.sendToServer(new SortInventoryPacket(
                        Util.INV_TYPE_STORAGE_BOX,
                        range.start,
                        range.end,
                        menu.getSortOrder())
                );
            }
        }
    }

    @SubscribeEvent
    public static void onServerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        LocalPlayer player = Minecraft.getInstance().player;

        ConfigManager.ClientConfig.sendClientSettings();
        ClientJukeboxHandler.init();
        if (player != null)
            JetpackManager.addPlayer(player);
        StorageControllerHighlight.removeAll();
    }

    @SubscribeEvent
    public static void onServerLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        LocalPlayer player = event.getPlayer();
        LocalPlayer localPlayer = Minecraft.getInstance().player;

        if (player != null && localPlayer != null && player.getUUID().equals(localPlayer.getUUID())) {
            ClientSettings.remove(player.getUUID());
            ClientJukeboxHandler.stopAllMusic();
        }
    }

    @SubscribeEvent
    public static void onClientRespawn(ClientPlayerNetworkEvent.Clone event) {
        JetpackManager.addPlayer(event.getNewPlayer());
        KeybindHandler.resetFlyKeyState();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Map<BlockPos, Set<BlockPos>> highlights = StorageControllerHighlight.getAll();
        if (highlights.isEmpty()) return;

        highlights.forEach(ClientEventHandler::renderConnectedBoxes);
    }

    private static void renderConnectedBoxes(BlockPos controllerPos, Set<BlockPos> components) {
        Outliner outliner = Outliner.getInstance();
        Object slot = "network_cluster_" + controllerPos.asLong();

        outliner.showCluster(slot, components)
                .colored(0xddc166)
                .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                .disableLineNormals()
                .lineWidth(1 / 24f);
    }
}
