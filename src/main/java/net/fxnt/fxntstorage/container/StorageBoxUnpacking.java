package net.fxnt.fxntstorage.container;

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

import static net.fxnt.fxntstorage.container.StorageBox.VOID_UPGRADE;

@SuppressWarnings("UnstableApiUsage")
public enum StorageBoxUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            boolean isVoidEnabled = blockState.getValue(VOID_UPGRADE);
            StorageBoxEntity storageBoxEntity = ((StorageBoxEntity) targetBE);
            IItemHandler targetInv = storageBoxEntity.getItemHandler();
            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), false);
                }
                return true;
            } else if (isVoidEnabled) {
                return true;
            } else {
                return UnpackingHandler.DEFAULT.unpack(level, blockPos, blockState, direction, items, packageOrderWithCrafts, simulate);
            }
        }
    }

}
