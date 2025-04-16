package net.fxnt.fxntstorage.controller;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public enum StorageInterfaceUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            StorageInterfaceEntity storageInterfaceEntity = ((StorageInterfaceEntity) targetBE);

            if (!simulate) {
                for (ItemStack itemStack : items) {
                    storageInterfaceEntity.controller.storageNetwork.insertItems(itemStack);
                }

                return true;
            } else {
                Map<Util.ItemWithComponent, Integer> itemCounts = new HashMap<>();
                for (ItemStack stack : items) {
                    if (stack.isEmpty()) continue;
                    Util.ItemWithComponent key = new Util.ItemWithComponent(stack.getItem(), stack.getComponentsPatch());
                    itemCounts.merge(key, stack.getCount(), Integer::sum);
                }

                Map<ItemStack, Boolean> condensed = new HashMap<>();
                for (Map.Entry<Util.ItemWithComponent, Integer> entry : itemCounts.entrySet()) {
                    Util.ItemWithComponent key = entry.getKey();
                    Item item = key.item();
                    DataComponentPatch patch = key.patch();

                    int totalCount = entry.getValue();

                    while (totalCount > 0) {
                        int stackSize = totalCount;
                        ItemStack stack = new ItemStack(item, stackSize);
                        if (!patch.isEmpty()) {
                            stack.applyComponents(patch);
                        }
                        condensed.put(stack, false);
                        totalCount -= stackSize;
                    }
                }

                for (Map.Entry<ItemStack, Boolean> entry : condensed.entrySet()) {
                    ItemStack itemStack = entry.getKey();

                    for (int slot = 0; slot < storageInterfaceEntity.getContainerSize(); ++slot) {
                        if (storageInterfaceEntity.canPlaceItem(slot, itemStack) && !entry.getValue()) {
                            entry.setValue(true);
                        }
                    }
                }

                return condensed.values().stream().allMatch(Boolean::booleanValue);

            }
        }
    }
}
