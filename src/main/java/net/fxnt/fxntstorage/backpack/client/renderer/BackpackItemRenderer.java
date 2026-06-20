package net.fxnt.fxntstorage.backpack.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelPlacement;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelRenderer;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BackpackItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final String WORKSHOP_ACTIVE_NAME =
            ((UpgradeItem) UpgradeType.WORKSHOP.getActiveItem()).getUpgradeName();

    public BackpackItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void renderByItem(ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack,
                             @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Minecraft mc = Minecraft.getInstance();

        BlockState state = blockItem.getBlock().defaultBlockState();

        BakedModel model = mc.getItemRenderer().getItemModelShaper().getItemModel(stack);
        RenderType renderType = ItemBlockRenderTypes.getRenderType(state, false);
        VertexConsumer modelConsumer = ItemRenderer.getFoilBuffer(buffer, renderType, true, stack.hasFoil());
        mc.getItemRenderer().renderModelLists(model, stack, packedLight, packedOverlay, poseStack, modelConsumer);

        if (!hasActiveWorkshop(stack) || !ConfigManager.ClientConfig.WORKSHOP_FLYWHEEL_VISUALS.get()) return;

        Direction facing = state.getValue(BackpackBlock.FACING);
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        WorkshopFlywheelRenderer.renderPair(poseStack, consumer, packedLight, 0f,
                WorkshopFlywheelPlacement.BLOCK_OFFSET_X, WorkshopFlywheelPlacement.BLOCK_OFFSET_Y,
                WorkshopFlywheelPlacement.BLOCK_OFFSET_Z, WorkshopFlywheelPlacement.BLOCK_SCALE);
        poseStack.popPose();
    }

    private static boolean hasActiveWorkshop(ItemStack stack) {
        List<String> upgrades = stack.get(ModDataComponents.BACKPACK_UPGRADES);
        return upgrades != null && upgrades.contains(WORKSHOP_ACTIVE_NAME);
    }
}
