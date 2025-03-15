package net.fxnt.fxntstorage.util;

import com.simibubi.create.foundation.utility.Lang;
import net.fxnt.fxntstorage.backpacks.main.BackpackBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Util {

    // Storage Box Size
    public static final int SLOTS_PER_ROW = 12;
    public static final int IRON_STORAGE_BOX_SIZE = 60;      // 5 Rows
    public static final int ANDESITE_STORAGE_BOX_SIZE = 84;  // 7 Rows
    public static final int COPPER_STORAGE_BOX_SIZE = 108;   // 9 Rows
    public static final int BRASS_STORAGE_BOX_SIZE = 132;    // 11 Rows
    public static final int HARDENED_STORAGE_BOX_SIZE = 156; // 13 Rows

    // BackPack Size
    public static final int IRON_BACKPACK_STACK_MULTIPLIER = 2;
    public static final int ANDESITE_BACKPACK_STACK_MULTIPLIER = 4;
    public static final int COPPER_BACKPACK_STACK_MULTIPLIER = 8;
    public static final int BRASS_BACKPACK_STACK_MULTIPLIER = 16;
    public static final int HARDENED_BACKPACK_STACK_MULTIPLIER = 32;

    // BackPack Upgrades
    public static final String BLANK_UPGRADE = "back_pack_blank_upgrade";
    public static final String STORAGE_BOX_VOID_UPGRADE = "storage_box_void_upgrade";
    public static final String STORAGE_BOX_CAPACITY_UPGRADE = "storage_box_capacity_upgrade";
    public static final String MAGNET_UPGRADE = "back_pack_magnet_upgrade";
    public static final String MAGNET_UPGRADE_DEACTIVATED = "back_pack_magnet_upgrade_deactivated";
    public static final String PICKBLOCK_UPGRADE = "back_pack_pickblock_upgrade";
    public static final String PICKBLOCK_UPGRADE_DEACTIVATED = "back_pack_pickblock_upgrade_deactivated";
    public static final String ITEMPICKUP_UPGRADE = "back_pack_itempickup_upgrade";
    public static final String ITEMPICKUP_UPGRADE_DEACTIVATED = "back_pack_itempickup_upgrade_deactivated";
    public static final String FLIGHT_UPGRADE = "back_pack_flight_upgrade";
    public static final String FLIGHT_UPGRADE_DEACTIVATED = "back_pack_flight_upgrade_deactivated";
    public static final String REFILL_UPGRADE = "back_pack_refill_upgrade";
    public static final String REFILL_UPGRADE_DEACTIVATED = "back_pack_refill_upgrade_deactivated";
    public static final String FEEDER_UPGRADE = "back_pack_feeder_upgrade";
    public static final String FEEDER_UPGRADE_DEACTIVATED = "back_pack_feeder_upgrade_deactivated";
    public static final String TOOLSWAP_UPGRADE = "back_pack_toolswap_upgrade";
    public static final String TOOLSWAP_UPGRADE_DEACTIVATED = "back_pack_toolswap_upgrade_deactivated";
    public static final String FALLDAMAGE_UPGRADE = "back_pack_falldamage_upgrade";
    public static final String FALLDAMAGE_UPGRADE_DEACTIVATED = "back_pack_falldamage_upgrade_deactivated";

    public static final byte BACKPACK_ON_BACK = 1;
    public static final byte BACKPACK_IN_HAND = 2;
    public static final byte BACKPACK_AS_BLOCK = 3;

    // BackPack Compartment Sizes
    public static final int ITEM_SLOT_START_RANGE = 0;
    public static final int ITEM_SLOT_END_RANGE = BackpackBlock.getItemSlotCount();
    public static final int TOOL_SLOT_START_RANGE = ITEM_SLOT_END_RANGE;
    public static final int TOOL_SLOT_END_RANGE = TOOL_SLOT_START_RANGE + BackpackBlock.getToolSlotCount();
    public static final int UPGRADE_SLOT_START_RANGE = TOOL_SLOT_END_RANGE;
    public static final int UPGRADE_SLOT_END_RANGE = UPGRADE_SLOT_START_RANGE + BackpackBlock.getUpgradeSlotCount();

    // Menus
    public static final int SLOT_SIZE = 18;
    public static final int CONTAINER_HEADER_HEIGHT = 17;

    // Key Bind Bytes
    public static byte OPEN_BACKPACK = 0;
    public static byte CLOSE_BACKPACK = -1;
    public static byte TOGGLE_HOVER = 1;

    public static byte JETPACK_KEY_PRESS = 1;
    public static byte JETPACK_KEY_RELEASE = 0;

    public static String formatNumber(int number) {
        if (number < 10_000) {
            return String.valueOf(number); // Numbers less than 10,000 are shown as the full integer
        } else if (number < 1_000_000) {
            // For numbers between 10,000 and 999,999, display as "X.XXk" or "XXXk"
            if (number % 1000 == 0) {
                return String.format("%dk", number / 1000); // Exact thousands
            } else {
                return String.format("%.2fk", number / 1000.0); // Otherwise, format to two decimal places
            }
        } else {
            // For numbers 1,000,000 or greater, display as "1.0M" or more
            if (number % 1_000_000 == 0) {
                return String.format("%dM", number / 1_000_000); // Exact millions
            } else {
                return String.format("%.2fM", number / 1_000_000.0); // Otherwise, format to two decimal places
            }
        }
    }

    public static boolean isVowel(char c) {
        return "AEIOUaeiou".indexOf(c) != -1;
    }

    public record ItemWithNBT(Item item, CompoundTag tag) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ItemWithNBT that = (ItemWithNBT) obj;
            return item == that.item && Objects.equals(tag, that.tag);
        }
    }

    public enum InventorySortOrder implements StringRepresentable {
        COUNT, NAME, TAG;

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }

}
