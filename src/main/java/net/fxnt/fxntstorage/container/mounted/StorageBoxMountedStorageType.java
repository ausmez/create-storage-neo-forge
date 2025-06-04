package net.fxnt.fxntstorage.container.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StorageBoxMountedStorageType extends MountedItemStorageType<StorageBoxMountedStorage> {
    public StorageBoxMountedStorageType() {
        super(StorageBoxMountedStorage.CODEC);
    }

    @Override
    public @Nullable StorageBoxMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        return be instanceof StorageBoxEntity storageBox ? StorageBoxMountedStorage.fromStorage(storageBox) : null;
    }

}
