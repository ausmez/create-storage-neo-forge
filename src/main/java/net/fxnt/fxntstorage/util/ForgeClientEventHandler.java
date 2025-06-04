package net.fxnt.fxntstorage.util;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.util.StorageBoxNetworkHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {
    private static double lastforwardImpulse = -99;
    private static double lastleftImpulse = -99;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Vec2 movement = event.getInput().getMoveVector();
        float forwardImpulse = movement.y;
        float leftImpulse = movement.x;

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putFloat("JetpackForward", forwardImpulse);
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putFloat("JetpackLeft", -leftImpulse);
        }

        if (forwardImpulse != lastforwardImpulse || leftImpulse != lastleftImpulse) {
            ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.PLAYER_INPUT, new FriendlyByteBuf(
                    Unpooled.buffer()
                            .writeFloat(forwardImpulse)
                            .writeFloat(-leftImpulse) // Need to invert for some reason
            )));
            lastforwardImpulse = forwardImpulse;
            lastleftImpulse = leftImpulse;
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

            BackpackNetworkHelper.doPickBlock(stack);
        }
    }

    /* BackpackMenu sort */
    @SubscribeEvent
    public static void onMiddleClickSort(InputEvent.MouseButton event) {
        if (event.getButton() != 2) return; // Only continue if MIDDLE button was clicked

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator() || !player.level().isClientSide || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
            return;

        if (event.getAction() == InputConstants.PRESS &&
                (player.containerMenu instanceof BackpackMenu || player.containerMenu instanceof StorageBoxMenu)) {
            event.setCanceled(true); // Prevent any further processing (might yield undesired results with other mods)

            final Screen screen = mc.screen;
            if (!(screen instanceof final AbstractContainerScreen<?> containerScreen && !(screen instanceof CreativeModeInventoryScreen)))
                return;

            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot == null) return;

            // InventorySorter "overrides" for Backpack and StorageBox
            if (player.containerMenu instanceof BackpackMenu menu)
                BackpackNetworkHelper.sortBackpack(slot.index, menu.getSortOrder());
            if (player.containerMenu instanceof StorageBoxMenu menu)
                StorageBoxNetworkHelper.sortStorageBox(slot.index, menu.getContainerSize(), menu.getSortOrder());
        }
    }

    @SubscribeEvent
    public static void onServerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            BackpackNetworkHelper.sendClientSettings();
        }
    }

}
