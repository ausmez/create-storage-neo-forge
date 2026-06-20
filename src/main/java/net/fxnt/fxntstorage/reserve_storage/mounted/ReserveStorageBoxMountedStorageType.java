package net.fxnt.fxntstorage.reserve_storage.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReserveStorageBoxMountedStorageType extends MountedItemStorageType<ReserveStorageBoxMountedStorage> {
    public ReserveStorageBoxMountedStorageType() {
        super(ReserveStorageBoxMountedStorage.CODEC);
    }

    @Override
    public @Nullable ReserveStorageBoxMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        return be instanceof ReserveStorageBoxEntity reserveStorageBox ? ReserveStorageBoxMountedStorage.fromStorage(reserveStorageBox) : null;
    }
}
