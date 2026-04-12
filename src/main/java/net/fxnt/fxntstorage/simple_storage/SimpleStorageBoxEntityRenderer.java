package net.fxnt.fxntstorage.simple_storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
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

public class SimpleStorageBoxEntityRenderer implements BlockEntityRenderer<SimpleStorageBoxEntity> {

    protected final BlockEntityRendererProvider.Context context;

    public SimpleStorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockState state = context.state;
        CompoundTag tag = context.blockEntityData;
        if (tag == null || state == null) return;

        boolean hasVoidUpgrade = tag.getBoolean("VoidUpgrade");

        int amount = tag.getInt("StoredAmount");
        int totalSpace = tag.getInt("MaxItemCapacity");
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

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

        ItemRenderer itemRenderer = mc.getItemRenderer();
        int color = getColorForDistance(distance);

        BlockPos lightPos = context.contraption.entity.blockPosition().offset(context.localPos).relative(side);

        int blockLight = context.world.getBrightness(LightLayer.BLOCK, lightPos);
        int skyLight = context.world.getBrightness(LightLayer.SKY, lightPos);
        int lightLevel = LightTexture.pack(blockLight, skyLight);

        renderLine(line1, -1f, poseStack, buffer, color, lightLevel);
        renderLine(line2, -4f, poseStack, buffer, color, lightLevel);

        if (tag.contains("FilterItem")) {
            ItemStack filterItem = tag.getCompound("FilterItem").isEmpty()
                    ? ItemStack.EMPTY
                    : ItemStack.of(tag.getCompound("FilterItem"));
            if (!filterItem.isEmpty() || hasVoidUpgrade) {
                renderItem(itemRenderer, filterItem, poseStack, buffer, lightLevel, hasVoidUpgrade);
            }
        }

        poseStack.popPose();
    }

    @Override
    public void render(SimpleStorageBoxEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;

        boolean isPonderScene = currentScreen instanceof AbstractSimiScreen;

        Player player = mc.player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int amount = blockEntity.getStoredAmount();
        int totalSpace = blockEntity.getMaxItemCapacity();
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));

        if (distance > getMaxDistance() && !isPonderScene) return;
        if (isPonderScene) distance = 3f;

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        int color = getColorForDistance(distance);
        int lightLevel = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());

        renderLine(line1, -1f, poseStack, buffer, color, lightLevel);
        renderLine(line2, -4f, poseStack, buffer, color, lightLevel);

        ItemStack filterItem = blockEntity.getFilterItem();
        if (!filterItem.isEmpty() || blockEntity.voidUpgrade) {
            renderItem(Minecraft.getInstance().getItemRenderer(), filterItem, poseStack, buffer, lightLevel, blockEntity.voidUpgrade);
        }

        poseStack.popPose();
    }
}
