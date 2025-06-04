package net.fxnt.fxntstorage.simple_storage.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimpleStorageBoxMountedStorageType extends MountedItemStorageType<SimpleStorageBoxMountedStorage> {
    public SimpleStorageBoxMountedStorageType() {
        super(SimpleStorageBoxMountedStorage.CODEC);
    }

    @Override
    public @Nullable SimpleStorageBoxMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        return be instanceof SimpleStorageBoxEntity simpleStorageBox ? SimpleStorageBoxMountedStorage.fromStorage(simpleStorageBox) : null;
    }

}
