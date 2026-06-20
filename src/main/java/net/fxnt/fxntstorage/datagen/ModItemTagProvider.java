package net.fxnt.fxntstorage.datagen;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, FXNTStorage.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        // Tags handled by Registrate:
        //   BACKPACK_ITEM, BACKPACK_UPGRADE_DEACTIVATED, STORAGE_BOX_ITEM, STORAGE_BOX_UPGRADE

        tag(ModTags.Items.BACKPACK_UPGRADE)
                .add(ModItems.BACKPACK_MAGNET_UPGRADE.get())
                .add(ModItems.BACKPACK_PICKBLOCK_UPGRADE.get())
                .add(ModItems.BACKPACK_ITEMPICKUP_UPGRADE.get())
                .add(ModItems.BACKPACK_FLIGHT_UPGRADE.get())
                .add(ModItems.BACKPACK_REFILL_UPGRADE.get())
                .add(ModItems.BACKPACK_FEEDER_UPGRADE.get())
                .add(ModItems.BACKPACK_TOOLSWAP_UPGRADE.get())
                .add(ModItems.BACKPACK_FALLDAMAGE_UPGRADE.get())
                .add(ModItems.BACKPACK_OREMINING_UPGRADE.get())
                .add(ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE.get())
                .add(ModItems.BACKPACK_JUKEBOX_UPGRADE.get())
                .add(ModItems.BACKPACK_HEALTH_UPGRADE.get())
                .add(ModItems.BACKPACK_CRAFTING_UPGRADE.get())
                .add(ModItems.BACKPACK_WORKSHOP_UPGRADE.get())
                .addTag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED);

        tag(ModTags.Items.CURIOS_BACK).addTag(ModTags.Items.BACKPACK_ITEM);
    }
}
