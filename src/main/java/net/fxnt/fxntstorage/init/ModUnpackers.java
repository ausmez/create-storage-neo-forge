package net.fxnt.fxntstorage.init;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fxnt.fxntstorage.backpack.main.BackpackUnpacking;
import net.fxnt.fxntstorage.container.StorageBoxUnpacking;
import net.fxnt.fxntstorage.controller.StorageControllerUnpacking;
import net.fxnt.fxntstorage.controller.StorageInterfaceUnpacking;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxUnpacking;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

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
        register(SimpleStorageBoxUnpacking.INSTANCE,
                ModBlocks.SIMPLE_STORAGE_BOX,
                ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE,
                ModBlocks.SIMPLE_STORAGE_BOX_BIRCH,
                ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE,
                ModBlocks.SIMPLE_STORAGE_BOX_ACACIA,
                ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK,
                ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE,
                ModBlocks.SIMPLE_STORAGE_BOX_CHERRY,
                ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO,
                ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON,
                ModBlocks.SIMPLE_STORAGE_BOX_WARPED
        );

        // Pale Oak if Vanilla Backport mod installed
        if (ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT)) {
            register(SimpleStorageBoxUnpacking.INSTANCE, ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK);
        }

        // Storage Controller & Interface
        register(StorageControllerUnpacking.INSTANCE, ModBlocks.STORAGE_CONTROLLER);
        register(StorageInterfaceUnpacking.INSTANCE, ModBlocks.STORAGE_INTERFACE);

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
