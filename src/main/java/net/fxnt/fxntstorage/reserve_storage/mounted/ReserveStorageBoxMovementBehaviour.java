package net.fxnt.fxntstorage.reserve_storage.mounted;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public class ReserveStorageBoxMovementBehaviour implements MovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        @Nullable ReserveStorageBoxMountedStorage storage = getMountedStorage(context);
        if (storage == null) return;

        if (storage.isDirty())
            storage.updateClientStorageData(context);
    }

    private @Nullable ReserveStorageBoxMountedStorage getMountedStorage(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        MountedItemStorage storage = entity.getContraption().getStorage().getAllItemStorages().get(context.localPos);
        if (storage instanceof ReserveStorageBoxMountedStorage reserveStorage) {
            return reserveStorage;
        }
        return null;
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        ReserveStorageBoxEntityRenderer.renderFromContraptionContext(context, matrices, buffer);
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }
}
