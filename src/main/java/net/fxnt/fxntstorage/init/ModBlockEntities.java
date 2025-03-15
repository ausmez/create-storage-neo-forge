package net.fxnt.fxntstorage.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.fxnt.fxntstorage.backpacks.main.BackpackEntity;
import net.fxnt.fxntstorage.containers.StorageBoxEntity;
import net.fxnt.fxntstorage.containers.StorageBoxEntityRenderer;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.passer.PasserEntity;
import net.fxnt.fxntstorage.passer.PasserSmartEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntityRenderer;

import static net.fxnt.fxntstorage.FXNTStorage.REGISTRATE;

public class ModBlockEntities {

    public static final BlockEntityEntry<StorageBoxEntity> STORAGE_BOX_ENTITY = REGISTRATE
            .blockEntity("storage_box_entity", StorageBoxEntity::new)
            .validBlocks(
                    ModBlocks.STORAGE_BOX,
                    ModBlocks.ANDESITE_STORAGE_BOX,
                    ModBlocks.COPPER_STORAGE_BOX,
                    ModBlocks.BRASS_STORAGE_BOX,
                    ModBlocks.HARDENED_STORAGE_BOX
            )
            .renderer(() -> StorageBoxEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<SimpleStorageBoxEntity> SIMPLE_STORAGE_BOX_ENTITY = REGISTRATE
            .blockEntity("simple_storage_box_entity", SimpleStorageBoxEntity::new)
            .validBlocks(
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
            )
            .renderer(() -> SimpleStorageBoxEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<BackpackEntity> BACK_PACK_ENTITY = REGISTRATE
            .blockEntity("back_pack_entity", BackpackEntity::new)
            .validBlocks(
                    ModBlocks.BACK_PACK,
                    ModBlocks.ANDESITE_BACK_PACK,
                    ModBlocks.COPPER_BACK_PACK,
                    ModBlocks.BRASS_BACK_PACK,
                    ModBlocks.HARDENED_BACK_PACK
            )
            .register();

    public static final BlockEntityEntry<StorageControllerEntity> STORAGE_CONTROLLER_ENTITY = REGISTRATE
            .blockEntity("storage_controller_entity", StorageControllerEntity::new)
            .validBlock(ModBlocks.STORAGE_CONTROLLER)
            .register();

    public static final BlockEntityEntry<StorageInterfaceEntity> STORAGE_INTERFACE_ENTITY = REGISTRATE
            .blockEntity("storage_interface_entity", StorageInterfaceEntity::new)
            .validBlock(ModBlocks.STORAGE_INTERFACE)
            .register();

    public static final BlockEntityEntry<PasserEntity> PASSER_ENTITY = REGISTRATE
            .blockEntity("passer_entity", PasserEntity::new)
            .validBlock(ModBlocks.PASSER_BLOCK)
            .register();

    public static final BlockEntityEntry<PasserSmartEntity> SMART_PASSER_ENTITY = REGISTRATE
            .blockEntity("smart_passer_entity", PasserSmartEntity::new)
            .validBlock(ModBlocks.SMART_PASSER_BLOCK)
            .register();

    public static void register() {
    }
}
