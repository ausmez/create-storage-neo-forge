package net.fxnt.fxntstorage.backpacks.main;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public enum BackpackUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            IItemHandler targetInv = targetBE.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve().orElse(null);
            BackpackEntity backpackEntity = ((BackpackEntity) targetBE);

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
                        int slotMaxStackSize = backpackEntity.maxStackSize * itemStack.getMaxStackSize();

                        if (stack.isEmpty() || (ItemStack.isSameItemSameTags(stack, itemStack) && stack.getCount() < slotMaxStackSize)) {
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
