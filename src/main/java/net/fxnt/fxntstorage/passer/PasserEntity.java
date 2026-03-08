package net.fxnt.fxntstorage.passer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static net.fxnt.fxntstorage.passer.PasserBlock.FACING;

public class PasserEntity extends SmartBlockEntity {
    private static final int UPDATE_EVERY_X_TICKS = 10;

    private int tickCount = 0;
    VersionedInventoryTrackerBehaviour invVersionTracker;

    public PasserEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
    }

    protected boolean canAcceptItem(ItemStack stack) {
        return !stack.isEmpty();
    }

    protected boolean canActivate() {
        return true;
    }

    protected int getExtractionAmount() {
        return 1;
    }

    protected ItemHelper.ExtractionCountMode getExtractionMode() {
        return ItemHelper.ExtractionCountMode.UPTO;
    }

    private Direction getFacing() {
        return getBlockState().getValue(PasserBlock.FACING);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (tickCount++ < UPDATE_EVERY_X_TICKS) return;
        tickCount = 0;

        IItemHandler srcContainer = getStorage(level, getFacing(), true);
        if (srcContainer == null) return;

        IItemHandler dstContainer = getStorage(level, getFacing(), false);
        if (dstContainer == null) return;

        if (!canActivate()) return; // Redstone check

        // Mimic SmartChute behavior
        Predicate<ItemStack> canAccept = new canAcceptItem(dstContainer);
        int count = getExtractionAmount();
        ItemHelper.ExtractionCountMode mode = getExtractionMode();

        // Test extract from src
        ItemStack extracted = ItemHelper.extract(srcContainer, canAccept, mode, count, true);
        if (extracted.isEmpty()) return;

        // Test insert to dst
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(dstContainer, extracted, true);
        int actualInsertAmount = extracted.getCount() - remainder.getCount();
        if (actualInsertAmount <= 0) return;

        // EXACTLY should only move exact amount
        if (mode == ItemHelper.ExtractionCountMode.EXACTLY && actualInsertAmount != count) return;

        // Do the move
        ItemStack actualExtractAmount = ItemHelper.extract(srcContainer, canAccept, mode, actualInsertAmount, false);
        ItemHandlerHelper.insertItemStacked(dstContainer, actualExtractAmount, false);
    }

    @Nullable
    private IItemHandler getStorage(Level level, Direction facing, boolean isSourceContainer) {
        BlockPos containerPos = isSourceContainer
                ? worldPosition.relative(facing.getOpposite())
                : worldPosition.relative(facing);

        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        BlockState blockState = level.getBlockState(containerPos);

        if (blockEntity != null) {
            if (blockEntity instanceof PackagerBlockEntity pbe) {
                if (pbe.animationTicks > 0 || pbe.getAvailableItems().isEmpty()) // A little hacky, but it works
                    return null;
            }
            if (blockEntity instanceof PackagePortBlockEntity)
                return ((PackagePortBlockEntity) blockEntity).inventory;

            return level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, blockState, blockEntity, facing);
        } else if (blockState.is(Blocks.COMPOSTER)) { // Not a BE
            return level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, blockState, null, isSourceContainer ? facing : facing.getOpposite());
        }
        return null;
    }

    private class canAcceptItem implements Predicate<ItemStack> {
        private final IItemHandler itemHandler;

        public canAcceptItem(IItemHandler itemHandler) {
            this.itemHandler = itemHandler;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            if (itemStack.isEmpty())
                return false;

            if (!canAcceptItem(itemStack))
                return false;

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                if (itemHandler.isItemValid(i, itemStack)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static class PasserFilteringBox extends ValueBoxTransform.Sided {

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(FACING);
            Direction side = getSide();
            float horizontalAngle = AngleHelper.horizontalAngle(side);
            Vec3 location = Vec3.ZERO;
            return switch (facing) {
                case DOWN ->
                        VecHelper.rotateCentered(VecHelper.voxelSpace(8f, 12f, 12.5f), horizontalAngle, Direction.Axis.Y);
                case UP -> VecHelper.rotateCentered(VecHelper.voxelSpace(8f, 4f, 12.5f), horizontalAngle, Direction.Axis.Y);
                case NORTH -> switch (side) {
                    case UP -> VecHelper.voxelSpace(8f, 12.5f, 12f);
                    case DOWN -> VecHelper.voxelSpace(8f, 3.5f, 12f);
                    case EAST -> VecHelper.voxelSpace(12.5f, 8f, 12f);
                    case WEST -> VecHelper.voxelSpace(3.5f, 8f, 12f);
                    case NORTH, SOUTH -> location;
                };
                case SOUTH -> switch (side) {
                    case UP -> VecHelper.voxelSpace(8f, 12.5f, 4f);
                    case DOWN -> VecHelper.voxelSpace(8f, 3.5f, 4f);
                    case EAST -> VecHelper.voxelSpace(12.5f, 8f, 4f);
                    case WEST -> VecHelper.voxelSpace(3.5f, 8f, 4f);
                    case NORTH, SOUTH -> location;
                };
                case EAST -> switch (side) {
                    case UP -> VecHelper.voxelSpace(4f, 12.5f, 8f);
                    case DOWN -> VecHelper.voxelSpace(4f, 3.5f, 8f);
                    case NORTH -> VecHelper.voxelSpace(4f, 8f, 3.5f);
                    case SOUTH -> VecHelper.voxelSpace(4f, 8f, 12.5f);
                    case EAST, WEST -> location;
                };
                case WEST -> switch (side) {
                    case UP -> VecHelper.voxelSpace(12f, 12.5f, 8f);
                    case DOWN -> VecHelper.voxelSpace(12f, 3.5f, 8f);
                    case NORTH -> VecHelper.voxelSpace(12f, 8f, 3.5f);
                    case SOUTH -> VecHelper.voxelSpace(12f, 8f, 12.5f);
                    case EAST, WEST -> location;
                };
            };
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(FACING);
            Direction side = getSide();
            float yRot = AngleHelper.horizontalAngle(side) + 180;
            float xRot = side == Direction.UP ? 90 : side == Direction.DOWN ? 270 : 0;

            switch (facing) {
                case NORTH -> yRot = side == Direction.UP ? 0 : yRot;
                case SOUTH -> yRot = side == Direction.DOWN ? 0 : yRot;
                case EAST -> yRot = side == Direction.UP ? 270 : side == Direction.DOWN ? 90 : yRot;
                case WEST -> yRot = side == Direction.UP ? 90 : side == Direction.DOWN ? 270 : yRot;
            }

            TransformStack.of(ms).rotateYDegrees(yRot).rotateXDegrees(xRot);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(FACING);
            if (facing == Direction.UP || facing == Direction.DOWN) {
                return direction.getAxis().isHorizontal();
            } else {
                return direction != facing && direction != facing.getOpposite();
            }
        }

        @Override
        protected Vec3 getSouthLocation() {
            return Vec3.ZERO;
        }
    }
}
