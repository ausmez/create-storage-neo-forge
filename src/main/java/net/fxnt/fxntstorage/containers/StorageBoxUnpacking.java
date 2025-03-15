package net.fxnt.fxntstorage.containers;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.fxnt.fxntstorage.containers.StorageBox.VOID_UPGRADE;

@SuppressWarnings("all")
public enum StorageBoxUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            boolean isVoidEnabled = blockState.getValue(VOID_UPGRADE);
            IItemHandler targetInv = targetBE.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve().orElse(null);
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
