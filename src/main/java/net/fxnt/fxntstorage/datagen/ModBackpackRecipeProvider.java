package net.fxnt.fxntstorage.datagen;

import com.simibubi.create.AllBlocks;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;


public class ModBackpackRecipeProvider extends RecipeProvider {

    public ModBackpackRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedBackpackRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ANDESITE_BACKPACK.get())
                .define('B', ModBlocks.BACKPACK)
                .define('C', AllBlocks.ANDESITE_CASING)
                .pattern("CCC")
                .pattern("CBC")
                .pattern("CCC")
                .group("backpack")
                .unlockedBy("has_backpack", RecipeProvider.has(ModBlocks.BACKPACK))
                .save(recipeOutput, modLoc("backpack_crafting/andesite_backpack_from_backpack"));

        ShapedBackpackRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.COPPER_BACKPACK.get())
                .define('B', ModBlocks.ANDESITE_BACKPACK)
                .define('C', AllBlocks.COPPER_CASING)
                .pattern("CCC")
                .pattern("CBC")
                .pattern("CCC")
                .group("backpack")
                .unlockedBy("has_backpack", RecipeProvider.has(ModBlocks.BACKPACK))
                .save(recipeOutput, modLoc("backpack_crafting/copper_backpack_from_andesite_backpack"));

        ShapedBackpackRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BRASS_BACKPACK.get())
                .define('B', ModBlocks.COPPER_BACKPACK)
                .define('C', AllBlocks.BRASS_CASING)
                .pattern("CCC")
                .pattern("CBC")
                .pattern("CCC")
                .group("backpack")
                .unlockedBy("has_backpack", RecipeProvider.has(ModBlocks.BACKPACK))
                .save(recipeOutput, modLoc("backpack_crafting/brass_backpack_from_copper_backpack"));

        ShapedBackpackRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HARDENED_BACKPACK.get())
                .define('B', ModBlocks.BRASS_BACKPACK)
                .define('C', AllBlocks.RAILWAY_CASING)
                .pattern("CCC")
                .pattern("CBC")
                .pattern("CCC")
                .group("backpack")
                .unlockedBy("has_backpack", RecipeProvider.has(ModBlocks.BACKPACK))
                .save(recipeOutput, modLoc("backpack_crafting/hardened_backpack_from_brass_backpack"));
    }

}
