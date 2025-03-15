package net.fxnt.fxntstorage.init;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderTag;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.ponder.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModPonder {
    public static final PonderTag STORAGE = new PonderTag(new ResourceLocation(FXNTStorage.MOD_ID, "storage"))
            .defaultLang("Storage Related", "Items and components related to Create Storage")
            .item(ModBlocks.SIMPLE_STORAGE_BOX.get(), true, false)
            .addToIndex();

    private static final CreateRegistrate createRegistrate = CreateRegistrate.create(FXNTStorage.MOD_ID);
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(FXNTStorage.MOD_ID);

    public static final BlockEntry<Block> STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.STORAGE_BOX);
    public static final BlockEntry<Block> ANDESITE_STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.ANDESITE_STORAGE_BOX);
    public static final BlockEntry<Block> COPPER_STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.COPPER_STORAGE_BOX);
    public static final BlockEntry<Block> BRASS_STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.BRASS_STORAGE_BOX);
    public static final BlockEntry<Block> HARDENED_STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.HARDENED_STORAGE_BOX);

    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_ACACIA = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_ACACIA);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_BAMBOO = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_BIRCH = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_BIRCH);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_CHERRY = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_CHERRY);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_CRIMSON = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_DARK_OAK = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_JUNGLE = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_MANGROVE = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_SPRUCE = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE);
    public static final BlockEntry<Block> SIMPLE_STORAGE_BOX_WARPED = new BlockEntry<>(createRegistrate, ModBlocks.SIMPLE_STORAGE_BOX_WARPED);

    public static final BlockEntry<Block> PASSER_BLOCK = new BlockEntry<>(createRegistrate, ModBlocks.PASSER_BLOCK);
    public static final BlockEntry<Block> SMART_PASSER_BLOCK = new BlockEntry<>(createRegistrate, ModBlocks.SMART_PASSER_BLOCK);

    public static final BlockEntry<Block> STORAGE_CONTROLLER = new BlockEntry<>(createRegistrate, ModBlocks.STORAGE_CONTROLLER);
    public static final BlockEntry<Block> STORAGE_INTERFACE = new BlockEntry<>(createRegistrate, ModBlocks.STORAGE_INTERFACE);


    public static void register() {
        HELPER.forComponents(STORAGE_BOX, ANDESITE_STORAGE_BOX, COPPER_STORAGE_BOX, BRASS_STORAGE_BOX, HARDENED_STORAGE_BOX)
                .addStoryBoard("storagebox/intro", StorageBoxScenes::intro, STORAGE)
                .addStoryBoard("storagebox/interact", StorageBoxScenes::interact, STORAGE)
                .addStoryBoard("storagebox/filter", StorageBoxScenes::filter, STORAGE);

        HELPER.forComponents(SIMPLE_STORAGE_BOX, SIMPLE_STORAGE_BOX_ACACIA, SIMPLE_STORAGE_BOX_BAMBOO, SIMPLE_STORAGE_BOX_BIRCH,
                SIMPLE_STORAGE_BOX_CHERRY, SIMPLE_STORAGE_BOX_CRIMSON, SIMPLE_STORAGE_BOX_DARK_OAK, SIMPLE_STORAGE_BOX_JUNGLE,
                SIMPLE_STORAGE_BOX_MANGROVE, SIMPLE_STORAGE_BOX_SPRUCE, SIMPLE_STORAGE_BOX_WARPED)
                .addStoryBoard("simplestoragebox/intro", SimpleStorageBoxScenes::intro, STORAGE)
                .addStoryBoard("simplestoragebox/interact", SimpleStorageBoxScenes::interact, STORAGE)
                .addStoryBoard("simplestoragebox/upgrades", SimpleStorageBoxScenes::upgrades, STORAGE);

        HELPER.forComponents(PASSER_BLOCK)
                .addStoryBoard("passerblock/intro", PasserBlockScenes::intro, STORAGE);

        HELPER.forComponents(SMART_PASSER_BLOCK)
                .addStoryBoard("smartpasserblock/intro", SmartPasserBlockScenes::intro, STORAGE);

        HELPER.forComponents(STORAGE_CONTROLLER)
                .addStoryBoard("storagecontroller/intro", StorageControllerScenes::intro, STORAGE);

        HELPER.forComponents(STORAGE_INTERFACE)
                .addStoryBoard("storageinterface/intro", StorageInterfaceScenes::intro, STORAGE);

    }

    public static void registerTags() {
        PonderRegistry.TAGS.forTag(STORAGE)
                .add(STORAGE_BOX)
                .add(SIMPLE_STORAGE_BOX)
                .add(PASSER_BLOCK)
                .add(SMART_PASSER_BLOCK)
                .add(STORAGE_CONTROLLER)
                .add(STORAGE_INTERFACE)
                .add(ModBlocks.STORAGE_TRIM)
                .add(ModItems.STORAGE_BOX_VOID_UPGRADE.get())
                .add(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())
                .add(ModBlocks.PASSER_BLOCK.get());
    }
}
