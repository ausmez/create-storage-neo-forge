package net.fxnt.fxntstorage.reserve_storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.utility.DyeHelper;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.fxnt.fxntstorage.compat.sable.SableCompat;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.GHOST_SLOTS;
import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.STORAGE_SLOTS;
import static net.fxnt.fxntstorage.util.RendererHelper.*;

public class ReserveStorageBoxEntityRenderer implements BlockEntityRenderer<ReserveStorageBoxEntity> {
    private static final float SLOT_INDICATOR_SCALE = 0.70f;

    private static final int COLOR_MET = DyeHelper.getDyeColors(DyeColor.GREEN).getFirst() & 0xFFFFFF;
    private static final int COLOR_PARTIAL = DyeHelper.getDyeColors(DyeColor.YELLOW).getFirst() & 0xFFFFFF;
    private static final int COLOR_UNMET = DyeHelper.getDyeColors(DyeColor.RED).getFirst() & 0xFFFFFF;
    private static final int COLOR_EMPTY = DyeHelper.getDyeColors(DyeColor.GRAY).getFirst() & 0xFFFFFF;

    public ReserveStorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ReserveStorageBoxEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        boolean isPonderScene = currentScreen instanceof AbstractSimiScreen;
        boolean inSubLevel = SableCompat.isInPlotGrid(blockEntity);

        Player player = mc.player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        char[] slotStatus = new char[GHOST_SLOTS];

        for (int i = 0; i < GHOST_SLOTS; i++) {
            ItemStack ghost = blockEntity.getItem(STORAGE_SLOTS + i);
            if (ghost.isEmpty()) {
                slotStatus[i] = 'E';
                continue;
            }
            int required = ghost.getCount();
            int stored = 0;
            for (int j = 0; j < STORAGE_SLOTS; j++) {
                ItemStack slot = blockEntity.getItem(j);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, ghost)) {
                    stored += slot.getCount();
                }
            }
            slotStatus[i] = stored >= required ? 'F' : stored > 0 ? 'P' : 'U';
        }

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));
        if (distance > getMaxDistance() && !isPonderScene && !inSubLevel) return;
        if (isPonderScene || inSubLevel) distance = 5f;

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        int envLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
        int textLight = emissiveLight(envLight);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        int color = getColorForDistance(distance);

        renderLine(buildSlotIndicator(slotStatus), 12f, poseStack, bufferSource, textLight, SLOT_INDICATOR_SCALE);
        renderLine(Util.formatNumber(blockEntity.getCurrentValue()), 7f, poseStack, bufferSource, color, textLight);
        renderLine(blockEntity.getPercentageUsed() + "% Used", 4f, poseStack, bufferSource, color, textLight);

        poseStack.popPose();
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        CompoundTag tag = context.blockEntityData;
        BlockState state = context.state;
        if (tag == null || state == null) return;

        Direction side = state.getValue(HorizontalDirectionalBlock.FACING);

        PoseStack poseStack = matrices.getModelViewProjection();
        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5f, context.localPos.getY() + 0.5f, context.localPos.getZ() + 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            poseStack.popPose();
            return;
        }

        double distance = context.position != null
                ? Math.sqrt(player.distanceToSqr(context.position))
                : Math.sqrt(player.distanceToSqr(context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1f)));

        if (distance >= getMaxDistance()) {
            poseStack.popPose();
            return;
        }

        BlockPos lightPos = context.contraption.entity.blockPosition().offset(context.localPos).relative(side);
        int envLight = LevelRenderer.getLightColor(context.world, lightPos);
        int textLight = emissiveLight(envLight);
        int color = getColorForDistance(distance);

        String slotStatus = tag.getString("ReserveSlotStatus");
        if (slotStatus.length() == GHOST_SLOTS) {
            renderLine(buildSlotIndicator(slotStatus.toCharArray()), 4f, poseStack, buffer, textLight, SLOT_INDICATOR_SCALE);
        }

        renderLine(Util.formatNumber(tag.getInt("StoredAmount")), -1f, poseStack, buffer, color, textLight);
        renderLine(Math.round(tag.getFloat("PercentageUsed")) + "% Used", -4f, poseStack, buffer, color, textLight);

        poseStack.popPose();
    }

    private static Component buildSlotIndicator(char[] status) {
        MutableComponent result = Component.empty();
        for (char c : status) {
            int rgb = switch (c) {
                case 'F' -> COLOR_MET;
                case 'P' -> COLOR_PARTIAL;
                case 'U' -> COLOR_UNMET;
                default -> COLOR_EMPTY;
            };
            String glyph = (c == 'F') ? "▒" : (c == 'E') ? "░" : "▒";
            result.append(Component.literal(glyph)
                    .withStyle(style -> style.withColor(TextColor.fromRgb(rgb))));
        }
        return result;
    }
}
