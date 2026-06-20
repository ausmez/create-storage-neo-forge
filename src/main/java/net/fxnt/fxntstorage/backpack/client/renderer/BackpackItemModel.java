package net.fxnt.fxntstorage.backpack.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class BackpackItemModel extends BakedModelWrapper<BakedModel> {

    public BackpackItemModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack,
                                              boolean applyLeftHandTransform) {
        super.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return this;
    }
}
