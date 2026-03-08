package net.fxnt.fxntstorage.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.container.StorageBoxEntityRenderer;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceFilteredEntity;
import net.fxnt.fxntstorage.passer.PasserEntity;
import net.fxnt.fxntstorage.passer.PasserSmartEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntityRenderer;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.stream.Stream;

import static net.fxnt.fxntstorage.FXNTStorage.REGISTRATE;

public class ModBlockEntities {

    public static final BlockEntityEntry<StorageBoxEntity> STORAGE_BOX_ENTITY = REGISTRATE
            .blockEntity("storage_box_entity", StorageBoxEntity::new)
            .validBlocks(
                    ModBlocks.STORAGE_BOX,
                    ModBlocks.CARDBOARD_STORAGE_BOX,
                    ModBlocks.WEATHERED_STORAGE_BOX,
                    ModBlocks.ANDESITE_STORAGE_BOX,
                    ModBlocks.COPPER_STORAGE_BOX,
                    ModBlocks.BRASS_STORAGE_BOX,
                    ModBlocks.HARDENED_STORAGE_BOX
            )
            .renderer(() -> StorageBoxEntityRenderer::new)
            .register();

    @SuppressWarnings("unchecked")
    public static final BlockEntityEntry<SimpleStorageBoxEntity> SIMPLE_STORAGE_BOX_ENTITY = REGISTRATE
            .blockEntity("simple_storage_box_entity", SimpleStorageBoxEntity::new)
            .validBlocks((NonNullSupplier<? extends Block>[]) Stream.of(
                    ModBlocks.SIMPLE_STORAGE_BOX_OAK,
                    ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE,
                    ModBlocks.SIMPLE_STORAGE_BOX_BIRCH,
                    ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE,
                    ModBlocks.SIMPLE_STORAGE_BOX_ACACIA,
                    ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK,
                    ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE,
                    ModBlocks.SIMPLE_STORAGE_BOX_CHERRY,
                    ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO,
                    ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON,
                    ModBlocks.SIMPLE_STORAGE_BOX_WARPED,
                    ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK
            ).filter(Objects::nonNull).toArray(NonNullSupplier[]::new))
            .renderer(() -> SimpleStorageBoxEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<BackpackEntity> BACKPACK_ENTITY = REGISTRATE
            .blockEntity("backpack_entity", BackpackEntity::new)
            .validBlocks(
                    ModBlocks.BACKPACK,
                    ModBlocks.ANDESITE_BACKPACK,
                    ModBlocks.COPPER_BACKPACK,
                    ModBlocks.BRASS_BACKPACK,
                    ModBlocks.HARDENED_BACKPACK
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

    public static final BlockEntityEntry<StorageInterfaceFilteredEntity> STORAGE_INTERFACE_FILTERED_ENTITY = REGISTRATE
            .blockEntity("storage_interface_filtered_entity", StorageInterfaceFilteredEntity::new)
            .validBlock(ModBlocks.STORAGE_INTERFACE_FILTERED)
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
