package net.fxnt.fxntstorage.containers.util;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.fxnt.fxntstorage.containers.StorageBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class StorageBoxFilteringBox extends ValueBoxTransform.Sided {

    @Override
    protected Vec3 getSouthLocation() { return Vec3.ZERO; }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction side = getSide();
        float horizontalAngle = AngleHelper.horizontalAngle(side);
        return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 10.8, 14.5f), horizontalAngle, Direction.Axis.Y);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {

        Direction facing = StorageBox.getDirectionFacing(state);

        if (facing != null && facing.getAxis().isVertical()) {
            super.rotate(level, pos, state, ms);
            return;
        }

        if (state.getBlock() instanceof StorageBox) {
            super.rotate(level, pos, state, ms);
            TransformStack.of(ms).rotateX(0f);
            return;
        }
        float yRot = AngleHelper.horizontalAngle(Objects.requireNonNull(StorageBox.getDirectionFacing(state))) + (facing == Direction.DOWN ? 180 : 0);
        TransformStack.of(ms).rotateYDegrees(yRot).rotateXDegrees(facing == Direction.DOWN ? -90 : 90);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        Direction facing = StorageBox.getDirectionFacing(state);
        if (facing == null) return false;
        if (facing.getAxis().isVertical()) return direction.getAxis().isHorizontal();
        return direction == facing;
    }

}