package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.HashMap;
import java.util.Map;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onMissingMappings(MissingMappingsEvent event) {
        remapContent(event, Registries.BLOCK, ForgeRegistries.BLOCKS);
        remapContent(event, Registries.ITEM, ForgeRegistries.ITEMS);
    }

    private static <T> void remapContent(MissingMappingsEvent event, ResourceKey<Registry<T>> registry, IForgeRegistry<T> forgeRegistry) {
        for (MissingMappingsEvent.Mapping<T> oldId : event.getAllMappings(registry)) {
            ResourceLocation newId = BLOCK_REMAP.get(oldId.getKey());
            if (newId != null) {
                FXNTStorage.LOGGER.debug("Remapping {} '{}' to '{}'", registry.location(), oldId.getKey(), newId);
                oldId.remap(forgeRegistry.getValue(newId));
            }
        }
    }
}
