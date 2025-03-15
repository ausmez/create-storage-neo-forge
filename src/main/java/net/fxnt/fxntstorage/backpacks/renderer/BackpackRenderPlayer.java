package net.fxnt.fxntstorage.backpacks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.main.BackpackItem;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModItems;
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
import org.jetbrains.annotations.NotNull;

public class BackpackRenderPlayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private ResourceLocation TEXTURE_LOCATION;
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

        if (FXNTStorage.curiosLoaded) {
            boolean isCuriosSlotVisible = BackpackHelper.isCuriosSlotVisible(livingEntity, "back");

            if (!(livingEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem)) {
                // Is the Curios slot visibility toggled
                if (!isCuriosSlotVisible) return;
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

        if (backpack.getItem().equals(ModBlocks.BACK_PACK.asItem())) {
            TEXTURE_LOCATION = new ResourceLocation(FXNTStorage.MOD_ID, "textures/block/back_pack.png");
        } else if (backpack.getItem().equals(ModBlocks.ANDESITE_BACK_PACK.asItem())) {
            TEXTURE_LOCATION = new ResourceLocation(FXNTStorage.MOD_ID, "textures/block/andesite_back_pack.png");
        } else if (backpack.getItem().equals(ModBlocks.COPPER_BACK_PACK.asItem())) {
            TEXTURE_LOCATION = new ResourceLocation(FXNTStorage.MOD_ID, "textures/block/copper_back_pack.png");
        } else if (backpack.getItem().equals(ModBlocks.BRASS_BACK_PACK.asItem())) {
            TEXTURE_LOCATION = new ResourceLocation(FXNTStorage.MOD_ID, "textures/block/brass_back_pack.png");
        } else if (backpack.getItem().equals(ModBlocks.HARDENED_BACK_PACK.asItem())) {
            TEXTURE_LOCATION = new ResourceLocation(FXNTStorage.MOD_ID, "textures/block/hardened_back_pack.png");
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE_LOCATION));

        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();

    }

}
