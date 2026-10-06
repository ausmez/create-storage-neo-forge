package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WorkshopFlywheelRenderer {

    public static final PartialModel FLYWHEEL = PartialModel.of(FXNTStorage.modLoc("block/backpack_flywheel"));

    private WorkshopFlywheelRenderer() {
    }

    // PartialModels must exist before models load. Called from the mod constructor
    public static void init() {
    }

    public static float spinDegPerTick(boolean processing) {
        return processing ? WorkshopUpgrade.kineticSpeed() * 3f / 10f : 0f;
    }

    // Renders the backpack's flywheel layout; pose must be in the backpack model's space (0-1 block units)
    public static void render(Item backpack, boolean worn, PoseStack pose, VertexConsumer consumer, int packedLight, float angle) {
        for (WorkshopFlywheelPlacement placement : WorkshopFlywheelLayouts.get(backpack, worn)) {
            renderOne(placement, pose, consumer, packedLight, angle);
        }
    }

    private static void renderOne(WorkshopFlywheelPlacement placement, PoseStack pose, VertexConsumer consumer,
                                  int packedLight, float angle) {
        float scale = placement.size() / 16f;
        SuperByteBuffer buffer = CachedBuffers.partial(FLYWHEEL, AllBlocks.FLYWHEEL.getDefaultState())
                .translate(placement.position().scale(1 / 16d).subtract(scale / 2f, scale / 2f, scale / 2f))
                .scale(scale)
                .rotateCenteredDegrees(placement.reverse() ? angle : -angle, placement.axis());

        // Turn the model's Y spin axis onto the placement axis, matching Create's flywheel blockstate
        switch (placement.axis()) {
            case X -> buffer.rotateCenteredDegrees(-90, Direction.Axis.Y).rotateCenteredDegrees(-90, Direction.Axis.X);
            case Z -> buffer.rotateCenteredDegrees(-180, Direction.Axis.Y).rotateCenteredDegrees(-90, Direction.Axis.X);
            case Y -> {
            }
        }

        buffer.light(packedLight).renderInto(pose, consumer);
    }
}
