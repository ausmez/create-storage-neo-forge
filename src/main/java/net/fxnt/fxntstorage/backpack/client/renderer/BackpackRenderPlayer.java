package net.fxnt.fxntstorage.backpack.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.model.BackpackModelBase;
import net.fxnt.fxntstorage.backpack.client.model.BackpackModelPlayer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopClientState;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelPlacement;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopFlywheelRenderer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BackpackRenderPlayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final BackpackModelPlayer<AbstractClientPlayer> model;

    public BackpackRenderPlayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
        ModelPart modelPart = BackpackModelBase.createModel(true).bakeRoot();
        this.model = new BackpackModelPlayer<>(modelPart);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull AbstractClientPlayer livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(livingEntity);
        if (backpack.isEmpty()) return;

        if (FXNTStorage.CURIOS_LOADED) {
            boolean isBackpackVisible = BackpackHelper.isBackpackCuriosSlotVisible(livingEntity);

            if (!(livingEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem)) {
                // Is the Curios slot visibility toggled
                if (!isBackpackVisible) return;
            }
        }

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(0F, 0.65F, -0.3F);
        poseStack.scale(0.85F, 0.85F, 0.85F);

        if (livingEntity.isCrouching()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-58.0F));
            poseStack.translate(0D, 0.15D, -0.1D);
        }

        this.getParentModel().copyPropertiesTo(model);
        model.setupAnim(this.getParentModel());

        String backpackType = ResourceLocation.read(backpack.getItem().toString())
                .result()
                .map(ResourceLocation::getPath)
                .orElse("backpack");
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "textures/block/" + backpackType + ".png");

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(textureLocation));

        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        if (hasActiveWorkshop(backpack) && ConfigManager.ClientConfig.WORKSHOP_FLYWHEEL_VISUALS.get()) {
            renderFlywheels(poseStack, buffer, packedLight, livingEntity.getId());
        }

        poseStack.popPose();
    }

    private static final String WORKSHOP_ACTIVE_NAME =
            ((UpgradeItem) UpgradeType.WORKSHOP.getActiveItem()).getUpgradeName();

    private static boolean hasActiveWorkshop(ItemStack backpack) {
        List<String> upgrades = backpack.get(ModDataComponents.BACKPACK_UPGRADES);
        return upgrades != null && upgrades.contains(WORKSHOP_ACTIVE_NAME);
    }

    private void renderFlywheels(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int entityId) {
        boolean processing = WorkshopClientState.isProcessing(entityId);
        float angle = WorkshopClientState.advanceAngle(entityId, WorkshopFlywheelRenderer.spinDegPerTick(processing));

        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());

        poseStack.pushPose();
        // Move into the backpack body's local space so the flywheels follow the pack as the body bends
        model.modelPart.translateAndRotate(poseStack);
        WorkshopFlywheelRenderer.renderPair(poseStack, consumer, packedLight, angle,
                WorkshopFlywheelPlacement.WORN_OFFSET_X, WorkshopFlywheelPlacement.WORN_OFFSET_Y,
                WorkshopFlywheelPlacement.WORN_OFFSET_Z, WorkshopFlywheelPlacement.WORN_SCALE);
        poseStack.popPose();
    }
}
