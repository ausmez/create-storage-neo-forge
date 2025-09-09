package net.fxnt.fxntstorage.storage_network;

import com.simibubi.create.compat.thresholdSwitch.ThresholdSwitchCompat;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageNetworkCompat implements ThresholdSwitchCompat {
    @Override
    public boolean isFromThisMod(BlockEntity blockEntity) {
        return blockEntity != null && FXNTStorage.MOD_ID
                .equals(RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType()).getNamespace());
    }

    @Override
    public long getSpaceInSlot(IItemHandler inv, int slot) {
        return inv.getSlotLimit(slot);
    }
}
