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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.util.RendererHelper.*;

public class StorageBoxEntityRenderer extends SmartBlockEntityRenderer<StorageBoxEntity> {

    public StorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockState state = context.state;
        CompoundTag tag = context.blockEntityData;
        if (tag == null || state == null) return;

        int amount = tag.getInt("StoredAmount");
        int percentUsed = Math.round(tag.getFloat("PercentageUsed"));

        String line1 = Util.formatNumber(amount);
        String line2 = tag.getBoolean("VoidUpgrade") ? "Void Mode" : percentUsed + "% Used";

        Direction side = state.getValue(HorizontalDirectionalBlock.FACING);

        PoseStack poseStack = matrices.getModelViewProjection();

        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5f, context.localPos.getY() + 0.5f, context.localPos.getZ() + 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        double distance = context.position != null
                ? Math.sqrt(player.distanceToSqr(context.position))
                : Math.sqrt(player.distanceToSqr(context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1f)));

        if (distance >= getMaxDistance()) {
            poseStack.popPose();
            return;
        }

        int color = getColorForDistance(distance);

        BlockPos lightPos = context.contraption.entity.blockPosition().offset(context.localPos).relative(side);

        int blockLight = context.world.getBrightness(LightLayer.BLOCK, lightPos);
        int skyLight = context.world.getBrightness(LightLayer.SKY, lightPos);
        int lightLevel = LightTexture.pack(blockLight, skyLight);

        renderLine(line1, -1f, poseStack, buffer, color, lightLevel);
        renderLine(line2, -4f, poseStack, buffer, color, lightLevel);

        if (tag.contains("Filter")) {
            ItemStack filterItem = tag.getCompound("Filter").isEmpty()
                    ? ItemStack.EMPTY
                    : ItemStack.of(tag.getCompound("Filter"));
            if (!filterItem.isEmpty()) {
                renderItem(mc.getItemRenderer(), filterItem, poseStack, buffer, lightLevel, false);
            }
        }

        poseStack.popPose();
    }

    @Override
    protected void renderSafe(StorageBoxEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean isPonderScene;

        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        isPonderScene = currentScreen instanceof AbstractSimiScreen;

        Player player = mc.player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int amount = blockEntity.getStoredAmount();

        String line1 = Util.formatNumber(amount);
        String line2 = blockEntity.voidUpgrade ? "Void Mode" : blockEntity.getPercentageUsed() + "% Used";

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));

        if (distance > getMaxDistance() && !isPonderScene) return;
        if (isPonderScene) distance = 3f;

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        int lightLevel = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());

        FilteringRenderer.renderOnBlockEntity(blockEntity, partialTick, poseStack, buffer, lightLevel, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        int color = getColorForDistance(distance);

        renderLine(line1, 7f, poseStack, buffer, color, lightLevel);
        renderLine(line2, 4f, poseStack, buffer, color, lightLevel);

        poseStack.popPose();
    }
}
