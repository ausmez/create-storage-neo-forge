package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("all")
public enum SimpleStorageBoxUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            if (!(targetBE instanceof SimpleStorageBoxEntity ssbe)) return false;
            IItemHandler targetInv = ssbe.getItemHandler();

            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    ItemStack remainder = targetInv.insertItem(0, itemStack, simulate);
                }

                return true;
            } else {
                // Test if all ItemStacks in package are the same
                ItemStack ref = null;
                for (ItemStack stack : items) {
                    if (ref == null) {
                        ref = stack;
                    } else if (!ItemStack.isSameItemSameTags(ref, stack)) {
                        return false;
                    }
                }

                int totalToInsert = 0;
                if (ssbe.filterTest(ref)) {
                    if (ssbe.voidUpgrade) return true;

                    for (ItemStack itemStack : items) {
                        if (itemStack != null && !itemStack.isEmpty()) totalToInsert += itemStack.getCount();
                    }

                    return totalToInsert + ssbe.storedAmount <= ssbe.maxItemCapacity;
                }

                return false;
            }
        }
    }
}
