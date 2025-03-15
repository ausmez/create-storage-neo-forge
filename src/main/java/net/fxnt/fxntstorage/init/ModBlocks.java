package net.fxnt.fxntstorage.init;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.main.BackpackBlock;
import net.fxnt.fxntstorage.containers.StorageBox;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.registry.SpriteShifts;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FXNTStorage.MOD_ID);

    // Register the block only, item is handled in ModItems
    public static final RegistryObject<Block> STORAGE_BOX = registerBlockOnly("storage_box", () -> new StorageBox(Util.IRON_STORAGE_BOX_SIZE, "industrial_iron_storage_box"));
    public static final RegistryObject<Block> ANDESITE_STORAGE_BOX = registerBlockOnly("andesite_storage_box", () -> new StorageBox(Util.ANDESITE_STORAGE_BOX_SIZE, "andesite_storage_box"));
    public static final RegistryObject<Block> COPPER_STORAGE_BOX = registerBlockOnly("copper_storage_box", () -> new StorageBox(Util.COPPER_STORAGE_BOX_SIZE, "copper_storage_box"));
    public static final RegistryObject<Block> BRASS_STORAGE_BOX = registerBlockOnly("brass_storage_box", () -> new StorageBox(Util.BRASS_STORAGE_BOX_SIZE, "brass_storage_box"));
    public static final RegistryObject<Block> HARDENED_STORAGE_BOX = registerBlockOnly("hardened_storage_box", () -> new StorageBox(Util.HARDENED_STORAGE_BOX_SIZE, "hardened_storage_box"));

    // Register the block only, item is handled in ModItems
    public static final RegistryObject<Block> BACK_PACK = registerBlockOnly("back_pack", () -> new BackpackBlock("back_pack", Util.IRON_BACKPACK_STACK_MULTIPLIER));
    public static final RegistryObject<Block> ANDESITE_BACK_PACK = registerBlockOnly("andesite_back_pack", () -> new BackpackBlock("andesite_back_pack", Util.ANDESITE_BACKPACK_STACK_MULTIPLIER));
    public static final RegistryObject<Block> COPPER_BACK_PACK = registerBlockOnly("copper_back_pack", () -> new BackpackBlock("copper_back_pack", Util.COPPER_BACKPACK_STACK_MULTIPLIER));
    public static final RegistryObject<Block> BRASS_BACK_PACK = registerBlockOnly("brass_back_pack", () -> new BackpackBlock("brass_back_pack", Util.BRASS_BACKPACK_STACK_MULTIPLIER));
    public static final RegistryObject<Block> HARDENED_BACK_PACK = registerBlockOnly("hardened_back_pack", () -> new BackpackBlock("hardened_back_pack", Util.HARDENED_BACKPACK_STACK_MULTIPLIER));

    public static final RegistryObject<Block> STORAGE_CONTROLLER = registerBlock("storage_controller", StorageController::new);
    public static final RegistryObject<Block> STORAGE_INTERFACE = registerBlock("storage_interface", StorageInterface::new);

    public static final RegistryObject<Block> PASSER_BLOCK = registerBlock("passer_block", () -> new PasserBlock(false));
    public static final RegistryObject<Block> SMART_PASSER_BLOCK = registerBlock("smart_passer_block", () -> new PasserBlock(true));

    // Register the block only, item is handled in ModItems
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX = registerBlockOnly("simple_storage_box", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_SPRUCE = registerBlockOnly("simple_storage_box_spruce", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_BIRCH = registerBlockOnly("simple_storage_box_birch", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_JUNGLE = registerBlockOnly("simple_storage_box_jungle", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_ACACIA = registerBlockOnly("simple_storage_box_acacia", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_DARK_OAK = registerBlockOnly("simple_storage_box_dark_oak", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_MANGROVE = registerBlockOnly("simple_storage_box_mangrove", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_CHERRY = registerBlockOnly("simple_storage_box_cherry", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_BAMBOO = registerBlockOnly("simple_storage_box_bamboo", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_CRIMSON = registerBlockOnly("simple_storage_box_crimson", SimpleStorageBox::new);
    public static final RegistryObject<Block> SIMPLE_STORAGE_BOX_WARPED = registerBlockOnly("simple_storage_box_warped", SimpleStorageBox::new);

    public static final BlockEntry<CasingBlock> STORAGE_TRIM = FXNTStorage.REGISTRATE.block("storage_trim", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.OAK_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_SPRUCE = FXNTStorage.REGISTRATE.block("storage_trim_spruce", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.SPRUCE_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BIRCH = FXNTStorage.REGISTRATE.block("storage_trim_birch", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.BIRCH_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_JUNGLE = FXNTStorage.REGISTRATE.block("storage_trim_jungle", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.JUNGLE_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_ACACIA = FXNTStorage.REGISTRATE.block("storage_trim_acacia", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.ACACIA_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_DARK_OAK = FXNTStorage.REGISTRATE.block("storage_trim_dark_oak", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.DARK_OAK_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_MANGROVE = FXNTStorage.REGISTRATE.block("storage_trim_mangrove", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.MANGROVE_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CHERRY = FXNTStorage.REGISTRATE.block("storage_trim_cherry", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.CHERRY_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_BAMBOO = FXNTStorage.REGISTRATE.block("storage_trim_bamboo", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.BAMBOO_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_CRIMSON = FXNTStorage.REGISTRATE.block("storage_trim_crimson", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.CRIMSON_CASING)).register();
    public static final BlockEntry<CasingBlock> STORAGE_TRIM_WARPED = FXNTStorage.REGISTRATE.block("storage_trim_warped", CasingBlock::new).properties(properties -> properties.mapColor(MapColor.PODZOL)).transform(BuilderTransformers.casing(() -> SpriteShifts.WARPED_CASING)).register();

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<T> registerBlockOnly(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
