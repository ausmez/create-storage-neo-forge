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

        public static final TagKey<Block> BACK_PACK = blockTag("back_pack");
        public static final TagKey<Block> BREAKABLE_WITH_ANY_TOOL = blockTag("breakable_with_any_tool");
        public static final TagKey<Block> SIMPLE_STORAGE_BOX = blockTag("simple_storage_box");
        public static final TagKey<Block> STORAGE_BOX = blockTag("storage_box");
        public static final TagKey<Block> STORAGE_NETWORK_BLOCK = blockTag("storage_network_block");
        public static final TagKey<Block> STORAGE_TRIM = blockTag("storage_trim");

        private static TagKey<Block> blockTag(String name) {
            return BlockTags.create(new ResourceLocation(FXNTStorage.MOD_ID, name));
        }

    }

    public static class Items {

        public static final TagKey<Item> BACK_PACK_ITEM = itemTag("back_pack");
        public static final TagKey<Item> BACK_PACK_UPGRADE = itemTag("back_pack_upgrade");
        public static final TagKey<Item> STORAGE_BOX_ITEM = itemTag("storage_box");
        public static final TagKey<Item> STORAGE_BOX_UPGRADE = itemTag("storage_box_upgrade");

        private static TagKey<Item> itemTag(String name) {
            return ItemTags.create(new ResourceLocation(FXNTStorage.MOD_ID, name));
        }

    }

}
