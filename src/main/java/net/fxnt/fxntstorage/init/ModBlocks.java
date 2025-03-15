package net.fxnt.fxntstorage.init;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fxnt.fxntstorage.backpacks.main.BackpackBlock;
import net.fxnt.fxntstorage.backpacks.main.BackpackItem;
import net.fxnt.fxntstorage.containers.StorageBox;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.registry.SpriteShifts;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import static net.fxnt.fxntstorage.FXNTStorage.REGISTRATE;

public class ModBlocks {

    // STORAGE BOXES //
    public static final BlockEntry<StorageBox> STORAGE_BOX = REGISTRATE
            .block("storage_box", properties -> new StorageBox(properties, Util.IRON_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .item().properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<StorageBox> ANDESITE_STORAGE_BOX = REGISTRATE
            .block("andesite_storage_box", properties -> new StorageBox(properties, Util.ANDESITE_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .item().properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<StorageBox> COPPER_STORAGE_BOX = REGISTRATE
            .block("copper_storage_box", properties -> new StorageBox(properties, Util.COPPER_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .item().properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<StorageBox> BRASS_STORAGE_BOX = REGISTRATE
            .block("brass_storage_box", properties -> new StorageBox(properties, Util.BRASS_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .item().properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<StorageBox> HARDENED_STORAGE_BOX = REGISTRATE
            .block("hardened_storage_box", properties -> new StorageBox(properties, Util.HARDENED_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .item().properties(Item.Properties::fireResistant)
            .build()
            .register();


    // BACKPACKS //
    public static final BlockEntry<BackpackBlock> BACK_PACK = REGISTRATE
            .block("back_pack", properties -> new BackpackBlock(properties, Util.IRON_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .item(BackpackItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> ANDESITE_BACK_PACK = REGISTRATE
            .block("andesite_back_pack", properties -> new BackpackBlock(properties, Util.ANDESITE_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .item(BackpackItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> COPPER_BACK_PACK = REGISTRATE
            .block("copper_back_pack", properties -> new BackpackBlock(properties, Util.COPPER_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .item(BackpackItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> BRASS_BACK_PACK = REGISTRATE
            .block("brass_back_pack", properties -> new BackpackBlock(properties, Util.BRASS_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .item(BackpackItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> HARDENED_BACK_PACK = REGISTRATE
            .block("hardened_back_pack", properties -> new BackpackBlock(properties, Util.HARDENED_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .item(BackpackItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();


    // STORAGE CONTROLLER BLOCKS //
    public static final BlockEntry<StorageController> STORAGE_CONTROLLER = REGISTRATE
            .block("storage_controller", StorageController::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();

    public static final BlockEntry<StorageInterface> STORAGE_INTERFACE = REGISTRATE
            .block("storage_interface", StorageInterface::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .register();


    // PASSER BLOCKS //
    public static final BlockEntry<PasserBlock> PASSER_BLOCK = REGISTRATE
            .block("passer_block", properties -> new PasserBlock(properties, false))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties.strength(1.5f))
            .simpleItem()
            .register();

    public static final BlockEntry<PasserBlock> SMART_PASSER_BLOCK = REGISTRATE
            .block("smart_passer_block", properties -> new PasserBlock(properties, true))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties.strength(1.5f))
            .simpleItem()
            .register();


    // SIMPLE STORAGE BOXES //
    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX = REGISTRATE
            .block("simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.OAK_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_SPRUCE = REGISTRATE
            .block("simple_storage_box_spruce", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.SPRUCE_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_BIRCH = REGISTRATE
            .block("simple_storage_box_birch", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.BIRCH_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_JUNGLE = REGISTRATE
            .block("simple_storage_box_jungle", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.JUNGLE_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_ACACIA = REGISTRATE
            .block("simple_storage_box_acacia", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.ACACIA_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_DARK_OAK = REGISTRATE
            .block("simple_storage_box_dark_oak", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.DARK_OAK_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_MANGROVE = REGISTRATE
            .block("simple_storage_box_mangrove", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.MANGROVE_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_CHERRY = REGISTRATE
            .block("simple_storage_box_cherry", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.CHERRY_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_BAMBOO = REGISTRATE
            .block("simple_storage_box_bamboo", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.BAMBOO_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_CRIMSON = REGISTRATE
            .block("simple_storage_box_crimson", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.CRIMSON_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_WARPED = REGISTRATE
            .block("simple_storage_box_warped", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.WARPED_PLANKS)
            .item(SimpleStorageBoxItem::new)
            .properties(Item.Properties::fireResistant)
            .build()
            .register();


    // CASING BLOCKS //
    public static final BlockEntry<CasingBlock> STORAGE_TRIM = REGISTRATE
            .block("storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.OAK_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_SPRUCE = REGISTRATE
            .block("storage_trim_spruce", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.SPRUCE_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BIRCH = REGISTRATE
            .block("storage_trim_birch", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.BIRCH_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_JUNGLE = REGISTRATE
            .block("storage_trim_jungle", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.JUNGLE_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_ACACIA = REGISTRATE
            .block("storage_trim_acacia", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.ACACIA_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_DARK_OAK = REGISTRATE
            .block("storage_trim_dark_oak", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.DARK_OAK_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_MANGROVE = REGISTRATE
            .block("storage_trim_mangrove", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.MANGROVE_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CHERRY = REGISTRATE
            .block("storage_trim_cherry", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.CHERRY_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BAMBOO = REGISTRATE
            .block("storage_trim_bamboo", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.BAMBOO_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CRIMSON = REGISTRATE
            .block("storage_trim_crimson", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.CRIMSON_CASING))
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_WARPED = REGISTRATE
            .block("storage_trim_warped", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(BuilderTransformers.casing(() -> SpriteShifts.WARPED_CASING))
            .register();

    public static void register() {
    }
}
