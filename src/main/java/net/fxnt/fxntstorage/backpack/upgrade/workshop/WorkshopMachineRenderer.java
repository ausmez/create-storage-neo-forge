package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WorkshopMachineRenderer {
    private static final int DEPLOYER_SCALE = 16;
    private static final int PRESS_SCALE = 16;

    private static final int SHADOW_W = 52;
    private static final int SHADOW_H = 11;
    private static final float DEPLOYER_SHADOW_SCALE = DEPLOYER_SCALE / 20f;
    private static final float PRESS_SHADOW_SCALE = PRESS_SCALE / 24f;
    private static final int DEPLOYER_SHADOW_DX = 0;
    private static final int DEPLOYER_SHADOW_DY = 20;
    private static final int PRESS_SHADOW_DX = 16;
    private static final int PRESS_SHADOW_DY = 40;

    private float animTime = 0f;
    private float lastRenderTime = Float.NaN;

    private float displayOffset = 0f;
    private int lastSync = Integer.MIN_VALUE;
    private float prevOffset = 0f;
    private float currOffset = 0f;
    private float baseRenderTime = Float.NaN;

    public void draw(GuiGraphics graphics, int anchorX, int anchorY, boolean deployer, int syncValue) {
        boolean processing = syncValue > 0;
        advanceClock(processing);
        updateOffset(syncValue, processing);

        if (deployer) {
            drawDeployer(graphics, anchorX, anchorY);
        } else {
            drawPress(graphics, anchorX, anchorY);
        }
    }

    private void updateOffset(int syncValue, boolean processing) {
        if (!processing) {
            lastSync = syncValue;
            baseRenderTime = Float.NaN;
            prevOffset = currOffset = displayOffset = 0f;
            return;
        }

        float now = AnimationTickHolder.getRenderTime();
        if (syncValue != lastSync || Float.isNaN(baseRenderTime)) {
            lastSync = syncValue;
            prevOffset = currOffset;
            currOffset = (syncValue - 1) / 1000f;
            baseRenderTime = now;
        }

        float elapsed = Math.clamp(now - baseRenderTime, 0f, 1f);
        displayOffset = Mth.lerp(elapsed, prevOffset, currOffset);
    }

    private void advanceClock(boolean processing) {
        float now = AnimationTickHolder.getRenderTime();
        if (processing && !Float.isNaN(lastRenderTime)) {
            float delta = now - lastRenderTime;
            // Guard against large jumps (pauses, screen reopen) so the animation stays smooth
            if (delta > 0f && delta < 100f) {
                animTime += delta;
            }
        }
        lastRenderTime = now;
    }

    private float shaftAngle() {
        float degPerTick = WorkshopUpgrade.kineticSpeed() * 3f / 10f;
        return (animTime * degPerTick) % 360f;
    }

    private void drawDeployer(GuiGraphics graphics, int anchorX, int anchorY) {
        drawShadow(graphics, anchorX + 13 + DEPLOYER_SHADOW_DX, anchorY + 30 + DEPLOYER_SHADOW_DY, DEPLOYER_SHADOW_SCALE);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX + 3, anchorY + 18, 100);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));
        pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = DEPLOYER_SCALE;

        AnimatedKinetics.defaultBlockElement(shaft(Direction.Axis.Z))
                .rotateBlock(0, 0, shaftAngle())
                .scale(scale)
                .render(graphics);

        AnimatedKinetics.defaultBlockElement(AllBlocks.DEPLOYER.getDefaultState()
                        .setValue(DeployerBlock.FACING, Direction.DOWN)
                        .setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false))
                .scale(scale)
                .render(graphics);

        pose.pushPose();
        pose.translate(0, displayOffset * 17, 0);
        AnimatedKinetics.defaultBlockElement(AllPartialModels.DEPLOYER_POLE)
                .rotateBlock(90, 0, 0)
                .scale(scale)
                .render(graphics);
        AnimatedKinetics.defaultBlockElement(AllPartialModels.DEPLOYER_HAND_HOLDING)
                .rotateBlock(90, 0, 0)
                .scale(scale)
                .render(graphics);
        pose.popPose();

        AnimatedKinetics.defaultBlockElement(AllBlocks.DEPOT.getDefaultState())
                .atLocal(0, 2, 0)
                .scale(scale)
                .render(graphics);

        pose.popPose();
    }

    private void drawPress(GuiGraphics graphics, int anchorX, int anchorY) {
        drawShadow(graphics, anchorX + PRESS_SHADOW_DX, anchorY + PRESS_SHADOW_DY, PRESS_SHADOW_SCALE);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX + 5, anchorY + 24, 200);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));
        pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = PRESS_SCALE;

        AnimatedKinetics.defaultBlockElement(shaft(Direction.Axis.Z))
                .rotateBlock(0, 0, shaftAngle())
                .scale(scale)
                .render(graphics);

        AnimatedKinetics.defaultBlockElement(AllBlocks.MECHANICAL_PRESS.getDefaultState())
                .scale(scale)
                .render(graphics);

        // displayOffset is 0 (up) .. 1 (fully down) - the head model presses down by that amount
        AnimatedKinetics.defaultBlockElement(AllPartialModels.MECHANICAL_PRESS_HEAD)
                .atLocal(0, displayOffset, 0)
                .scale(scale)
                .render(graphics);

        pose.popPose();
    }

    private void drawShadow(GuiGraphics graphics, float centreX, float centreY, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centreX, centreY, 0);
        pose.scale(scale, scale, 1f);
        AllGuiTextures.JEI_SHADOW.render(graphics, -SHADOW_W / 2, -SHADOW_H / 2);
        pose.popPose();
    }

    private static BlockState shaft(Direction.Axis axis) {
        return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
    }
}
