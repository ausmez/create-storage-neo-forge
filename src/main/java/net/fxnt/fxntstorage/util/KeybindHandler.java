package net.fxnt.fxntstorage.util;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.network.packet.JetpackFlyingPacket;
import net.fxnt.fxntstorage.network.packet.KeyPressedPacket;
import net.fxnt.fxntstorage.simple_storage.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Optional;

public class KeybindHandler {

    public static final String KEY_CATEGORY_FXNTSTORAGE = "hotKey.fxntstorage.category";

    public static final KeyMapping TOGGLE_BACKPACK_KEY = new KeyMapping("hotKey.fxntstorage.toggle_backpack", GLFW.GLFW_KEY_B, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping TOGGLE_JETPACK_HOVER_KEY = new KeyMapping("hotKey.fxntstorage.toggle_jetpack_hover", GLFW.GLFW_KEY_H, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping ORE_MINE_ANY_BLOCK = new KeyMapping("hotKey.fxntstorage.oremine_any_block", GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping FLY_JETPACK = new KeyMapping("hotKey.fxntstorage.fly_jetpack", GLFW.GLFW_KEY_SPACE, KEY_CATEGORY_FXNTSTORAGE);
    public static final KeyMapping COMPACTING_WHEEL_KEY = new KeyMapping("hotKey.fxntstorage.compacting_wheel", GLFW.GLFW_KEY_TAB, KEY_CATEGORY_FXNTSTORAGE);

    private static boolean flykeyWasDown = false;
    private static boolean minekeyWasDown = false;
    private static boolean minePreviewActive = false;

    public static void resetFlyKeyState() {
        flykeyWasDown = false;
    }

    @EventBusSubscriber(modid = FXNTStorage.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (mc.level == null || player == null) return;

            boolean isSurvival = !player.isCreative() && !player.isSpectator();
            boolean isWearingBackpack = BackpackHelper.isWearingBackpack(player);
            IBackpackContainer backpackContainer = isWearingBackpack
                    ? BackpackContainer.Cache.getOrCreateWornBackpack(player, BackpackHelper.getEquippedBackpackStack(player))
                    : null;

            while (TOGGLE_BACKPACK_KEY.consumeClick()) {
                if (!player.isSpectator() && isWearingBackpack) handleOpenCloseBackpack();
            }

            while (COMPACTING_WHEEL_KEY.consumeClick()) {
                if (!player.isSpectator() && mc.screen == null) openCompactingWheel(mc);
            }

            while (TOGGLE_JETPACK_HOVER_KEY.consumeClick()) {
                if (!isSurvival || !isWearingBackpack) break;
                if (!UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.FLIGHT)) break;

                PacketDistributor.sendToServer(new KeyPressedPacket(Util.TOGGLE_HOVER, true, Optional.empty()));

                JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                    jetpackHandler.toggleHover();
                }
            }

            // === ORE MINING KEY ===
            boolean minekeyIsDown = ORE_MINE_ANY_BLOCK.isDown();
            boolean hasTool = isToolItem(player.getMainHandItem());
            boolean hasUpgrade = isSurvival && isWearingBackpack
                    && UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.OREMINING);

            if (hasUpgrade) {
                if (!minePreviewActive && minekeyIsDown && !minekeyWasDown && hasTool) {
                    // Key just pressed with a tool — activate preview
                    Optional<BlockPos> pos =
                            mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                                    ? Optional.of(hit.getBlockPos())
                                    : Optional.empty();
                    PacketDistributor.sendToServer(new KeyPressedPacket(Util.MINE_ALL_BLOCKS, true, pos));
                    minePreviewActive = true;
                } else if (minePreviewActive && (!minekeyIsDown || !hasTool)) {
                    // Key released, or main hand switched to a non-tool while key held — deactivate preview
                    PacketDistributor.sendToServer(new KeyPressedPacket(Util.MINE_ALL_BLOCKS, false, Optional.empty()));
                    minePreviewActive = false;
                }
            } else if (minePreviewActive) {
                // Lost upgrade or conditions while preview was active
                PacketDistributor.sendToServer(new KeyPressedPacket(Util.MINE_ALL_BLOCKS, false, Optional.empty()));
                minePreviewActive = false;
            }
            minekeyWasDown = minekeyIsDown;

            // === FLY JETPACK KEY ===
            boolean flykeyIsDown = FLY_JETPACK.isDown();
            boolean shiftIsDown = player.isShiftKeyDown();

            if (flykeyIsDown != flykeyWasDown && isSurvival && isWearingBackpack
                    && UpgradeHelper.hasActiveUpgrade(backpackContainer.getItemHandler(), UpgradeType.FLIGHT)) {
                PacketDistributor.sendToServer(new JetpackFlyingPacket(flykeyIsDown, shiftIsDown));
                JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(flykeyIsDown, shiftIsDown);
            }

            flykeyWasDown = flykeyIsDown;
        }
    }

    private static boolean isToolItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.PICKAXES)
                || itemStack.is(ItemTags.AXES)
                || itemStack.is(ItemTags.SHOVELS)
                || itemStack.is(ItemTags.HOES)
                || itemStack.is(Tags.Items.TOOLS_SHEAR);
    }

    public static void handleOpenCloseBackpack() {
        PacketDistributor.sendToServer(new KeyPressedPacket(
                (Minecraft.getInstance().screen instanceof BackpackScreen)
                        ? Util.CLOSE_BACKPACK
                        : Util.OPEN_BACKPACK,
                true, Optional.empty())
        );
    }

    private static void openCompactingWheel(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;

        // Static block entity case
        if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            var state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof SimpleStorageBox) {
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                if (blockHit.getDirection() == facing) {
                    var be = mc.level.getBlockEntity(pos);
                    if (be instanceof SimpleStorageBoxEntity ssbe && ssbe.compactingUpgrade && ssbe.compactingChain != null) {
                        mc.setScreen(new CompactingWheelScreen(pos, ssbe.compactingChain,
                                ssbe.getStoredAmount(), ssbe.compactingSelectedTier));
                        return;
                    }
                }
            }
        }

        // Mounted case
        openMountedCompactingWheel(mc);
    }

    private static void openMountedCompactingWheel(Minecraft mc) {
        var rayInputs = ContraptionHandlerClient.getRayInputs(mc.player);
        Vec3 origin = rayInputs.getFirst();
        Vec3 target = rayInputs.getSecond();
        AABB aabb = new AABB(origin, target).inflate(16);

        Collection<WeakReference<AbstractContraptionEntity>> contraptions =
                ContraptionHandler.loadedContraptions.get(mc.level).values();

        for (WeakReference<AbstractContraptionEntity> ref : contraptions) {
            AbstractContraptionEntity contraptionEntity = ref.get();
            if (contraptionEntity == null) continue;
            if (!contraptionEntity.getBoundingBox().intersects(aabb)) continue;

            BlockHitResult rayResult = ContraptionHandlerClient.rayTraceContraption(origin, target, contraptionEntity);
            if (rayResult == null) continue;

            BlockPos localPos = rayResult.getBlockPos();
            var blockInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
            if (blockInfo == null || !(blockInfo.state().getBlock() instanceof SimpleStorageBox)) continue;

            Direction facing = blockInfo.state().getValue(HorizontalDirectionalBlock.FACING);
            if (rayResult.getDirection() != facing) continue;

            CompoundTag tag = blockInfo.nbt();
            if (tag == null || !tag.getBoolean("CompactingUpgrade")) continue;

            ItemStack filterItem = tag.contains("FilterItem")
                    ? ItemStack.parseOptional(mc.level.registryAccess(), tag.getCompound("FilterItem"))
                    : ItemStack.EMPTY;
            if (filterItem.isEmpty()) continue;

            if (CompactingRecipeHelper.isEmpty()) {
                CompactingRecipeHelper.rebuild(mc.level.getRecipeManager(), mc.level.registryAccess());
            }
            CompactingChain chain = CompactingRecipeHelper.buildChain(filterItem.getItem());
            if (chain == null) continue;

            int t0Stored = tag.getInt("StoredAmount");
            int currentTier = tag.getInt("CompactingSelectedTier");
            mc.setScreen(new CompactingWheelScreen(contraptionEntity.getId(), localPos, chain, t0Stored, currentTier));
            return;
        }
    }
}
