package net.fxnt.fxntstorage.passer;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

import static net.fxnt.fxntstorage.passer.PasserBlock.FACING;
import static net.fxnt.fxntstorage.passer.PasserBlock.POWERED;

public class PasserSmartEntity extends SmartBlockEntity {
    public int lastTick = 0;
    public boolean doTick = false;
    public int updateEveryXTicks = 10;
    private Direction facing;
    public FilteringBehaviour filtering;

    public PasserSmartEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.facing = this.getBlockState().getValue(FACING);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(filtering =
                new FilteringBehaviour(this, new PasserFilteringBox()).showCount());
    }

    public void serverTick(Level level, BlockPos blockPos) {
        if (level.isClientSide) return;

        BlockState blockState = this.getBlockState();
        if (blockState.hasProperty(POWERED) && blockState.getValue(POWERED)) return;

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

        ItemStack filterItem = filtering.getFilter();
        long amount = filtering.getAmount();
        boolean fixedAmount = !filtering.upTo;

        PasserHelper.passItems(level, srcContainer, dstContainer, amount, fixedAmount, filterItem); // Set to limit set by filter
    }

}
