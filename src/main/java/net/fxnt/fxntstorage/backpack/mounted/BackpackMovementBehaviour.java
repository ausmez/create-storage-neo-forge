package net.fxnt.fxntstorage.backpack.mounted;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

public class BackpackMovementBehaviour implements MovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        if (context.world == null || context.world.isClientSide) return;

        @Nullable BackpackMountedStorage storage = getMountedStorage(context);
        if (storage == null) return;

        if (storage.hasActiveUpgrade(UpgradeType.JUKEBOX))
            storage.tickJukebox(context);
        if (storage.hasActiveUpgrade(UpgradeType.MAGNET) && context.world.getGameTime() % 30 == 0)
            storage.tickMagnet(context);
        if (storage.hasActiveUpgrade(UpgradeType.WORKSHOP))
            storage.tickWorkshop(context);
    }

    private @Nullable BackpackMountedStorage getMountedStorage(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        MountedItemStorage storage = entity.getContraption().getStorage().getAllItemStorages().get(context.localPos);
        return storage instanceof BackpackMountedStorage mountedStorage ? mountedStorage : null;
    }
}
