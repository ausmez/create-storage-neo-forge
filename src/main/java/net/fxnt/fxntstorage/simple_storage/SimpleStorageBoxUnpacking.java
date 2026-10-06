package net.fxnt.fxntstorage.simple_storage;

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
            } else if (ssbe.hasCompactingUpgrade() && ssbe.compactingChain != null) {
                return unpackCompacting(ssbe, ssbe.compactingChain, items, simulate);
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), false);
                }

                return true;
            } else {
                // Test if all ItemStacks in package are the same type
                ItemStack ref = null;
                for (ItemStack stack : items) {
                    if (ref == null) {
                        ref = stack;
                    } else if (!ItemStack.isSameItemSameComponents(ref, stack)) {
                        return false;
                    }
                }

                int totalToInsert = 0;
                if (ssbe.filterTest(ref)) {
                    if (ssbe.voidUpgrade) return true;

                    for (ItemStack itemStack : items) {
                        if (itemStack != null && !itemStack.isEmpty()) totalToInsert += itemStack.getCount();
                    }

                    return totalToInsert + ssbe.getStoredAmount() <= ssbe.getMaxItemCapacity();
                }

                return false;
            }
        }
    }

    private static boolean unpackCompacting(SimpleStorageBoxEntity ssbe, CompactingChain chain, List<ItemStack> items, boolean simulate) {
        if (!simulate) {
            IItemHandler handler = ssbe.getCapabilityHandler();
            for (ItemStack itemStack : items) {
                ItemHandlerHelper.insertItem(handler, itemStack.copy(), false);
            }
            return true;
        }

        long t0ToInsert = 0;
        for (ItemStack itemStack : items) {
            if (itemStack == null || itemStack.isEmpty()) continue;
            int t0Units = chain.toT0Units(itemStack.getItem(), itemStack.getCount());
            if (t0Units <= 0) return false;
            t0ToInsert += t0Units;
        }

        if (ssbe.voidUpgrade) return true;
        return t0ToInsert + ssbe.getStoredAmount() <= ssbe.getMaxItemCapacity();
    }
}
