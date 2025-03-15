package net.fxnt.fxntstorage.passer;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class PasserHelper {

    @Nullable
    public static IItemHandler getStorage(Level level, BlockPos blockPos, Direction facing, boolean isSourceContainer) {
        BlockPos containerPos = isSourceContainer ? blockPos.relative(facing.getOpposite()) : blockPos.relative(facing);
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity != null) {
            LazyOptional<IItemHandler> itemHandlerOpt = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, facing);

            if (itemHandlerOpt.isPresent()) {
                return itemHandlerOpt.orElse(null);
            }
        }
        return null;
    }

    public static void passItems(Level level, IItemHandler srcHandler, IItemHandler dstHandler, Direction facing, long amount, boolean fixedAmount, ItemStack filterItem) {
        if (!(srcHandler instanceof IItemHandlerModifiable srcModifiable) || !(dstHandler instanceof IItemHandlerModifiable dstModifiable))
            return;

        for (int srcSlot = 0; srcSlot < srcModifiable.getSlots(); srcSlot++) {
            ItemStack srcStack = srcModifiable.getStackInSlot(srcSlot);

            if (srcStack.isEmpty()) continue;

            // Check Filter
            if (!FilterItemStack.of(filterItem).test(level, srcStack)) continue;

            // Determine amount to transfer
            long transferAmount = Math.min(srcStack.getCount(), amount);
            if (transferAmount <= 0) continue;

            // Create a new stack for the amount to be transferred
            ItemStack stackToMove = srcStack.copy();
            stackToMove.setCount((int) transferAmount);

            // Try to insert into the destination
            int dstSlot;
            ItemStack remaining;
            boolean doExtract = false;

            for (dstSlot = 0; dstSlot < dstModifiable.getSlots(); dstSlot++) {
                ItemStack dstStack = dstModifiable.getStackInSlot(dstSlot);

                // Check if the destination slot is compatible
                if (dstStack.isEmpty() || ItemStack.isSameItemSameTags(dstStack, stackToMove)) {

                    remaining = dstModifiable.insertItem(dstSlot, stackToMove, true);

                    // Calculate the actual inserted amount
                    int actualInsertAmount = stackToMove.getCount() - remaining.getCount();

                    if (actualInsertAmount > 0) {
                        if (fixedAmount && actualInsertAmount == transferAmount) {
                            doExtract = true;
                        } else if (!fixedAmount) {
                            doExtract = true;
                        }

                        if (doExtract) {
                            // Commit the insertion
                            ItemStack toMove = stackToMove.copy();
                            toMove.setCount(actualInsertAmount);

                            remaining = dstModifiable.insertItem(dstSlot, toMove, false);
                            ItemStack extracted = srcModifiable.extractItem(srcSlot, actualInsertAmount, false);

                            // Check if we actually extracted the items
                            if (extracted.getCount() != actualInsertAmount) {
                                // Rollback the insertion if extraction was unsuccessful
                                dstModifiable.insertItem(dstSlot, remaining, false); // Rollback insertion
                                srcModifiable.insertItem(srcSlot, extracted, false); // Rollback extraction
                            }
                            return; // Success
                        }
                    }
                }
            }
        }
    }

}
