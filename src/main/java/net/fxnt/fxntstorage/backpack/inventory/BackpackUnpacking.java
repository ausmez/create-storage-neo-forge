package net.fxnt.fxntstorage.backpack.inventory;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    ItemStack remaining = itemStack.copy();

                    for (int i : layout.items().range()) {
                        if (remaining.isEmpty())
                            break;

                        remaining = targetInv.insertItem(i, remaining, false);
                    }

                    if (!remaining.isEmpty())
                        return false;
                }

                return true;
            } else {
                // Create a simulated inventory state
                Map<Integer, ItemStack> simulatedSlots = new HashMap<>();
                for (int slot : layout.items().range()) {
                    ItemStack slotStack = targetInv.getStackInSlot(slot);
                    simulatedSlots.put(slot, slotStack.isEmpty() ? ItemStack.EMPTY : slotStack.copy());
                }

                for (ItemStack stack : items) {
                    ItemStack remaining = stack.copy();

                    for (int slot : layout.items().range()) {
                        if (remaining.isEmpty())
                            break;

                        ItemStack slotStack = simulatedSlots.get(slot);

                        if (!targetInv.isItemValid(slot, remaining))
                            continue;

                        // Check if slot can accept this item
                        if (!slotStack.isEmpty() && !ItemStack.isSameItemSameTags(slotStack, remaining)) {
                            continue; // Different item, skip this slot
                        }

                        int slotLimit = targetInv.getSlotLimit(slot);
                        int currentCount = slotStack.getCount();
                        int spaceAvailable = slotLimit - currentCount;

                        if (spaceAvailable <= 0) {
                            continue; // Slot is full
                        }

                        int toInsert = Math.min(remaining.getCount(), spaceAvailable);

                        if (slotStack.isEmpty()) {
                            simulatedSlots.put(slot, remaining.copy());
                            simulatedSlots.get(slot).setCount(toInsert);
                        } else {
                            slotStack.grow(toInsert);
                        }

                        remaining.shrink(toInsert);
                    }

                    if (!remaining.isEmpty())
                        return false;
                }
                return true;
            }
        }
    }
}
