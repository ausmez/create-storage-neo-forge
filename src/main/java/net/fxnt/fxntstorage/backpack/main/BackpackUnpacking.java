package net.fxnt.fxntstorage.backpack.main;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("all")
public enum BackpackUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            BackpackEntity backpackEntity = ((BackpackEntity) targetBE);
            IItemHandler targetInv = backpackEntity.getItemHandler();

            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    targetInv.insertItem(targetInv.getSlots() - 1, itemStack, false);
                    backpackEntity.moveItems();
                }

                return true;
            } else {
                int slotsAvailable = 0;
                for (ItemStack itemStack : items) {

                    for (int i = 0; i < Util.ITEM_SLOT_END_RANGE; ++i) {
                        ItemStack stack = targetInv.getStackInSlot(i);
                        int slotMaxStackSize = backpackEntity.stackMultiplier * itemStack.getMaxStackSize();

                        if (stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, itemStack) && stack.getCount() < slotMaxStackSize)) {
                            int availableSpace = slotMaxStackSize - stack.getCount();
                            if (itemStack.getCount() <= availableSpace) {
                                ++slotsAvailable;
                            }
                        }
                    }

                }

                return slotsAvailable >= items.size();
            }
        }
    }

}
