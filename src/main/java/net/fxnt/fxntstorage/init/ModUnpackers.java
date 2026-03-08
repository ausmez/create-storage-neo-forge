package net.fxnt.fxntstorage.init;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.inventory.BackpackUnpacking;
import net.fxnt.fxntstorage.container.StorageBoxUnpacking;
import net.fxnt.fxntstorage.controller.StorageControllerUnpacking;
import net.fxnt.fxntstorage.controller.StorageInterfaceUnpacking;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxUnpacking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("UnstableApiUsage")
public class ModUnpackers {
    public static void registerHandlers() {
        // Storage Boxes
        register(StorageBoxUnpacking.INSTANCE,
                ModBlocks.CARDBOARD_STORAGE_BOX,
                ModBlocks.STORAGE_BOX,
                ModBlocks.WEATHERED_STORAGE_BOX,
                ModBlocks.ANDESITE_STORAGE_BOX,
                ModBlocks.COPPER_STORAGE_BOX,
                ModBlocks.BRASS_STORAGE_BOX,
                ModBlocks.HARDENED_STORAGE_BOX
        );

        // Simple Storage Boxes
        BuiltInRegistries.BLOCK.stream().filter(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            return id.getNamespace().equals(FXNTStorage.MOD_ID) && id.getPath().contains("simple_storage_box");
        }).forEach((block -> UnpackingHandler.REGISTRY.register(block, SimpleStorageBoxUnpacking.INSTANCE)));

        // Storage Controller & Interface
        register(StorageControllerUnpacking.INSTANCE, ModBlocks.STORAGE_CONTROLLER);
        register(StorageInterfaceUnpacking.INSTANCE, ModBlocks.STORAGE_INTERFACE, ModBlocks.STORAGE_INTERFACE_FILTERED);

        // Backpacks
        register(BackpackUnpacking.INSTANCE,
                ModBlocks.BACKPACK,
                ModBlocks.ANDESITE_BACKPACK,
                ModBlocks.COPPER_BACKPACK,
                ModBlocks.BRASS_BACKPACK,
                ModBlocks.HARDENED_BACKPACK
        );
    }

    @SafeVarargs
    private static void register(UnpackingHandler handler, BlockEntry<? extends Block>... blocks) {
        for (BlockEntry<? extends Block> block : blocks) {
            UnpackingHandler.REGISTRY.register(block.get(), handler);
        }
    }
}
