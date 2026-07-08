package net.fxnt.fxntstorage.reserve_storage;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBox.VOID_UPGRADE;

@SuppressWarnings("UnstableApiUsage")
public enum ReserveStorageBoxUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (!(targetBE instanceof ReserveStorageBoxEntity reserve)) return false;

        IItemHandler targetInv = reserve.getAutomationHandler();
        boolean isVoidEnabled = blockState.getValue(VOID_UPGRADE);

        if (simulate) {
            if (isVoidEnabled) return true;
            // DEFAULT simulates against the block's ItemHandler capability, which is the automation handler
            return UnpackingHandler.DEFAULT.unpack(level, blockPos, blockState, direction, items, packageOrderWithCrafts, true);
        }

        boolean allInserted = true;
        for (ItemStack itemStack : items) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), false);
            if (!remainder.isEmpty() && !isVoidEnabled) {
                allInserted = false;
            }
        }
        return allInserted;
    }
}
