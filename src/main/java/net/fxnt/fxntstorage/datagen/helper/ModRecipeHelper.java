package net.fxnt.fxntstorage.datagen.helper;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.controller.StorageInterfaceFiltered;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModRecipeHelper {

    public static NonNullBiConsumer<DataGenContext<Block, StorageBox>, RegistrateRecipeProvider> storageBox(Supplier<? extends Block> supplier) {
        return (ctx, prov) -> {
            Block casing = supplier.get();
            String path = ForgeRegistries.BLOCKS.getKey(casing).getPath();
            String casingName = casing.equals(AllBlocks.INDUSTRIAL_IRON_BLOCK.get())
                    ? ""
                    : path.substring(0, path.indexOf("_")).replace("railway", "hardened") + "_";

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('C', casing)
                    .define('D', AllBlocks.DISPLAY_BOARD)
                    .define('V', AllBlocks.ITEM_VAULT)
                    .pattern("CCC")
                    .pattern("CVC")
                    .pattern("CDC")
                    .group("storage_box")
                    .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(prov, modLoc("crafting_shaped/storage_box/" + casingName + "storage_box"));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, SimpleStorageBox>, RegistrateRecipeProvider> simpleStorageBox(Block planks) {
        return (ctx, prov) -> generateSimpleStorageBox(ctx, prov, planks);
    }

    public static NonNullBiConsumer<DataGenContext<Block, SimpleStorageBox>, RegistrateRecipeProvider> simpleStorageBox(Supplier<? extends Block> planks) {
        return (ctx, prov) -> generateSimpleStorageBox(ctx, prov, planks.get());
    }

    private static void generateSimpleStorageBox(DataGenContext<Block, SimpleStorageBox> ctx, RegistrateRecipeProvider prov, Block planks) {
        String path = ForgeRegistries.BLOCKS.getKey(planks).getPath();
        String woodType = path.substring(0, path.indexOf("_planks"));

        Consumer<FinishedRecipe> conditionalConsumer = recipe -> {
            ConditionalRecipe.builder()
                    .addCondition(new ModLoadedCondition("vanillabackport"))
                    .addRecipe(recipe)
                    .build(prov, modLoc("crafting_shaped/simple_storage_box/" + woodType + "_simple_storage_box"));
        };

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('A', AllItems.ANDESITE_ALLOY)
                .define('D', AllBlocks.DISPLAY_BOARD)
                .define('V', AllBlocks.ITEM_VAULT)
                .define('W', planks)
                .pattern("AWA")
                .pattern("WVW")
                .pattern("ADA")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save((woodType.equals("pale_oak")) ? conditionalConsumer : prov, modLoc("crafting_shaped/simple_storage_box/" + woodType + "_simple_storage_box"));
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateRecipeProvider> storageTrim(Block planks) {
        return (ctx, prov) -> genStorageTrim(ctx, prov, planks);
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateRecipeProvider> storageTrim(Supplier<? extends Block> planks) {
        return (ctx, prov) -> genStorageTrim(ctx, prov, planks.get());
    }

    public static void genStorageTrim(DataGenContext<Block, CasingBlock> ctx, RegistrateRecipeProvider prov, Block planks) {
        String path = ForgeRegistries.BLOCKS.getKey(planks).getPath();
        String woodType = path.substring(0, path.indexOf("_planks"));

        Consumer<FinishedRecipe> conditionalConsumer = recipe -> {
            ConditionalRecipe.builder()
                    .addCondition(new ModLoadedCondition("vanillabackport"))
                    .addRecipe(recipe)
                    .build(prov, modLoc("crafting_shaped/storage_trim/" + woodType + "_storage_trim"));
        };

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                .define('A', AllItems.ANDESITE_ALLOY)
                .define('W', planks)
                .pattern("AWA")
                .pattern("W W")
                .pattern("AWA")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save((woodType.equals("pale_oak")) ? conditionalConsumer : prov, modLoc("crafting_shaped/storage_trim/" + woodType + "_storage_trim"));
    }

    public static NonNullBiConsumer<DataGenContext<Block, PasserBlock>, RegistrateRecipeProvider> passer(boolean isSmart) {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('X', isSmart ? AllBlocks.SMART_CHUTE : AllBlocks.CHUTE)
                .define('Y', Items.REDSTONE)
                .define('Z', Items.HOPPER)
                .pattern("X")
                .pattern("Y")
                .pattern("Z")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Block, BackpackBlock>, RegistrateRecipeProvider> backpack() {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('C', AllBlocks.INDUSTRIAL_IRON_BLOCK)
                .define('L', Items.LEATHER)
                .define('S', Items.STRING)
                .define('V', AllBlocks.ITEM_VAULT)
                .pattern("SCS")
                .pattern("LVL")
                .pattern("CCC")
                .group("backpack")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Item, UpgradeItem>, RegistrateRecipeProvider> backpackUpgradeBlock(Supplier<? extends Block> supplier) {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                .define('R', AllBlocks.REDSTONE_LINK)
                .define('I', AllItems.IRON_SHEET)
                .define('X', supplier.get())
                .pattern(" R ")
                .pattern("IBI")
                .pattern(" X ")
                .group("backpack")
                .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Item, UpgradeItem>, RegistrateRecipeProvider> backpackUpgradeItem(Supplier<? extends Item> supplier) {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                .define('R', AllBlocks.REDSTONE_LINK)
                .define('I', AllItems.IRON_SHEET)
                .define('X', supplier.get())
                .pattern(" R ")
                .pattern("IBI")
                .pattern(" X ")
                .group("backpack")
                .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageController>, RegistrateRecipeProvider> storageController() {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('X', AllBlocks.REDSTONE_LINK)
                .define('Y', AllBlocks.BRASS_CASING)
                .define('Z', AllItems.ELECTRON_TUBE)
                .pattern("X")
                .pattern("Y")
                .pattern("Z")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageInterface>, RegistrateRecipeProvider> storageInterface() {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('X', AllBlocks.REDSTONE_LINK)
                .define('Y', AllBlocks.BRASS_CASING)
                .pattern("X")
                .pattern("Y")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageInterfaceFiltered>, RegistrateRecipeProvider> storageInterfaceFiltered() {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('X', AllBlocks.REDSTONE_LINK)
                .define('Y', AllBlocks.BRASS_CASING)
                .define('Z', Items.COMPARATOR)
                .pattern("X")
                .pattern("Y")
                .pattern("Z")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }
}
