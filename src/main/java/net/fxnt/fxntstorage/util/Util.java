package net.fxnt.fxntstorage.util;

import com.mojang.datafixers.util.Pair;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataManager;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Util {

    // Storage Box Size
    public static final int SLOTS_PER_ROW = 12;
    public static final int CARDBOARD_STORAGE_BOX_SIZE = 48; // 4 Rows
    public static final int IRON_STORAGE_BOX_SIZE = 60;      // 5 Rows
    public static final int ANDESITE_STORAGE_BOX_SIZE = 84;  // 7 Rows
    public static final int COPPER_STORAGE_BOX_SIZE = 108;   // 9 Rows
    public static final int BRASS_STORAGE_BOX_SIZE = 132;    // 11 Rows
    public static final int HARDENED_STORAGE_BOX_SIZE = 156; // 13 Rows

    // Backpack Size
    public static final int IRON_BACKPACK_STACK_MULTIPLIER = 2;
    public static final int ANDESITE_BACKPACK_STACK_MULTIPLIER = 4;
    public static final int COPPER_BACKPACK_STACK_MULTIPLIER = 8;
    public static final int BRASS_BACKPACK_STACK_MULTIPLIER = 16;
    public static final int HARDENED_BACKPACK_STACK_MULTIPLIER = 32;

    // Backpack Upgrades
    public static final String BLANK_UPGRADE = "backpack_blank_upgrade";
    public static final String STORAGE_BOX_VOID_UPGRADE = "storage_box_void_upgrade";
    public static final String STORAGE_BOX_CAPACITY_UPGRADE = "storage_box_capacity_upgrade";
    public static final String MAGNET_UPGRADE = "backpack_magnet_upgrade";
    public static final String MAGNET_UPGRADE_DEACTIVATED = "backpack_magnet_upgrade_deactivated";
    public static final String PICKBLOCK_UPGRADE = "backpack_pickblock_upgrade";
    public static final String PICKBLOCK_UPGRADE_DEACTIVATED = "backpack_pickblock_upgrade_deactivated";
    public static final String ITEMPICKUP_UPGRADE = "backpack_itempickup_upgrade";
    public static final String ITEMPICKUP_UPGRADE_DEACTIVATED = "backpack_itempickup_upgrade_deactivated";
    public static final String FLIGHT_UPGRADE = "backpack_flight_upgrade";
    public static final String FLIGHT_UPGRADE_DEACTIVATED = "backpack_flight_upgrade_deactivated";
    public static final String REFILL_UPGRADE = "backpack_refill_upgrade";
    public static final String REFILL_UPGRADE_DEACTIVATED = "backpack_refill_upgrade_deactivated";
    public static final String FEEDER_UPGRADE = "backpack_feeder_upgrade";
    public static final String FEEDER_UPGRADE_DEACTIVATED = "backpack_feeder_upgrade_deactivated";
    public static final String TOOLSWAP_UPGRADE = "backpack_toolswap_upgrade";
    public static final String TOOLSWAP_UPGRADE_DEACTIVATED = "backpack_toolswap_upgrade_deactivated";
    public static final String FALLDAMAGE_UPGRADE = "backpack_falldamage_upgrade";
    public static final String FALLDAMAGE_UPGRADE_DEACTIVATED = "backpack_falldamage_upgrade_deactivated";
    public static final String OREMINING_UPGRADE = "backpack_oremining_upgrade";
    public static final String OREMINING_UPGRADE_DEACTIVATED = "backpack_oremining_upgrade_deactivated";
    public static final String TORCHDEPLOYER_UPGRADE = "backpack_torchdeployer_upgrade";
    public static final String TORCHDEPLOYER_UPGRADE_DEACTIVATED = "backpack_torchdeployer_upgrade_deactivated";
    public static final String JUKEBOX_UPGRADE = "backpack_jukebox_upgrade";
    public static final String JUKEBOX_UPGRADE_DEACTIVATED = "backpack_jukebox_upgrade_deactivated";
    public static final String HEALTH_UPGRADE = "backpack_health_upgrade";
    public static final String HEALTH_UPGRADE_DEACTIVATED = "backpack_health_upgrade_deactivated";

    // Menus
    public static final int SLOT_SIZE = 18;
    public static final int CONTAINER_HEADER_HEIGHT = 17;

    // Key Bind Bytes
    public static final byte JETPACK_KEY_PRESS = 0;
    public static final byte JETPACK_KEY_RELEASE = 1;
    public static final byte OPEN_BACKPACK = 2;
    public static final byte CLOSE_BACKPACK = 3;
    public static final byte BACKPACK_MENU_CTRL = 4;
    public static final byte TOGGLE_HOVER = 5;
    public static final byte MINE_ALL_BLOCKS = 6;

    // Inventory sorting
    public static final byte INV_TYPE_BACKPACK = 0;
    public static final byte INV_TYPE_STORAGE_BOX = 1;

    public static String formatNumber(int number) {
        if (number < 10_000) return String.valueOf(number);
        if (number < 1_000_000) return number % 1_000 == 0
                ? String.format("%dk", number / 1_000)
                : String.format("%.1fk", number / 1_000.0);
        return number % 1_000_000 == 0
                ? String.format("%dM", number / 1_000_000)
                : String.format("%.2fM", number / 1_000_000.0);
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

        public Component getDisplayName() {
            if (tag != null && tag.contains("display", Tag.TAG_COMPOUND)) {
                CompoundTag display = tag.getCompound("display");

                if (display.contains("Name", Tag.TAG_STRING)) {
                    String jsonName = display.getString("Name");
                    Component parsed = Component.Serializer.fromJson(jsonName);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
            return Component.empty();
        }

        public String getDisplayNameString() {
            return getDisplayName().getString();
        }

    }

    public static CompoundTag getOrCreateSubTag(CompoundTag root, String key) {
        if (!root.contains(key, Tag.TAG_COMPOUND)) {
            root.put(key, new CompoundTag());
        }
        return root.getCompound(key);
    }

    public static <T> boolean isSymmetrical(int width, int height, List<T> list) {
        if (width != 1) {
            int i = width / 2;

            for (int j = 0; j < height; ++j) {
                for (int k = 0; k < i; ++k) {
                    int l = width - 1 - k;
                    T t = list.get(k + j * width);
                    T t1 = list.get(l + j * width);
                    if (!t.equals(t1)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isEdible(@NotNull ItemStack food, LivingEntity player) {
        if (food.isEmpty())
            return false;

        FoodProperties foodProperties = food.getFoodProperties(player);
        return foodProperties != null && foodProperties.getNutrition() > 0;
    }

    public static boolean hasNegativeEffects(@NotNull ItemStack food, LivingEntity player) {
        FoodProperties foodProperties = food.getFoodProperties(player);
        if (foodProperties == null) return false;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);

        if (food.is(Items.CHORUS_FRUIT) && !manager.getSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, false))
            return true;

        CompoundTag tag = food.getTag();
        if (tag != null && tag.contains("Effects", Tag.TAG_LIST)) {
            ListTag effects = tag.getList("Effects", Tag.TAG_COMPOUND);

            for (Tag entry : effects) {
                if (!(entry instanceof CompoundTag effectTag)) continue;
                if (!effectTag.contains("forge:effect_id", Tag.TAG_STRING)) continue;

                String effectString = effectTag.getString("forge:effect_id");
                ResourceLocation effectId = ResourceLocation.parse(effectString);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);

                if (effect != null)
                    return effect.getCategory().equals(MobEffectCategory.HARMFUL);
            }
        }

        // This should capture most foods with negative effects
        for (Pair<MobEffectInstance, Float> effect : foodProperties.getEffects()) {
            return (effect.getFirst().getEffect().getCategory().equals(MobEffectCategory.HARMFUL));
        }
        return false;
    }

    public static void sortStorageItems(AbstractContainerMenu menu, ServerPlayer player, int startIndex, int endIndex, SortOrder sortOrder, int containerSlotCount) {
        // Create a map to track all items (with or without NBT)
        Map<ItemWithNBT, Integer> itemCompMap = new HashMap<>();
        // Track a template stack per key to preserve ForgeCaps when rebuilding
        Map<Util.ItemWithNBT, ItemStack> templateStacks = new HashMap<>();

        // Add all items in the container from startIndex to endIndex into the map
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                CompoundTag tag = stack.getTag();
                Util.ItemWithNBT key = new Util.ItemWithNBT(stack.getItem(), tag);
                itemCompMap.merge(key, stack.getCount(), Integer::sum);
                templateStacks.putIfAbsent(key, stack);
            }
        }

        // Create a list of entries and sort them
        List<Map.Entry<Util.ItemWithNBT, Integer>> sortedItems = new ArrayList<>(itemCompMap.entrySet());

        switch (sortOrder) {
            case MOD:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(entry.getKey().item())).toString())  // Sort by registry name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            case NAME:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
                        .thenComparing(entry -> entry.getKey().getDisplayNameString()) // Then by custom name
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            default:
                // Default to COUNT
                sortedItems.sort(
                        Map.Entry.<Util.ItemWithNBT, Integer>comparingByValue().reversed()
                                .thenComparing(entry -> entry.getKey().toString())
                );
        }

        NonNullList<ItemStack> compactedList = NonNullList.withSize(endIndex - startIndex, ItemStack.EMPTY);
        int idx = 0;

        // Rebuild the item stack list based on sorted entries
        for (Map.Entry<Util.ItemWithNBT, Integer> entry : sortedItems) {
            Util.ItemWithNBT key = entry.getKey();
            ItemStack template = templateStacks.get(key);
            int totalCount = entry.getValue();

            int maxStackSize = template.getMaxStackSize();

            while (totalCount > 0) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = template.copy();
                stack.setCount(stackSize);
                compactedList.set(idx, stack);
                totalCount -= stackSize;
                idx++;
            }
        }

        // Place the sorted items back into the inventory
        for (int i = 0; i < compactedList.size(); i++) {
            ItemStack stack = compactedList.get(i);
            Slot slot = player.containerMenu.getSlot(i + startIndex);
            slot.set(stack);

            if (i + startIndex < containerSlotCount) {
                player.connection.send(new ClientboundContainerSetSlotPacket(player.containerMenu.containerId, menu.getStateId(), i + startIndex, stack));
            }
        }
    }
}
