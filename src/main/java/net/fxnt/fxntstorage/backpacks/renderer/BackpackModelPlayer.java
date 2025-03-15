package net.fxnt.fxntstorage.backpacks.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class BackpackModelPlayer<T extends LivingEntity> extends HumanoidModel<T> {

    public final ModelPart modelPart;

    public BackpackModelPlayer(ModelPart root) {
        super(root);
        this.modelPart = root.getChild("bone");
    }

    public void setupAnim(@NotNull HumanoidModel<T> model) {
        this.modelPart.copyFrom(model.body);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        modelPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

}
