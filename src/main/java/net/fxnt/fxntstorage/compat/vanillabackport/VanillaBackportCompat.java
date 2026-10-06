package net.fxnt.fxntstorage.compat.vanillabackport;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VanillaBackportCompat {

    public static final ResourceLocation PALE_OAK_PLANKS = ResourceLocation.withDefaultNamespace("pale_oak_planks");
    public static final ResourceLocation POPLAR_PLANKS = ResourceLocation.withDefaultNamespace("poplar_planks");

    private VanillaBackportCompat() {
    }

    // Planks to copy block properties from, falling back to oak when they're not registered
    public static NonNullSupplier<Block> planksOrOak(ResourceLocation planksId) {
        return () -> BuiltInRegistries.BLOCK.getOptional(planksId).orElse(Blocks.OAK_PLANKS);
    }

    public static boolean planksExist(ResourceLocation planksId) {
        return BuiltInRegistries.BLOCK.containsKey(planksId);
    }

    // Box and trim items whose planks aren't registered (VanillaBackport missing or a version without that wood type)
    public static Set<Item> unavailableItems() {
        Map<ResourceLocation, List<BlockEntry<?>>> blocksByPlanks = Map.of(
                PALE_OAK_PLANKS, List.of(ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK, ModBlocks.STORAGE_TRIM_PALE_OAK),
                POPLAR_PLANKS, List.of(ModBlocks.SIMPLE_STORAGE_BOX_POPLAR, ModBlocks.STORAGE_TRIM_POPLAR)
        );
        Set<Item> items = new HashSet<>();
        blocksByPlanks.forEach((planksId, blocks) -> {
            if (!planksExist(planksId)) blocks.forEach(block -> items.add(block.asItem()));
        });
        return items;
    }
}
