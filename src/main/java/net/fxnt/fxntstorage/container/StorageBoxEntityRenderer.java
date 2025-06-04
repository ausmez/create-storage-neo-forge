package net.fxnt.fxntstorage.container;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class StorageBoxEntityRenderer extends SmartBlockEntityRenderer<StorageBoxEntity> {
    private final BlockEntityRendererProvider.Context context;
    private static final int TEXT_COLOR_TRANSPARENT = FastColor.ARGB32.color(0, 255, 255, 255);

    public StorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.context = context;
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockState state = context.state;
        CompoundTag tag = context.blockEntityData;
        if (tag == null || state == null) return;

        int amount = tag.getInt("StoredAmount");
        int percentUsed = tag.getInt("PercentageUsed");

        String line1 = Util.formatNumber(amount);
        String line2 = tag.getBoolean("VoidUpgrade") ? "Void Mode" : percentUsed + "% Used";

        Direction side = state.getValue(HorizontalDirectionalBlock.FACING);

        PoseStack poseStack = matrices.getModelViewProjection();

        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5f, context.localPos.getY() + 0.5f, context.localPos.getZ() + 0.5f);
        poseStack.mulPose(Axis.YP.rotation(getRotationYForSide2D(side)));
        poseStack.translate(-0.5f, 0, -0.5f);

        float zOffset = 15.05f / 16f;
        int packedLight = 0xF000F0;

        LocalPlayer player = Minecraft.getInstance().player;
        double distance = player != null && context.position != null ? player.distanceToSqr(context.position) : 0;

        float fadeDistance = 256f;
        float baseAlpha = 0.9f;
        float fadeFactor = 1.0f - Mth.clamp((float) distance / fadeDistance, 0f, baseAlpha);
        float alpha = baseAlpha * fadeFactor;

        if (alpha <= 0.01f) {
            poseStack.popPose();
            return;
        }

        Font font = Minecraft.getInstance().font;

        renderLineStatic(font, line1, -1f / 16f, zOffset, packedLight, poseStack, buffer, alpha);
        renderLineStatic(font, line2, -4f / 16f, zOffset, packedLight, poseStack, buffer, alpha);

        poseStack.popPose();
    }

    private static float getRotationYForSide2D(Direction side) {
        float[] sideRotationY2D = {0, 0, 2, 0, 3, 1};
        return sideRotationY2D[side.ordinal()] * 90 * (float) Math.PI / 180f;
    }

    private static void renderLineStatic(Font font, String text, float yOffset, float zOffset, int packedLight, PoseStack poseStack, MultiBufferSource buffer, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.5f, yOffset, zOffset);
        poseStack.scale(1 / 64f, -1 / 64f, 1f);

        int color = (int) (255 * alpha) << 24 | FastColor.ARGB32.color(0, (int) (255 * alpha), (int) (255 * alpha), (int) (255 * alpha));
        float x = (float) -font.width(text) / 2;
        Matrix4f matrix = poseStack.last().pose();

        font.drawInBatch(text, x, 0, color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    protected void renderSafe(StorageBoxEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        final int MAX_DISTANCE = 10;
        boolean isPonderScene;

        Screen currentScreen = Minecraft.getInstance().screen;
        isPonderScene = currentScreen instanceof AbstractSimiScreen;

        FilteringRenderer.renderOnBlockEntity(blockEntity, partialTick, poseStack, buffer, 255, packedOverlay);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int amount = blockEntity.getStoredAmount();

        String line1 = Util.formatNumber(amount);
        String line2 = blockEntity.voidUpgrade ? "Void Mode" : blockEntity.getPercentageUsed() + "% Used";

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));
        float alpha = Math.max(1f - ((distance) / MAX_DISTANCE), 0.05f);

        if (distance > MAX_DISTANCE && !isPonderScene) return;
        if (isPonderScene) alpha = 0.75F;

        float Line1Offset = 7f;
        float Line2Offset = 4f;

        renderLine(line1, Line1Offset, blockEntity, partialTick, poseStack, buffer, packedLight, alpha);
        renderLine(line2, Line2Offset, blockEntity, partialTick, poseStack, buffer, packedLight, alpha);
    }

    private void renderLine(String text, float YOffset, StorageBoxEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha) {
        Font textRenderer = this.context.getFont();
        int textWidth = textRenderer.width(text);

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();

        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(getRotationYForSide2D(side), 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);

        // Adjust position to render on the block face
        float ZOffset = 15.05f;
        poseStack.translate(0.5f, YOffset / 16f, ZOffset / 16f);  // Adjust these values as needed

        // Flip Text Upside Down & Shrink
        poseStack.scale(1 / 64f, -1 / 64f, 1f);

        int color = (int) (255 * alpha) << 24 | TEXT_COLOR_TRANSPARENT;
        float x = (float) -textWidth / 2;
        float y = 0;
        boolean dropShadow = false;
        Matrix4f matrix = poseStack.last().pose();
        Font.DisplayMode displayMode = Font.DisplayMode.NORMAL;
        int backgroundColor = 0;
        packedLight = 250;

        textRenderer.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, displayMode, backgroundColor, packedLight);

        poseStack.popPose();
    }

}
