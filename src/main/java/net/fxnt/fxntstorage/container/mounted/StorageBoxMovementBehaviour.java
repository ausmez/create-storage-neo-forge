package net.fxnt.fxntstorage.container.mounted;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.fxnt.fxntstorage.container.StorageBoxEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nullable;

public class StorageBoxMovementBehaviour implements MovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        @Nullable StorageBoxMountedStorage storage = getMountedStorage(context);
        if (storage != null) {
            if (!storage.initialized) {
                storage.initBlockEntityData(context);
            }
            if (storage.isDirty()) {
                storage.updateClientStorageData(context);
            }
        }
    }

    private @Nullable StorageBoxMountedStorage getMountedStorage(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        MountedItemStorage storage = entity.getContraption().getStorage().getAllItemStorages().get(context.localPos);

        if (storage instanceof StorageBoxMountedStorage storageBox) {
            return storageBox;
        }

        return null;
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        StorageBoxEntityRenderer.renderFromContraptionContext(context, matrices, buffer);
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

}
