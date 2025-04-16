package net.fxnt.fxntstorage.init;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import net.fxnt.fxntstorage.backpack.main.BackpackUnpacking;
import net.fxnt.fxntstorage.container.StorageBoxUnpacking;
import net.fxnt.fxntstorage.controller.StorageControllerUnpacking;
import net.fxnt.fxntstorage.controller.StorageInterfaceUnpacking;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxUnpacking;

@SuppressWarnings("all")
public class ModUnpackers {
    public static void registerHandlers() {
        UnpackingHandler.REGISTRY.register(ModBlocks.STORAGE_BOX.get(), StorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.ANDESITE_STORAGE_BOX.get(), StorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.COPPER_STORAGE_BOX.get(), StorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.BRASS_STORAGE_BOX.get(), StorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.HARDENED_STORAGE_BOX.get(), StorageBoxUnpacking.INSTANCE);

        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_ACACIA.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_CHERRY.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON.get(), SimpleStorageBoxUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.SIMPLE_STORAGE_BOX_WARPED.get(), SimpleStorageBoxUnpacking.INSTANCE);

        UnpackingHandler.REGISTRY.register(ModBlocks.STORAGE_CONTROLLER.get(), StorageControllerUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.STORAGE_INTERFACE.get(), StorageInterfaceUnpacking.INSTANCE);

        UnpackingHandler.REGISTRY.register(ModBlocks.BACKPACK.get(), BackpackUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.ANDESITE_BACKPACK.get(), BackpackUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.COPPER_BACKPACK.get(), BackpackUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.BRASS_BACKPACK.get(), BackpackUnpacking.INSTANCE);
        UnpackingHandler.REGISTRY.register(ModBlocks.HARDENED_BACKPACK.get(), BackpackUnpacking.INSTANCE);
    }
}
