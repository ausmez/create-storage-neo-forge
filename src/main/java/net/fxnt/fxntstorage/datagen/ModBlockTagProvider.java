package net.fxnt.fxntstorage.datagen;

import com.simibubi.create.AllTags;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, FXNTStorage.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        // Tags handled by Registrate:
        //   BACKPACK, SIMPLE_STORAGE_BOX, STORAGE_BOX, STORAGE_TRIM

        tag(ModTags.Blocks.SIMPLE_STORAGE_BOX)
                .add(ModBlocks.SIMPLE_STORAGE_BOX_OAK.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_ACACIA.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_CHERRY.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON.get())
                .add(ModBlocks.SIMPLE_STORAGE_BOX_WARPED.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "pale_oak_simple_storage_box"));

        tag(ModTags.Blocks.STORAGE_TRIM)
                .add(ModBlocks.STORAGE_TRIM_OAK.get())
                .add(ModBlocks.STORAGE_TRIM_SPRUCE.get())
                .add(ModBlocks.STORAGE_TRIM_BIRCH.get())
                .add(ModBlocks.STORAGE_TRIM_JUNGLE.get())
                .add(ModBlocks.STORAGE_TRIM_ACACIA.get())
                .add(ModBlocks.STORAGE_TRIM_DARK_OAK.get())
                .add(ModBlocks.STORAGE_TRIM_MANGROVE.get())
                .add(ModBlocks.STORAGE_TRIM_CHERRY.get())
                .add(ModBlocks.STORAGE_TRIM_BAMBOO.get())
                .add(ModBlocks.STORAGE_TRIM_CRIMSON.get())
                .add(ModBlocks.STORAGE_TRIM_WARPED.get())
                .addOptional(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "pale_oak_storage_trim"));

        tag(ModTags.Blocks.BREAKABLE_WITH_ANY_TOOL)
                .addTag(Tags.Blocks.GLASS)
                .addTag(Tags.Blocks.GLASS_PANES)
                .addTag(BlockTags.BEDS)
                .addTag(BlockTags.CANDLES)
                .addTag(BlockTags.CORALS)
                .addTag(BlockTags.WOOL_CARPETS)
                .addTag(BlockTags.WOOL)
                .add(Blocks.BEACON)
                .add(Blocks.HONEYCOMB_BLOCK)
                .add(Blocks.LEVER)
                .add(Blocks.TURTLE_EGG)
                .add(Blocks.SNIFFER_EGG)
                .add(Blocks.CACTUS)
                .add(Blocks.OCHRE_FROGLIGHT)
                .add(Blocks.VERDANT_FROGLIGHT)
                .add(Blocks.PEARLESCENT_FROGLIGHT)
                .add(Blocks.GLOWSTONE)
                .add(Blocks.REDSTONE_LAMP)
                .add(Blocks.SEA_LANTERN)
                .add(Blocks.DRAGON_EGG);

        tag(ModTags.Blocks.ORE_MINING_BLOCK).addTag(Tags.Blocks.ORES);

        tag(ModTags.Blocks.STORAGE_NETWORK_BLOCK)
                .addTag(ModTags.Blocks.SIMPLE_STORAGE_BOX)
                .addTag(ModTags.Blocks.STORAGE_TRIM)
                .add(ModBlocks.STORAGE_CONTROLLER.get())
                .add(ModBlocks.STORAGE_INTERFACE.get())
                .add(ModBlocks.STORAGE_INTERFACE_FILTERED.get());

        tag(ModTags.Blocks.SYMMETRY_WAND_BLACKLIST)
                .addTag(ModTags.Blocks.BACKPACK)
                .addTag(ModTags.Blocks.STORAGE_BOX)
                .addTag(ModTags.Blocks.SIMPLE_STORAGE_BOX)
                .addTag(BlockTags.SHULKER_BOXES)
                .addTag(AllTags.AllBlockTags.TOOLBOXES.tag);

        tag(ModTags.Blocks.WRENCH_PICKUP)
                .addTag(ModTags.Blocks.STORAGE_BOX)
                .addTag(ModTags.Blocks.SIMPLE_STORAGE_BOX)
                .add(ModBlocks.STORAGE_CONTROLLER.get())
                .add(ModBlocks.STORAGE_INTERFACE.get())
                .add(ModBlocks.STORAGE_INTERFACE_FILTERED.get())
                .add(ModBlocks.PASSER_BLOCK.get())
                .add(ModBlocks.SMART_PASSER_BLOCK.get());

        tag(ModTags.Blocks.TOMS_STORAGE_INV_CONN_SKIP)
                .add(ModBlocks.STORAGE_CONTROLLER.get())
                .add(ModBlocks.STORAGE_INTERFACE.get())
                .add(ModBlocks.STORAGE_INTERFACE_FILTERED.get());

        tag(ModTags.Blocks.CARRYON_BLACKLIST_BLOCK)
                .addTag(ModTags.Blocks.BACKPACK)
                .addTag(ModTags.Blocks.STORAGE_BOX)
                .addTag(ModTags.Blocks.STORAGE_NETWORK_BLOCK)
                .add(ModBlocks.PASSER_BLOCK.get())
                .add(ModBlocks.SMART_PASSER_BLOCK.get());

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.CARDBOARD_STORAGE_BOX.get())
                .addTag(ModTags.Blocks.SIMPLE_STORAGE_BOX)
                .addTag(ModTags.Blocks.STORAGE_TRIM);

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.PASSER_BLOCK.get())
                .add(ModBlocks.SMART_PASSER_BLOCK.get())
                .add(ModBlocks.STORAGE_CONTROLLER.get())
                .add(ModBlocks.STORAGE_INTERFACE.get())
                .add(ModBlocks.STORAGE_INTERFACE_FILTERED.get())
                .addTag(ModTags.Blocks.STORAGE_BOX);

        tag(AllTags.AllBlockTags.CASING.tag)
                .addTag(ModTags.Blocks.STORAGE_TRIM);

    }
}
