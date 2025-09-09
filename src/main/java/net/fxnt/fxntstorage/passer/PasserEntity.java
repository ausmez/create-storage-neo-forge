package net.fxnt.fxntstorage.passer;

import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
    private Direction facing;
    VersionedInventoryTrackerBehaviour invVersionTracker;

    public PasserEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.facing = pBlockState.getValue(FACING);
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

    private boolean canTick() {
        if (tickCount++ < UPDATE_EVERY_X_TICKS) return false;
        tickCount = 0;

        return true;
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!canTick()) return;

        this.facing = getBlockState().getValue(FACING);

        IItemHandler srcContainer = getStorage(level, facing, true);
        if (srcContainer == null) return;

        IItemHandler dstContainer = getStorage(level, facing, false);
        if (dstContainer == null) return;

        if (!canActivate()) return; // Redstone check

        // Mimic SmartChute behaviour
        Predicate<ItemStack> canAccept = new canAcceptItem(dstContainer);
        int count = getExtractionAmount();
        ItemHelper.ExtractionCountMode mode = getExtractionMode();

        ItemStack extracted = ItemHelper.extract(srcContainer, canAccept, mode, count, true);
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(dstContainer, extracted, true);

        int actualInsertAmount = extracted.getCount() - remainder.getCount();

        if (actualInsertAmount > 0) {
            // Do the move!
            if (mode.equals(ItemHelper.ExtractionCountMode.UPTO)) count = actualInsertAmount;
            extracted = ItemHelper.extract(srcContainer, canAccept, mode, count, false);
            if (extracted.getCount() == actualInsertAmount)
                ItemHandlerHelper.insertItemStacked(dstContainer, extracted, false);
        }
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
            if (blockEntity instanceof PackagePortBlockEntity) return null;

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

}
