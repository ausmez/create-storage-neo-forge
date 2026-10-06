package net.fxnt.fxntstorage.controller;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public enum StorageInterfaceUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            if (!(targetBE instanceof StorageInterfaceEntity sie)) return false;

            if (sie.controller == null)
                return false;
            final StorageNetwork storageNetwork = sie.controller.getConnectedNetwork();

            // A filtered interface set to exclude empty storage may only fill boxes that already hold the item
            boolean allowEmpty = !(sie instanceof StorageInterfaceFilteredEntity sife) || sife.includesEmptyStorage();

            if (!simulate) return storageNetwork.unpackItems(level, blockPos, direction, items, allowEmpty);

            if (sie instanceof StorageInterfaceFilteredEntity sife) {
                FilteringBehaviour filter = sife.getBehaviour(FilteringBehaviour.TYPE);
                for (ItemStack item : items) {
                    if (filter != null && !filter.test(item)) return false;
                }
            }

            return storageNetwork.canInsertAll(items, allowEmpty);
        }
    }
}
