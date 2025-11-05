package net.fxnt.fxntstorage.init;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.datagen.helper.ModRecipeHelper;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModItems {
    private static final CreateRegistrate REGISTRATE = FXNTStorage.REGISTRATE;

    static {
        REGISTRATE.setCreativeTab(ModTabs.CREATIVE_MODE_TAB);
    }

    // Simple storage box upgrade items
    public static final ItemEntry<UpgradeItem> STORAGE_BOX_CAPACITY_UPGRADE = REGISTRATE
            .item("storage_box_capacity_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_CAPACITY_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('V', AllBlocks.ITEM_VAULT)
                    .pattern(" V ")
                    .pattern("VBV")
                    .pattern(" V ")
                    .group("storage_box")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov, modLoc("crafting_shaped/" + ctx.getName())))
            .tag(ModTags.Items.STORAGE_BOX_UPGRADE)
            .register();

    public static final ItemEntry<UpgradeItem> STORAGE_BOX_VOID_UPGRADE = REGISTRATE
            .item("storage_box_void_upgrade", properties -> new UpgradeItem(properties, Util.STORAGE_BOX_VOID_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .recipe((ctx, prov) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                    .requires(ModItems.BACKPACK_BLANK_UPGRADE)
                    .requires(Items.FLINT_AND_STEEL)
                    .group("storage_box")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov))
            .tag(ModTags.Items.STORAGE_BOX_UPGRADE)
            .register();


    // Backpack upgrade items
    public static final ItemEntry<UpgradeItem> BACKPACK_BLANK_UPGRADE = REGISTRATE
            .item("backpack_blank_upgrade", properties -> new UpgradeItem(properties, Util.BLANK_UPGRADE))
            .properties(properties -> properties.stacksTo(16))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get(), 4)
                    .define('A', AllItems.ANDESITE_ALLOY)
                    .define('S', AllItems.EMPTY_SCHEMATIC)
                    .define('-', AllItems.IRON_SHEET)
                    .pattern("A-A")
                    .pattern("-S-")
                    .pattern("A-A")
                    .group("backpack")
                    .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY))
                    .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_MAGNET_UPGRADE = REGISTRATE
            .item("backpack_magnet_upgrade", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe(ModRecipeHelper.backpackUpgradeItem(AllItems.EXTENDO_GRIP))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_MAGNET_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_magnet_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.MAGNET_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_PICKBLOCK_UPGRADE = REGISTRATE
            .item("backpack_pickblock_upgrade", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe(ModRecipeHelper.backpackUpgradeBlock(AllBlocks.DEPLOYER))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_PICKBLOCK_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_pickblock_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.PICKBLOCK_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_ITEMPICKUP_UPGRADE = REGISTRATE
            .item("backpack_itempickup_upgrade", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe(ModRecipeHelper.backpackUpgradeBlock(AllBlocks.SMART_CHUTE))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_ITEMPICKUP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_itempickup_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.ITEMPICKUP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FLIGHT_UPGRADE = REGISTRATE
            .item("backpack_flight_upgrade", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe((ctx, prov) -> MechanicalCraftingRecipeBuilder.shapedRecipe(ctx.get())
                    .key('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .key('I', AllItems.IRON_SHEET)
                    .key('T', AllItems.NETHERITE_BACKTANK)
                    .key('F', AllBlocks.ENCASED_FAN)
                    .key('P', AllItems.PROPELLER)
                    .key('N', AllBlocks.NOZZLE)
                    .key('V', AllBlocks.COPPER_VALVE_HANDLE)
                    .key('#', AllItems.BRASS_SHEET)
                    .patternLine(" #V# ")
                    .patternLine("#PIP#")
                    .patternLine("#TBT#")
                    .patternLine("#FIF#")
                    .patternLine(" #N# ")
                    .build(prov, modLoc("mechanical_crafting/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FLIGHT_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_flight_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FLIGHT_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_REFILL_UPGRADE = REGISTRATE
            .item("backpack_refill_upgrade", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe(ModRecipeHelper.backpackUpgradeItem(AllItems.BRASS_HAND))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_REFILL_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_refill_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.REFILL_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FEEDER_UPGRADE = REGISTRATE
            .item("backpack_feeder_upgrade", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('R', AllBlocks.REDSTONE_LINK)
                    .define('I', AllItems.IRON_SHEET)
                    .define('X', AllItems.HONEYED_APPLE)
                    .define('Y', AllItems.CHOCOLATE_BERRIES)
                    .define('Z', AllItems.SWEET_ROLL)
                    .pattern(" R ")
                    .pattern("IBI")
                    .pattern("XYZ")
                    .group("backpack")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FEEDER_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_feeder_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FEEDER_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TOOLSWAP_UPGRADE = REGISTRATE
            .item("backpack_toolswap_upgrade", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('R', AllBlocks.REDSTONE_LINK)
                    .define('I', AllItems.IRON_SHEET)
                    .define('X', Items.IRON_SWORD)
                    .define('Y', Items.IRON_PICKAXE)
                    .define('Z', Items.IRON_HOE)
                    .pattern(" R ")
                    .pattern("IBI")
                    .pattern("XYZ")
                    .group("backpack")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TOOLSWAP_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_toolswap_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.TOOLSWAP_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FALLDAMAGE_UPGRADE = REGISTRATE
            .item("backpack_falldamage_upgrade", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('S', AllBlocks.SAIL)
                    .define('I', AllItems.IRON_SHEET)
                    .define('P', AllItems.PROPELLER)
                    .define('X', Items.COBWEB)
                    .define('Y', AllItems.COPPER_DIVING_BOOTS)
                    .pattern("PSP")
                    .pattern("IBI")
                    .pattern("XYX")
                    .group("backpack")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_FALLDAMAGE_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_falldamage_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.FALLDAMAGE_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_OREMINING_UPGRADE = REGISTRATE
            .item("backpack_oremining_upgrade", properties -> new UpgradeItem(properties, Util.OREMINING_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe(ModRecipeHelper.backpackUpgradeBlock(AllBlocks.MECHANICAL_DRILL))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_OREMINING_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_oremining_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.OREMINING_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TORCHDEPLOYER_UPGRADE = REGISTRATE
            .item("backpack_torchdeployer_upgrade", properties -> new UpgradeItem(properties, Util.TORCHDEPLOYER_UPGRADE))
            .properties(properties -> properties.stacksTo(1))
            .recipe((ctx, prov) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                    .define('B', ModItems.BACKPACK_BLANK_UPGRADE)
                    .define('R', AllBlocks.REDSTONE_LINK)
                    .define('I', AllItems.IRON_SHEET)
                    .define('X', Items.TORCH)
                    .define('C', AllBlocks.COGWHEEL)
                    .pattern(" R ")
                    .pattern("IBI")
                    .pattern("CXC")
                    .group("backpack")
                    .unlockedBy("has_blank_upgrade", RegistrateRecipeProvider.has(ModItems.BACKPACK_BLANK_UPGRADE))
                    .save(prov, modLoc("crafting_shaped/backpack_upgrade/" + ctx.getName())))
            .register();

    public static final ItemEntry<UpgradeItem> BACKPACK_TORCHDEPLOYER_UPGRADE_DEACTIVATED = REGISTRATE
            .item("backpack_torchdeployer_upgrade_deactivated", properties -> new UpgradeItem(properties, Util.TORCHDEPLOYER_UPGRADE_DEACTIVATED))
            .properties(properties -> properties.stacksTo(1))
            .tag(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED)
            .register();

    public static void register() {
    }

}
