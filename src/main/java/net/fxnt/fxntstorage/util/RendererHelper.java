package net.fxnt.fxntstorage.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.fxnt.fxntstorage.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;

public class RendererHelper {
    // Use Create config for max render distance - read at call time so config changes take effect without restart
    public static double getMaxDistance() { return AllConfigs.client().filterItemRenderDistance.get(); }

    public static void renderLine(String text, float yOffset, PoseStack poseStack, MultiBufferSource buffer, int color, int packedLight) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(0f, yOffset / 16f, 0f);
        poseStack.scale(1 / 64f, -1 / 64f, 1f);

        float x = -font.width(text) / 2f;

        font.drawInBatch(text, x, 0, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    public static void renderItem(ItemRenderer itemRenderer, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean voidUpgrade) {
        Level level = Minecraft.getInstance().level;
        poseStack.pushPose();
        poseStack.translate(0f, 0.175f, 0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        BakedModel model = itemRenderer.getModel(stack, null, null, 0);
        boolean flatItem = !model.isGui3d();

        float scale = (flatItem ? 0.25f : 0.50f) + 0.01f;
        float zOffset = (flatItem ? -.025f : .068f) + customZOffset(stack.getItem());

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0, zOffset);

        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, 0);
        poseStack.popPose();

        if (voidUpgrade) {
            poseStack.pushPose();
            poseStack.translate(0f, 0.5f, 0f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-0.3f, -0.2f, 0f);

            scale = 0.25f + (1 / 64f);
            poseStack.scale(scale, scale, scale);

            ItemStack icon = new ItemStack(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
            itemRenderer.renderStatic(icon, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, 0);

            poseStack.popPose();
        }
    }

    public static int getColorForDistance(double distance) {
        final int START = 255;
        final int END = 50;

        double maxDistance = getMaxDistance();
        double clamped = Math.min(distance, maxDistance);
        double factor = clamped / maxDistance;

        int val = (int) (START + (END - START) * factor);

        return (val << 16) | (val << 8) | val;
    }

    private static float customZOffset(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof AbstractSimpleShaftBlock
                    || block instanceof FenceBlock
                    || block.defaultBlockState().is(BlockTags.BUTTONS)
                    || block == Blocks.END_ROD) {
                return -0.1f;
            }
        }
        return 0f;
    }
}
