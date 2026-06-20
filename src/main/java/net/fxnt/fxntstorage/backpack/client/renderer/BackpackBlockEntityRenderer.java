package net.fxnt.fxntstorage.backpack.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelPlacement;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelRenderer;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BackpackBlockEntityRenderer implements BlockEntityRenderer<BackpackEntity> {

    public BackpackBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BackpackEntity backpack, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!ConfigManager.ClientConfig.WORKSHOP_FLYWHEEL_VISUALS.get()) return;
        if (!UpgradeHelper.hasActiveUpgrade(backpack.getItemHandler(), UpgradeType.WORKSHOP)) return;

        float angle = backpack.advanceClientFlywheelAngle(
                WorkshopFlywheelRenderer.spinDegPerTick(backpack.isWorkshopProcessing()));

        Direction facing = backpack.getBlockState().getValue(BackpackBlock.FACING);
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        WorkshopFlywheelRenderer.renderPair(poseStack, consumer, packedLight, angle,
                WorkshopFlywheelPlacement.BLOCK_OFFSET_X, WorkshopFlywheelPlacement.BLOCK_OFFSET_Y,
                WorkshopFlywheelPlacement.BLOCK_OFFSET_Z, WorkshopFlywheelPlacement.BLOCK_SCALE);
        poseStack.popPose();
    }
}
