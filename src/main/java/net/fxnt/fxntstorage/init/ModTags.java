package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block> BACKPACK = blockTag("backpack");
        public static final TagKey<Block> BREAKABLE_WITH_ANY_TOOL = blockTag("breakable_with_any_tool");
        public static final TagKey<Block> SIMPLE_STORAGE_BOX = blockTag("simple_storage_box");
        public static final TagKey<Block> STORAGE_BOX = blockTag("storage_box");
        public static final TagKey<Block> STORAGE_NETWORK_BLOCK = blockTag("storage_network_block");
        public static final TagKey<Block> STORAGE_TRIM = blockTag("storage_trim");
        public static final TagKey<Block> ORE_MINING_BLOCK = blockTag("ore_mining_block");

        private static TagKey<Block> blockTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, name));
        }

    }

    public static class Items {

        public static final TagKey<Item> BACKPACK_ITEM = itemTag("backpack");
        public static final TagKey<Item> BACKPACK_UPGRADE = itemTag("backpack_upgrade");
        public static final TagKey<Item> BACKPACK_UPGRADE_DEACTIVATED = itemTag("backpack_upgrade_deactivated");
        public static final TagKey<Item> STORAGE_BOX_ITEM = itemTag("storage_box");
        public static final TagKey<Item> STORAGE_BOX_UPGRADE = itemTag("storage_box_upgrade");
        public static final TagKey<Item> REFILL_BLACKLIST = itemTag("refill_blacklist");

        private static TagKey<Item> itemTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, name));
        }

    }

}
