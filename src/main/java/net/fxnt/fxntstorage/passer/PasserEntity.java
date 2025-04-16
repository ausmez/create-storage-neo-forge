package net.fxnt.fxntstorage.passer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import static net.fxnt.fxntstorage.passer.PasserBlock.FACING;

public class PasserEntity extends BlockEntity {
    public int lastTick = 0;
    public boolean doTick = false;
    public int updateEveryXTicks = 10;
    private Direction facing;

    public PasserEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.facing = this.getBlockState().getValue(FACING);
    }

    public void serverTick(Level level, BlockPos blockPos) {
        if (!level.isClientSide) {

            lastTick++;
            if (lastTick >= updateEveryXTicks) {
                lastTick = 0;
                doTick = true;
            }
            if (!doTick) return;
            doTick = false;

            this.facing = this.getBlockState().getValue(FACING);
            IItemHandler srcContainer = PasserHelper.getStorage(level, blockPos, this.facing, true);
            if (srcContainer == null) {
                return;
            }
            IItemHandler dstContainer = PasserHelper.getStorage(level, blockPos, this.facing, false);
            if (dstContainer == null) {
                return;
            }

            ItemStack filterItem = ItemStack.EMPTY;
            long amount = 1;
            boolean fixedAmount = false;

            PasserHelper.passItems(level, srcContainer, dstContainer, this.facing, amount, fixedAmount, filterItem); // Set to limit set by filter
        }
    }

}
