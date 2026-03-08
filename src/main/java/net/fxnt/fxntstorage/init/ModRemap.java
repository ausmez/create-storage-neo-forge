package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.ModifyRegistriesEvent;

import java.util.HashMap;
import java.util.Map;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

@EventBusSubscriber(modid = FXNTStorage.MOD_ID)
public class ModRemap {

    private static final Map<ResourceLocation, ResourceLocation> BLOCK_REMAP = new HashMap<>();

    static {
        // === SIMPLE_STORAGE_BOX RENAME === //
        BLOCK_REMAP.put(modLoc("simple_storage_box"), ModBlocks.SIMPLE_STORAGE_BOX_OAK.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_spruce"), ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_birch"), ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_jungle"), ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_acacia"), ModBlocks.SIMPLE_STORAGE_BOX_ACACIA.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_dark_oak"), ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_mangrove"), ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_cherry"), ModBlocks.SIMPLE_STORAGE_BOX_CHERRY.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_bamboo"), ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_crimson"), ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_warped"), ModBlocks.SIMPLE_STORAGE_BOX_WARPED.getId());
        BLOCK_REMAP.put(modLoc("simple_storage_box_pale_oak"), modLoc("pale_oak_simple_storage_box"));
        // === STORAGE_TRIM RENAME === //
        BLOCK_REMAP.put(modLoc("storage_trim"), ModBlocks.STORAGE_TRIM_OAK.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_spruce"), ModBlocks.STORAGE_TRIM_SPRUCE.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_birch"), ModBlocks.STORAGE_TRIM_BIRCH.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_jungle"), ModBlocks.STORAGE_TRIM_JUNGLE.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_acacia"), ModBlocks.STORAGE_TRIM_ACACIA.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_dark_oak"), ModBlocks.STORAGE_TRIM_DARK_OAK.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_mangrove"), ModBlocks.STORAGE_TRIM_MANGROVE.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_cherry"), ModBlocks.STORAGE_TRIM_CHERRY.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_bamboo"), ModBlocks.STORAGE_TRIM_BAMBOO.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_crimson"), ModBlocks.STORAGE_TRIM_CRIMSON.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_warped"), ModBlocks.STORAGE_TRIM_WARPED.getId());
        BLOCK_REMAP.put(modLoc("storage_trim_pale_oak"), modLoc("pale_oak_storage_trim"));
    }

    @SubscribeEvent
    public static void onModifyRegistries(ModifyRegistriesEvent event) {
        remapContent(event.getRegistry(Registries.BLOCK));
        remapContent(event.getRegistry(Registries.ITEM));
    }

    private static <T> void remapContent(Registry<T> registry) {
        BLOCK_REMAP.forEach((oldId, newId) -> {
            registry.addAlias(oldId, newId);
            FXNTStorage.LOGGER.debug("Remapping {} '{}' to '{}'", registry.key().location(), oldId, newId);
        });
    }
}
