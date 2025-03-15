package net.fxnt.fxntstorage.ponder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModItems;
import net.minecraft.resources.ResourceLocation;


public class ModPonder {
    public static final ResourceLocation CREATE_STORAGE = new ResourceLocation(FXNTStorage.MOD_ID, "storage");

    public static class Tags {

        public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {

            helper.registerTag(CREATE_STORAGE)
                    .addToIndex()
                    .item(ModBlocks.SIMPLE_STORAGE_BOX.get(), true, false)
                    .title("Create Storage")
                    .description("Items and components related to Create Storage")
                    .register();

            PonderTagRegistrationHelper<RegistryEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
            HELPER.addToTag(CREATE_STORAGE)
                    .add(ModBlocks.STORAGE_BOX)
                    .add(ModBlocks.SIMPLE_STORAGE_BOX)
                    .add(ModBlocks.PASSER_BLOCK)
                    .add(ModBlocks.SMART_PASSER_BLOCK)
                    .add(ModBlocks.STORAGE_CONTROLLER)
                    .add(ModBlocks.STORAGE_INTERFACE)
                    .add(ModBlocks.STORAGE_TRIM)
                    .add(ModItems.STORAGE_BOX_VOID_UPGRADE)
                    .add(ModItems.STORAGE_BOX_CAPACITY_UPGRADE);

        }

    }

    public static class Scenes {

        public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
            PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

            HELPER.forComponents(ModBlocks.STORAGE_BOX, ModBlocks.ANDESITE_STORAGE_BOX, ModBlocks.COPPER_STORAGE_BOX, ModBlocks.BRASS_STORAGE_BOX, ModBlocks.HARDENED_STORAGE_BOX)
                    .addStoryBoard("storagebox/intro", StorageBoxScenes::intro, CREATE_STORAGE)
                    .addStoryBoard("storagebox/interact", StorageBoxScenes::interact, CREATE_STORAGE)
                    .addStoryBoard("storagebox/filter", StorageBoxScenes::filter, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.SIMPLE_STORAGE_BOX, ModBlocks.SIMPLE_STORAGE_BOX_ACACIA, ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO, ModBlocks.SIMPLE_STORAGE_BOX_BIRCH,
                            ModBlocks.SIMPLE_STORAGE_BOX_CHERRY, ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON, ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK, ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE,
                            ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE, ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE, ModBlocks.SIMPLE_STORAGE_BOX_WARPED)
                    .addStoryBoard("simplestoragebox/intro", SimpleStorageBoxScenes::intro, CREATE_STORAGE)
                    .addStoryBoard("simplestoragebox/interact", SimpleStorageBoxScenes::interact, CREATE_STORAGE)
                    .addStoryBoard("simplestoragebox/upgrades", SimpleStorageBoxScenes::upgrades, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.PASSER_BLOCK)
                    .addStoryBoard("passerblock/intro", PasserBlockScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.SMART_PASSER_BLOCK)
                    .addStoryBoard("smartpasserblock/intro", SmartPasserBlockScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.STORAGE_CONTROLLER)
                    .addStoryBoard("storagecontroller/intro", StorageControllerScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.STORAGE_INTERFACE)
                    .addStoryBoard("storageinterface/intro", StorageInterfaceScenes::intro, CREATE_STORAGE);


        }
    }

}
