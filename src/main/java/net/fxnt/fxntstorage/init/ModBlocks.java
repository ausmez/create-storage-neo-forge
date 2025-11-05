package net.fxnt.fxntstorage.init;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.StorageBoxItem;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMovementBehaviour;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.datagen.helper.*;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.registry.SpriteShifts;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMovementBehaviour;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.ModList;

import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;

public class ModBlocks {
    private static final CreateRegistrate REGISTRATE = FXNTStorage.REGISTRATE;

    static {
        REGISTRATE.setCreativeTab(ModTabs.CREATIVE_MODE_TAB);
    }

    // STORAGE BOXES //
    public static final BlockEntry<StorageBox> CARDBOARD_STORAGE_BOX = REGISTRATE
            .block("cardboard_storage_box", properties -> new StorageBox(properties, Util.CARDBOARD_STORAGE_BOX_SIZE))
            .initialProperties(AllBlocks.CARDBOARD_BLOCK::get)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.storageBox("cardboard"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.CARDBOARD_BLOCK))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("cardboard"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> STORAGE_BOX = REGISTRATE
            .block("storage_box", properties -> new StorageBox(properties, Util.IRON_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.storageBox("industrial_iron"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.INDUSTRIAL_IRON_BLOCK))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("industrial_iron"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> WEATHERED_STORAGE_BOX = REGISTRATE
            .block("weathered_storage_box", properties -> new StorageBox(properties, Util.IRON_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .blockstate(ModBlockstateHelper.storageBox("weathered"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.WEATHERED_IRON_BLOCK))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("weathered"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> ANDESITE_STORAGE_BOX = REGISTRATE
            .block("andesite_storage_box", properties -> new StorageBox(properties, Util.ANDESITE_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.ANDESITE)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .blockstate(ModBlockstateHelper.storageBox("andesite"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.ANDESITE_CASING))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("andesite"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> COPPER_STORAGE_BOX = REGISTRATE
            .block("copper_storage_box", properties -> new StorageBox(properties, Util.COPPER_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.COPPER_BLOCK)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .blockstate(ModBlockstateHelper.storageBox("copper"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.COPPER_CASING))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("copper"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> BRASS_STORAGE_BOX = REGISTRATE
            .block("brass_storage_box", properties -> new StorageBox(properties, Util.BRASS_STORAGE_BOX_SIZE))
            .initialProperties(AllBlocks.BRASS_BLOCK)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .blockstate(ModBlockstateHelper.storageBox("brass"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.BRASS_CASING))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("brass"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();

    public static final BlockEntry<StorageBox> HARDENED_STORAGE_BOX = REGISTRATE
            .block("hardened_storage_box", properties -> new StorageBox(properties, Util.HARDENED_STORAGE_BOX_SIZE))
            .initialProperties(() -> Blocks.NETHERITE_BLOCK)
            .transform(mountedItemStorage(ModMountedStorageTypes.STORAGE_BOX_MOUNTED))
            .blockstate(ModBlockstateHelper.storageBox("hardened"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.storageBox(AllBlocks.RAILWAY_CASING))
            .tag(ModTags.Blocks.STORAGE_BOX)
            .onRegister(movementBehaviour(new StorageBoxMovementBehaviour()))
            .item(StorageBoxItem::new)
            .model(ModModelHelper.storageBox("hardened"))
            .tag(ModTags.Items.STORAGE_BOX_ITEM)
            .build()
            .register();


    // BACKPACKS //
    public static final BlockEntry<BackpackBlock> BACKPACK = REGISTRATE
            .block("backpack", properties -> new BackpackBlock(properties, Util.IRON_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .blockstate(ModBlockstateHelper.backpack("industrial_iron"))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.backpack())
            .tag(ModTags.Blocks.BACKPACK)
            .item(BackpackItem::new)
            .model(ModModelHelper.backpackItem("industrial_iron"))
            .tag(ModTags.Items.BACKPACK_ITEM)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> ANDESITE_BACKPACK = REGISTRATE
            .block("andesite_backpack", properties -> new BackpackBlock(properties, Util.ANDESITE_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .blockstate(ModBlockstateHelper.backpack("andesite"))
            .loot(ModLootTableHelper.copyComponents())
            .tag(ModTags.Blocks.BACKPACK)
            .item(BackpackItem::new)
            .model(ModModelHelper.backpackItem("andesite"))
            .tag(ModTags.Items.BACKPACK_ITEM)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> COPPER_BACKPACK = REGISTRATE
            .block("copper_backpack", properties -> new BackpackBlock(properties, Util.COPPER_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .blockstate(ModBlockstateHelper.backpack("copper"))
            .loot(ModLootTableHelper.copyComponents())
            .tag(ModTags.Blocks.BACKPACK)
            .item(BackpackItem::new)
            .model(ModModelHelper.backpackItem("copper"))
            .tag(ModTags.Items.BACKPACK_ITEM)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> BRASS_BACKPACK = REGISTRATE
            .block("brass_backpack", properties -> new BackpackBlock(properties, Util.BRASS_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .blockstate(ModBlockstateHelper.backpack("brass"))
            .loot(ModLootTableHelper.copyComponents())
            .tag(ModTags.Blocks.BACKPACK)
            .item(BackpackItem::new)
            .model(ModModelHelper.backpackItem("brass"))
            .tag(ModTags.Items.BACKPACK_ITEM)
            .build()
            .register();

    public static final BlockEntry<BackpackBlock> HARDENED_BACKPACK = REGISTRATE
            .block("hardened_backpack", properties -> new BackpackBlock(properties, Util.HARDENED_BACKPACK_STACK_MULTIPLIER))
            .initialProperties(() -> Blocks.WHITE_WOOL)
            .properties(BlockBehaviour.Properties::noCollission)
            .blockstate(ModBlockstateHelper.backpack("hardened"))
            .loot(ModLootTableHelper.copyComponents())
            .tag(ModTags.Blocks.BACKPACK)
            .item(BackpackItem::new)
            .model(ModModelHelper.backpackItem("hardened"))
            .tag(ModTags.Items.BACKPACK_ITEM)
            .build()
            .register();


    // STORAGE CONTROLLER BLOCKS //
    public static final BlockEntry<StorageController> STORAGE_CONTROLLER = REGISTRATE
            .block("storage_controller", StorageController::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .blockstate(ModBlockstateHelper.storageController())
            .recipe(ModRecipeHelper.storageController())
            .simpleItem()
            .register();

    public static final BlockEntry<StorageInterface> STORAGE_INTERFACE = REGISTRATE
            .block("storage_interface", StorageInterface::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .blockstate(ModBlockstateHelper.storageInterface())
            .recipe(ModRecipeHelper.storageInterface())
            .simpleItem()
            .register();


    // PASSER BLOCKS //
    public static final BlockEntry<PasserBlock> PASSER_BLOCK = REGISTRATE
            .block("passer_block", properties -> new PasserBlock(properties, false))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties.strength(1.5f))
            .blockstate(ModBlockstateHelper.passerBlock(false))
            .recipe(ModRecipeHelper.passer(false))
            .simpleItem()
            .register();

    public static final BlockEntry<PasserBlock> SMART_PASSER_BLOCK = REGISTRATE
            .block("smart_passer_block", properties -> new PasserBlock(properties, true))
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties.strength(1.5f))
            .blockstate(ModBlockstateHelper.passerBlock(true))
            .recipe(ModRecipeHelper.passer(true))
            .simpleItem()
            .register();


    // SIMPLE STORAGE BOXES //
    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_OAK = REGISTRATE
            .block("oak_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.OAK_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.OAK_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.OAK_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "oak"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_SPRUCE = REGISTRATE
            .block("spruce_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.SPRUCE_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.SPRUCE_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.SPRUCE_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "spruce"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_BIRCH = REGISTRATE
            .block("birch_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.BIRCH_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.BIRCH_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.BIRCH_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "birch"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_JUNGLE = REGISTRATE
            .block("jungle_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.JUNGLE_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.JUNGLE_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.JUNGLE_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "jungle"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_ACACIA = REGISTRATE
            .block("acacia_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.ACACIA_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.ACACIA_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.ACACIA_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "acacia"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_DARK_OAK = REGISTRATE
            .block("dark_oak_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.DARK_OAK_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.DARK_OAK_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.DARK_OAK_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "dark_oak"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_MANGROVE = REGISTRATE
            .block("mangrove_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.MANGROVE_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.MANGROVE_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.MANGROVE_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "mangrove"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_CHERRY = REGISTRATE
            .block("cherry_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.CHERRY_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.CHERRY_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.CHERRY_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "cherry"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_BAMBOO = REGISTRATE
            .block("bamboo_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.BAMBOO_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.BAMBOO_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.BAMBOO_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "bamboo"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_CRIMSON = REGISTRATE
            .block("crimson_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.CRIMSON_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.CRIMSON_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.CRIMSON_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "crimson"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_WARPED = REGISTRATE
            .block("warped_simple_storage_box", SimpleStorageBox::new)
            .initialProperties(() -> Blocks.WARPED_PLANKS)
            .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
            .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
            .blockstate(ModBlockstateHelper.simpleStorageBox(Blocks.WARPED_PLANKS))
            .loot(ModLootTableHelper.copyComponents())
            .recipe(ModRecipeHelper.simpleStorageBox(Blocks.WARPED_PLANKS))
            .item(SimpleStorageBoxItem::new)
            .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "warped"))
            .build()
            .register();

    public static final BlockEntry<SimpleStorageBox> SIMPLE_STORAGE_BOX_PALE_OAK =
            ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT) ? REGISTRATE
                    .block("pale_oak_simple_storage_box", SimpleStorageBox::new)
                    .initialProperties(com.blackgear.vanillabackport.common.registries.ModBlocks.PALE_OAK_PLANKS::get)
                    .transform(mountedItemStorage(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED))
                    .onRegister(movementBehaviour(new SimpleStorageBoxMovementBehaviour()))
                    .blockstate(ModBlockstateHelper.simpleStorageBox(com.blackgear.vanillabackport.common.registries.ModBlocks.PALE_OAK_PLANKS))
                    .loot(ModLootTableHelper.copyComponents())
                    .recipe(ModRecipeHelper.simpleStorageBox(com.blackgear.vanillabackport.common.registries.ModBlocks.PALE_OAK_PLANKS))
                    .item(SimpleStorageBoxItem::new)
                    .model((ctx, prov) -> ModModelHelper.simpleStorageBox(ctx, prov, "pale_oak"))
                    .build()
                    .register() : null;


    // CASING BLOCKS //
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_OAK = REGISTRATE
            .block("oak_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.OAK_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("oak"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.OAK_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_SPRUCE = REGISTRATE
            .block("spruce_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.SPRUCE_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("spruce"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.SPRUCE_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BIRCH = REGISTRATE
            .block("birch_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.BIRCH_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("birch"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.BIRCH_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_JUNGLE = REGISTRATE
            .block("jungle_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.JUNGLE_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("jungle"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.JUNGLE_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_ACACIA = REGISTRATE
            .block("acacia_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.ACACIA_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("acacia"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.ACACIA_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_DARK_OAK = REGISTRATE
            .block("dark_oak_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.DARK_OAK_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("dark_oak"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.DARK_OAK_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_MANGROVE = REGISTRATE
            .block("mangrove_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.MANGROVE_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("mangrove"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.MANGROVE_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CHERRY = REGISTRATE
            .block("cherry_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.CHERRY_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("cherry"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.CHERRY_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BAMBOO = REGISTRATE
            .block("bamboo_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.BAMBOO_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("bamboo"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.BAMBOO_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CRIMSON = REGISTRATE
            .block("crimson_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.CRIMSON_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("crimson"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.CRIMSON_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .setData(ProviderType.ITEM_TAGS, NonNullBiConsumer.noop())
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_WARPED = REGISTRATE
            .block("warped_storage_trim", CasingBlock::new)
            .properties(properties -> properties.mapColor(MapColor.PODZOL))
            .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.WARPED_CASING))
            .blockstate(ModBlockstateHelper.storageTrim("warped"))
            .recipe(ModRecipeHelper.storageTrim(Blocks.WARPED_PLANKS))
            .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
            .register();

    public static final BlockEntry<CasingBlock> STORAGE_TRIM_PALE_OAK =
            ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT) ? REGISTRATE
                    .block("pale_oak_storage_trim", CasingBlock::new)
                    .properties(properties -> properties.mapColor(MapColor.QUARTZ))
                    .transform(ModBlockBuilderHelper.casing(() -> SpriteShifts.PALE_OAK_CASING))
                    .blockstate(ModBlockstateHelper.storageTrim("pale_oak"))
                    .recipe(ModRecipeHelper.storageTrim(com.blackgear.vanillabackport.common.registries.ModBlocks.PALE_OAK_PLANKS))
                    .removeTag(ProviderType.BLOCK_TAGS, AllTags.AllBlockTags.CASING.tag)
                    .register() : null;

    public static void register() {
    }

}
