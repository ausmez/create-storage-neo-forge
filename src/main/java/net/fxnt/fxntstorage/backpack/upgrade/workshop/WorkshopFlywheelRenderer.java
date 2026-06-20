package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WorkshopFlywheelRenderer {
    private static final BlockState FLYWHEEL = AllBlocks.FLYWHEEL.getDefaultState()
            .setValue(BlockStateProperties.AXIS, Direction.Axis.X);

    private WorkshopFlywheelRenderer() {
    }

    public static float spinDegPerTick(boolean processing) {
        return processing ? WorkshopUpgrade.kineticSpeed() * 3f / 10f : 0f;
    }

    public static void renderPair(PoseStack pose, VertexConsumer consumer, int packedLight, float angle,
                                  float offsetX, float offsetY, float offsetZ, float scale) {
        renderOne(pose, consumer, packedLight, angle, offsetX, offsetY, offsetZ, scale);
        renderOne(pose, consumer, packedLight, angle, -offsetX, offsetY, offsetZ, scale);
    }

    private static void renderOne(PoseStack pose, VertexConsumer consumer, int packedLight, float angle,
                                  float x, float y, float z, float scale) {
        CachedBuffers.block(FLYWHEEL)
                .translate(x - scale / 2f, y - scale / 2f, z - scale / 2f)
                .scale(scale)
                .rotateCenteredDegrees(angle, Direction.Axis.X)
                .light(packedLight)
                .renderInto(pose, consumer);
    }
}
