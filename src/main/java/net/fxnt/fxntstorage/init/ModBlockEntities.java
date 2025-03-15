package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.main.BackpackEntity;
import net.fxnt.fxntstorage.containers.StorageBoxEntity;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.passer.PasserEntity;
import net.fxnt.fxntstorage.passer.PasserSmartEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FXNTStorage.MOD_ID);

    public static final RegistryObject<BlockEntityType<StorageBoxEntity>> STORAGE_BOX_ENTITY =
            BLOCK_ENTITIES.register("storage_box_entity", () ->
                    BlockEntityType.Builder.of(StorageBoxEntity::new,
                            ModBlocks.STORAGE_BOX.get(),
                            ModBlocks.ANDESITE_STORAGE_BOX.get(),
                            ModBlocks.COPPER_STORAGE_BOX.get(),
                            ModBlocks.BRASS_STORAGE_BOX.get(),
                            ModBlocks.HARDENED_STORAGE_BOX.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<SimpleStorageBoxEntity>> SIMPLE_STORAGE_BOX_ENTITY =
            BLOCK_ENTITIES.register("simple_storage_box_entity", () ->
                    BlockEntityType.Builder.of(SimpleStorageBoxEntity::new,
                            ModBlocks.SIMPLE_STORAGE_BOX.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_ACACIA.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_CHERRY.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON.get(),
                            ModBlocks.SIMPLE_STORAGE_BOX_WARPED.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<BackpackEntity>> BACK_PACK_ENTITY =
            BLOCK_ENTITIES.register("back_pack_entity", () ->
                    BlockEntityType.Builder.of(BackpackEntity::new,
                            ModBlocks.BACK_PACK.get(),
                            ModBlocks.ANDESITE_BACK_PACK.get(),
                            ModBlocks.COPPER_BACK_PACK.get(),
                            ModBlocks.BRASS_BACK_PACK.get(),
                            ModBlocks.HARDENED_BACK_PACK.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<StorageControllerEntity>> STORAGE_CONTROLLER_ENTITY =
            BLOCK_ENTITIES.register("storage_controller_entity", () ->
                    BlockEntityType.Builder.of(StorageControllerEntity::new,
                            ModBlocks.STORAGE_CONTROLLER.get()
                    ).build(null));
    public static final RegistryObject<BlockEntityType<StorageInterfaceEntity>> STORAGE_INTERFACE_ENTITY =
            BLOCK_ENTITIES.register("storage_interface_entity", () ->
                    BlockEntityType.Builder.of(StorageInterfaceEntity::new,
                            ModBlocks.STORAGE_INTERFACE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<PasserEntity>> PASSER_ENTITY =
            BLOCK_ENTITIES.register("passer_entity", () ->
                    BlockEntityType.Builder.of(PasserEntity::new,
                            ModBlocks.PASSER_BLOCK.get()
                    ).build(null));
    public static final RegistryObject<BlockEntityType<PasserSmartEntity>> SMART_PASSER_ENTITY =
            BLOCK_ENTITIES.register("smart_passer_entity", () ->
                    BlockEntityType.Builder.of(PasserSmartEntity::new,
                            ModBlocks.SMART_PASSER_BLOCK.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
