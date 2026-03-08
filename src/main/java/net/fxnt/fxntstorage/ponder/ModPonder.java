package net.fxnt.fxntstorage.ponder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ModPonder {
    public static final ResourceLocation CREATE_STORAGE = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "storage");

    public static class Tags {

        public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {

            helper.registerTag(CREATE_STORAGE)
                    .addToIndex()
                    .item(ModBlocks.SIMPLE_STORAGE_BOX_OAK.get(), true, false)
                    .title("Create: Storage")
                    .description("Items and components related to Create: Storage")
                    .register();

            PonderTagRegistrationHelper<RegistryEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
            HELPER.addToTag(CREATE_STORAGE)
                    .add(ModBlocks.STORAGE_BOX)
                    .add(ModBlocks.SIMPLE_STORAGE_BOX_OAK)
                    .add(ModBlocks.PASSER_BLOCK)
                    .add(ModBlocks.SMART_PASSER_BLOCK)
                    .add(ModBlocks.STORAGE_CONTROLLER)
                    .add(ModBlocks.STORAGE_INTERFACE)
                    .add(ModBlocks.STORAGE_INTERFACE_FILTERED)
                    .add(ModBlocks.STORAGE_TRIM_OAK)
                    .add(ModItems.STORAGE_BOX_VOID_UPGRADE)
                    .add(ModItems.STORAGE_BOX_CAPACITY_UPGRADE);
        }
    }

    public static class Scenes {

        public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
            PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

            HELPER.forComponents(ModBlocks.STORAGE_BOX, ModBlocks.CARDBOARD_STORAGE_BOX, ModBlocks.WEATHERED_STORAGE_BOX, ModBlocks.ANDESITE_STORAGE_BOX, ModBlocks.COPPER_STORAGE_BOX, ModBlocks.BRASS_STORAGE_BOX, ModBlocks.HARDENED_STORAGE_BOX)
                    .addStoryBoard("storagebox/intro", StorageBoxScenes::intro, CREATE_STORAGE)
                    .addStoryBoard("storagebox/interact", StorageBoxScenes::interact, CREATE_STORAGE)
                    .addStoryBoard("storagebox/filter", StorageBoxScenes::filter, CREATE_STORAGE);

            List<ResourceLocation> simpleStorageBoxes = ForgeRegistries.BLOCKS.getEntries().stream()
                    .filter(entry -> entry.getKey().location().getNamespace().equals(FXNTStorage.MOD_ID)
                            && entry.getKey().location().getPath().contains("simple_storage_box")
                            && entry.getValue() instanceof SimpleStorageBox)
                    .map(entry -> entry.getKey().location())
                    .toList();

            MultiSceneBuilder builder = helper.forComponents(simpleStorageBoxes);

            if (ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT)) {
                builder.addStoryBoard("simplestoragebox/intro_alt", SimpleStorageBoxScenes::intro, CREATE_STORAGE);
            } else {
                builder.addStoryBoard("simplestoragebox/intro", SimpleStorageBoxScenes::intro, CREATE_STORAGE);
            }
            builder.addStoryBoard("simplestoragebox/interact", SimpleStorageBoxScenes::interact, CREATE_STORAGE)
                    .addStoryBoard("simplestoragebox/upgrades", SimpleStorageBoxScenes::upgrades, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.PASSER_BLOCK)
                    .addStoryBoard("passerblock/intro", PasserBlockScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.SMART_PASSER_BLOCK)
                    .addStoryBoard("smartpasserblock/intro", SmartPasserBlockScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.STORAGE_CONTROLLER)
                    .addStoryBoard("storagecontroller/intro", StorageControllerScenes::intro, CREATE_STORAGE)
                    .addStoryBoard("storagecontroller/highlight", StorageControllerScenes::highlighting, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.STORAGE_INTERFACE)
                    .addStoryBoard("storageinterface/intro", StorageInterfaceScenes::intro, CREATE_STORAGE);

            HELPER.forComponents(ModBlocks.STORAGE_INTERFACE_FILTERED)
                    .addStoryBoard("storageinterfacefiltered/intro", StorageInterfaceFilteredScenes::intro, CREATE_STORAGE)
                    .addStoryBoard("storageinterfacefiltered/filter", StorageInterfaceFilteredScenes::filter, CREATE_STORAGE);
        }
    }

}
