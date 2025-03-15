package net.fxnt.fxntstorage.controller;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("all")
public enum StorageControllerUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            IItemHandler targetInv = targetBE.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve().orElse(null);
            StorageControllerEntity storageControllerEntity = ((StorageControllerEntity) targetBE);

            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    storageControllerEntity.storageNetwork.insertItems(itemStack);
                }

                return true;
            } else {
                int totalToInsert = 0;
                boolean roomAvailable = false;
                for (ItemStack itemStack : items) {
                    if (itemStack != null && !itemStack.isEmpty()) totalToInsert += itemStack.getCount();
                    for (int slot = 0; slot < storageControllerEntity.getContainerSize(); ++slot) {
                        if (storageControllerEntity.canPlaceItem(slot, itemStack)) {
                            roomAvailable = true;
                        }
                    }
                }

                return roomAvailable;
            }
        }
    }
}
