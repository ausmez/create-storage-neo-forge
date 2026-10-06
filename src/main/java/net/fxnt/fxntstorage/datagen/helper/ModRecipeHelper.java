package net.fxnt.fxntstorage.datagen.helper;

import cn.mlus.thirst.content.registry.ItemInit;
import cn.mlus.thirst.content.registry.ThirstComponent;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeEnabledCondition;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.controller.StorageInterfaceFiltered;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModRecipeHelper {
    // Recipes for planks from optional VanillaBackport, gated on the planks existing, and skipped if they're absent at datagen
    private static void withOptionalPlanks(RegistrateRecipeProvider prov, ResourceLocation planksId, BiConsumer<RecipeOutput, Block> generator) {
        BuiltInRegistries.BLOCK.getOptional(planksId).ifPresentOrElse(
                planks -> generator.accept(prov.withConditions(new ItemExistsCondition(planksId)), planks),
                () -> FXNTStorage.LOGGER.warn("Skipping recipe datagen for missing planks {}", planksId));
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageBox>, RegistrateRecipeProvider> storageBox(Supplier<? extends Block> supplier) {
        return (ctx, prov) -> {
            Block casing = supplier.get();
            String path = BuiltInRegistries.BLOCK.getKey(casing).getPath();
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

    public static NonNullBiConsumer<DataGenContext<Block, SimpleStorageBox>, RegistrateRecipeProvider> simpleStorageBox(ResourceLocation optionalPlanksId) {
        return (ctx, prov) -> withOptionalPlanks(prov, optionalPlanksId, (output, planks) -> generateSimpleStorageBox(ctx, output, planks));
    }

    private static void generateSimpleStorageBox(DataGenContext<Block, SimpleStorageBox> ctx, RecipeOutput output, Block planks) {
        String path = BuiltInRegistries.BLOCK.getKey(planks).getPath();
        String woodType = path.substring(0, path.indexOf("_planks"));

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
                .save(output, modLoc("crafting_shaped/simple_storage_box/" + woodType + "_simple_storage_box"));
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateRecipeProvider> storageTrim(Block planks) {
        return (ctx, prov) -> genStorageTrim(ctx, prov, planks);
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateRecipeProvider> storageTrim(Supplier<? extends Block> planks) {
        return (ctx, prov) -> genStorageTrim(ctx, prov, planks.get());
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateRecipeProvider> storageTrim(ResourceLocation optionalPlanksId) {
        return (ctx, prov) -> withOptionalPlanks(prov, optionalPlanksId, (output, planks) -> genStorageTrim(ctx, output, planks));
    }

    public static void genStorageTrim(DataGenContext<Block, CasingBlock> ctx, RecipeOutput output, Block planks) {
        String path = BuiltInRegistries.BLOCK.getKey(planks).getPath();
        String woodType = path.substring(0, path.indexOf("_planks"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                .define('A', AllItems.ANDESITE_ALLOY)
                .define('W', planks)
                .pattern("AWA")
                .pattern("W W")
                .pattern("AWA")
                .group("storage_box")
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                .save(output, modLoc("crafting_shaped/storage_trim/" + woodType + "_storage_trim"));
    }

    public static NonNullBiConsumer<DataGenContext<Block, PasserBlock>, RegistrateRecipeProvider> passer(boolean isSmart) {
        return (ctx, prov) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('X', isSmart ? AllBlocks.SMART_CHUTE : AllBlocks.CHUTE)
                    .define('Y', Items.REDSTONE)
                    .define('Z', Items.HOPPER)
                    .pattern("X")
                    .pattern("Y")
                    .pattern("Z")
                    .group("storage_box")
                    .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(prov, modLoc("crafting_shaped/" + ctx.getName()));

            if (isSmart) {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                        .requires(ModBlocks.PASSER_BLOCK)
                        .requires(AllItems.BRASS_SHEET)
                        .requires(AllItems.ELECTRON_TUBE)
                        .group("storage_box")
                        .unlockedBy("has_passer_block", RegistrateRecipeProvider.has(ModBlocks.PASSER_BLOCK))
                        .save(prov, modLoc("smart_passer_block_from_passer"));
            }
        };
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

    // Drop the recipe when a modpack adds the upgrade to disabled_backpack_upgrades
    public static RecipeOutput enabledUpgrade(RecipeOutput output, Item upgrade) {
        return output.withConditions(new UpgradeEnabledCondition(upgrade));
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
                .save(enabledUpgrade(prov, ctx.get()), modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName()));
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
                .save(enabledUpgrade(prov, ctx.get()), modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName()));
    }

    public static NonNullBiConsumer<DataGenContext<Item, UpgradeItem>, RegistrateRecipeProvider> thirstUpgradeItem() {
        return (ctx, prov) -> {
            // The ingredients need Thirst's registered objects. Fail rather than silently drop the recipe,
            // which is how it went missing from 1.3.5. Thirst stays an optional dependency for players.
            if (!FXNTStorage.THIRST_LOADED)
                throw new IllegalStateException("Thirst Was Reclaimed must be on the runtime classpath to generate " + ctx.getName());

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('R', AllBlocks.REDSTONE_LINK)
                    .define('I', AllItems.IRON_SHEET)
                    .define('X', DataComponentIngredient.of(false, ThirstComponent.PURITY, 1, ItemInit.TERRACOTTA_WATER_BOWL))
                    .define('Y', DataComponentIngredient.of(false, ThirstComponent.PURITY, 2, ItemInit.TERRACOTTA_WATER_BOWL))
                    .define('Z', DataComponentIngredient.of(false, ThirstComponent.PURITY, 3, ItemInit.TERRACOTTA_WATER_BOWL))
                    .pattern(" R ")
                    .pattern("IBI")
                    .pattern("XYZ")
                    .group("backpack")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov.withConditions(new ModLoadedCondition(ModCompats.THIRST_WAS_RECLAIMED), new UpgradeEnabledCondition(ctx.get())),
                            modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName()));
        };
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

    public static NonNullBiConsumer<DataGenContext<Block, ReserveStorageBox>, RegistrateRecipeProvider> reserveStorageBox() {
        return (ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                .define('C', Blocks.CHEST)
                .define('D', AllBlocks.DISPLAY_BOARD)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('W', ItemTags.PLANKS)
                .pattern("WSW")
                .pattern("WCW")
                .pattern("WDW")
                .group("storage_box")
                .unlockedBy("has_industrial_iron", RegistrateRecipeProvider.has(AllBlocks.INDUSTRIAL_IRON_BLOCK))
                .save(prov, modLoc("crafting_shaped/" + ctx.getName()));
    }
}
