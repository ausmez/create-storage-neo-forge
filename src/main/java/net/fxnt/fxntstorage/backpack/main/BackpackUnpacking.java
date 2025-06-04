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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
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
                int slotLimit = Util.ITEM_SLOT_END_RANGE;
                int stackMultiplier = backpackEntity.stackMultiplier;

                // Virtual copies of slot content and remaining capacity
                ItemStack[] virtualStacks = new ItemStack[slotLimit];
                int[] remaining = new int[slotLimit];

                // Initialize from real inventory
                for (int i = 0; i < slotLimit; i++) {
                    ItemStack slotStack = targetInv.getStackInSlot(i);
                    virtualStacks[i] = slotStack.copy();

                    if (slotStack.isEmpty()) {
                        remaining[i] = -1; // Mark empty, will be calculated when assigned
                    } else {
                        remaining[i] = (stackMultiplier * slotStack.getMaxStackSize()) - slotStack.getCount();
                    }
                }

                // Try to virtually insert each item
                outer:
                for (ItemStack insertStack : items) {
                    int toInsert = insertStack.getCount();

                    // First pass: try merging into compatible stacks
                    for (int i = 0; i < slotLimit; i++) {
                        ItemStack vs = virtualStacks[i];
                        if (vs.isEmpty()) continue;

                        if (ItemStack.isSameItemSameTags(vs, insertStack)) {
                            int insertable = Math.min(remaining[i], toInsert);
                            if (insertable > 0) {
                                remaining[i] -= insertable;
                                vs.grow(insertable);
                                toInsert -= insertable;
                                if (toInsert <= 0) continue outer;
                            }
                        }
                    }

                    // Second pass: assign empty slot
                    for (int i = 0; i < slotLimit; i++) {
                        ItemStack vs = virtualStacks[i];
                        if (!vs.isEmpty()) continue;

                        // Initialize empty slot for this item
                        virtualStacks[i] = insertStack.copy();
                        virtualStacks[i].setCount(0);
                        remaining[i] = stackMultiplier * insertStack.getMaxStackSize();

                        int insertable = Math.min(remaining[i], toInsert);
                        if (insertable > 0) {
                            remaining[i] -= insertable;
                            virtualStacks[i].grow(insertable);
                            toInsert -= insertable;
                            if (toInsert <= 0) continue outer;
                        }
                    }

                    // Could not insert this item fully
                    return false;
                }

                return true;
            }
        }
    }

}
