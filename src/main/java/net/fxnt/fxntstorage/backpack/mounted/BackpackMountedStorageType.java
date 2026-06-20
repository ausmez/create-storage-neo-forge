package net.fxnt.fxntstorage.backpack.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BackpackMountedStorageType extends MountedItemStorageType<BackpackMountedStorage> {

    public BackpackMountedStorageType() {
        super(BackpackMountedStorage.CODEC);
    }

    @Override
    public @Nullable BackpackMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        return be instanceof BackpackEntity backpackEntity ? BackpackMountedStorage.fromStorage(backpackEntity) : null;
    }
}
