package net.fxnt.fxntstorage.simple_storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class SimpleStorageBoxEntityRenderer implements BlockEntityRenderer<SimpleStorageBoxEntity> {
    private final BlockEntityRendererProvider.Context context;
    private static final int TEXT_COLOR_TRANSPARENT = FastColor.ARGB32.color(0, 255, 255, 255);

    public SimpleStorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockState state = context.state;
        CompoundTag tag = context.blockEntityData;
        if (tag == null || state == null) return;

        int amount = tag.getInt("StoredAmount");
        int totalSpace = tag.getInt("MaxItemCapacity");
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

        Direction side = state.getValue(HorizontalDirectionalBlock.FACING);

        PoseStack poseStack = matrices.getModelViewProjection();

        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5f, context.localPos.getY() + 0.5f, context.localPos.getZ() + 0.5f);
        poseStack.mulPose(Axis.YP.rotation(getRotationYForSide2D(side)));
        poseStack.translate(-0.5f, 0, -0.5f);

        float zOffset = 15.05f / 16f;
        int packedLight = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;

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
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        renderLineStatic(font, line1, -1f / 16f, zOffset, packedLight, poseStack, buffer, alpha);
        renderLineStatic(font, line2, -4f / 16f, zOffset, packedLight, poseStack, buffer, alpha);

        if (tag.contains("FilterItem")) {
            ItemStack filterItem = ItemStack.of(tag.getCompound("FilterItem"));
            if (!filterItem.isEmpty()) {
                renderItemStatic(itemRenderer, filterItem, zOffset, poseStack, buffer, packedLight, overlay);
            }
        }

        if (tag.getBoolean("VoidUpgrade")) {
            ItemStack voidIcon = new ItemStack(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
            renderItemStatic(itemRenderer, voidIcon, zOffset, poseStack, buffer, packedLight, overlay, 0.8f, 0.3f, 0.25f);
        }

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

    private static void renderItemStatic(ItemRenderer itemRenderer, ItemStack stack, float zOffset, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel modelWithOverrides = itemRenderer.getModel(stack, null, null, 0);
        boolean flatItem = !modelWithOverrides.isGui3d();

        renderItemStatic(itemRenderer, stack, zOffset, poseStack, buffer, packedLight, packedOverlay, 0.5f, 0.175f, flatItem ? 0.25f : 0.5f);
    }

    private static void renderItemStatic(ItemRenderer itemRenderer, ItemStack stack, float zOffset, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float x, float y, float scale) {
        poseStack.pushPose();
        poseStack.translate(x, y, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.scale(scale + 1 / 64f, scale + 1 / 64f, scale + 1 / 64f);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, null, 0);
        poseStack.popPose();
    }

    @Override
    public void render(@NotNull SimpleStorageBoxEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        final int MAX_DISTANCE = 10;
        boolean isPonderScene;

        Screen currentScreen = Minecraft.getInstance().screen;
        isPonderScene = currentScreen instanceof AbstractSimiScreen;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int amount = blockEntity.getStoredAmount();
        int totalSpace = blockEntity.getMaxItemCapacity();
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));
        float alpha = Math.max(1f - ((distance) / MAX_DISTANCE), 0.05f);

        if (distance > MAX_DISTANCE && !isPonderScene) return;
        if (isPonderScene) alpha = 0.75F;

        float Line1Offset = -1f / 16f;
        float Line2Offset = -4f / 16f;

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();

        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(getRotationYForSide2D(side), 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);

        // Adjust position to render on the block face
        float zOffset = 15.05f / 16f;

        packedLight = 255;

        renderLine(line1, Line1Offset, zOffset, packedLight, poseStack, buffer, alpha);
        renderLine(line2, Line2Offset, zOffset, packedLight, poseStack, buffer, alpha);

        ItemStack filterItem = blockEntity.getFilterItem();
        if (!filterItem.isEmpty()) {
            renderItem(filterItem, zOffset, poseStack, buffer, packedLight, packedOverlay);
        }

        if (blockEntity.voidUpgrade) {
            renderVoidIcon(zOffset, poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderLine(String text, float yOffset, float zOffset, int packedLight, @NotNull PoseStack poseStack, MultiBufferSource buffer, float alpha) {
        Font textRenderer = this.context.getFont();
        int textWidth = textRenderer.width(text);

        poseStack.pushPose();
        // Adjust position to render on the block face
        poseStack.translate(0.5f, yOffset, zOffset);
        // Flip Text Upside Down & Shrink
        poseStack.scale(1 / 64f, -1 / 64f, 1f);

        int color = (int) (255 * alpha) << 24 | TEXT_COLOR_TRANSPARENT;
        float x = (float) -textWidth / 2;
        float y = 0;
        boolean dropShadow = false;
        Matrix4f matrix = poseStack.last().pose();
        Font.DisplayMode displayMode = Font.DisplayMode.NORMAL;
        int backgroundColor = 0;

        textRenderer.drawInBatch(text, x, y, color, dropShadow, matrix, buffer, displayMode, backgroundColor, packedLight);
        poseStack.popPose();
    }

    private void renderItem(ItemStack filter, float zOffset, @NotNull PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel modelWithOverrides = itemRenderer.getModel(filter, null, null, 0);
        boolean flatItem = !modelWithOverrides.isGui3d();

        zOffset += flatItem ? 0 : 0f;
        poseStack.translate(0.5f, 0.175f, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        float scale = (flatItem ? 0.25f : 0.5f) + 1 / 64f;
        poseStack.scale(scale, scale, scale);

        itemRenderer.renderStatic(filter, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, mc.level, 0);
        poseStack.popPose();
    }

    private void renderVoidIcon(float zOffset, @NotNull PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        ItemStack icon = new ItemStack(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        poseStack.translate(0.8f, 0.3f, zOffset);

        float scale = 0.25f + (1 / 64f);
        poseStack.scale(scale, scale, scale);

        itemRenderer.renderStatic(icon, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, mc.level, 0);
        poseStack.popPose();
    }

}
