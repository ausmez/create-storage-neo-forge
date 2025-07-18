package net.fxnt.fxntstorage.simple_storage.mounted;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public class SimpleStorageBoxMovementBehaviour implements MovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        @Nullable SimpleStorageBoxMountedStorage storage = getMountedStorage(context);
        if (storage != null) {
            if (!storage.initialized) {
                storage.initBlockEntityData(context);
            }
            if (storage.isDirty()) {
                storage.updateClientStorageData(context);
            }
        }
    }

    private @Nullable SimpleStorageBoxMountedStorage getMountedStorage(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        MountedItemStorage storage = entity.getContraption().getStorage().getAllItemStorages().get(context.localPos);

        if (storage instanceof SimpleStorageBoxMountedStorage simpleStorageBox) {
            return simpleStorageBox;
        }

        return null;
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        SimpleStorageBoxEntityRenderer.renderFromContraptionContext(context, matrices, buffer);
    }

}
