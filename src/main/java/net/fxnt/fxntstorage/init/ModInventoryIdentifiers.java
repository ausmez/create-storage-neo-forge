package net.fxnt.fxntstorage.init;

import com.simibubi.create.api.packager.InventoryIdentifier;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceFilteredEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModInventoryIdentifiers {
    public static void registerHandlers() {
        InventoryIdentifier.REGISTRY.register(ModBlocks.STORAGE_CONTROLLER.get(), (level, state, face) -> {
            BlockEntity be = level.getBlockEntity(face.getPos());
            if (!(be instanceof StorageControllerEntity sce)) return null;
            return sce.getInventoryIdentifier();
        });

        InventoryIdentifier.REGISTRY.register(ModBlocks.STORAGE_INTERFACE.get(), (level, state, face) -> {
            BlockEntity be = level.getBlockEntity(face.getPos());
            if (!(be instanceof StorageInterfaceEntity sie) || sie.controller == null) return null;
            return sie.controller.getInventoryIdentifier();
        });

        InventoryIdentifier.REGISTRY.register(ModBlocks.STORAGE_INTERFACE_FILTERED.get(), (level, state, face) -> {
            BlockEntity be = level.getBlockEntity(face.getPos());
            if (!(be instanceof StorageInterfaceFilteredEntity sife) || sife.controller == null) return null;
            return sife.controller.getInventoryIdentifier();
        });
    }
}
