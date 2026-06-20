package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataManager;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.compat.sable.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static final String STORAGE_BOX_COMPACTING_UPGRADE = "storage_box_compacting_upgrade";
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
    public static final String CRAFTING_UPGRADE = "backpack_crafting_upgrade";
    public static final String CRAFTING_UPGRADE_DEACTIVATED = "backpack_crafting_upgrade_deactivated";
    public static final String WORKSHOP_UPGRADE = "backpack_workshop_upgrade";
    public static final String WORKSHOP_UPGRADE_DEACTIVATED = "backpack_workshop_upgrade_deactivated";

    // Menus
    public static final int SLOT_SIZE = 18;
    public static final int CONTAINER_HEADER_HEIGHT = 17;

    // Keybind Bytes
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

    public record ItemWithComponent(Item item, DataComponentPatch patch) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ItemWithComponent that = (ItemWithComponent) obj;
            return item == that.item && Objects.equals(patch, that.patch);
        }

        public String getCustomName() {
            ItemStack stack = new ItemStack(item);
            stack.applyComponents(patch);
            return stack.getOrDefault(DataComponents.CUSTOM_NAME, "").toString();
        }
    }

    public static boolean isEdible(@NotNull ItemStack food, LivingEntity player) {
        if (!food.has(DataComponents.FOOD))
            return false;

        FoodProperties foodProperties = food.getItem().getFoodProperties(food, player);
        return foodProperties != null && foodProperties.nutrition() > 0;
    }

    public static boolean hasNegativeEffects(@NotNull ItemStack food, LivingEntity player) {
        FoodProperties foodProperties = food.getFoodProperties(player);
        if (foodProperties == null) return false;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);

        if (food.is(Items.CHORUS_FRUIT) && !manager.getSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, false))
            return true;

        if (food.is(Items.OMINOUS_BOTTLE)) return true;

        SuspiciousStewEffects stewEffects = food.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stewEffects != null) {
            for (SuspiciousStewEffects.Entry entry : stewEffects.effects()) {
                if (entry.effect().value().getCategory().equals(MobEffectCategory.HARMFUL)) return true;
            }
        }

        // This should capture most foods with negative effects
        for (FoodProperties.PossibleEffect effect : foodProperties.effects()) {
            MobEffectInstance instance = effect.effectSupplier().get();
            if (instance.getEffect().value().getCategory().equals(MobEffectCategory.HARMFUL))
                return true;
        }
        return false;
    }

    public static void sortStorageItems(AbstractContainerMenu menu, ServerPlayer player, int startIndex, int endIndex, SortOrder sortOrder, int containerSlotCount) {
        Map<ItemWithComponent, Integer> itemCompMap = new HashMap<>();

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                DataComponentPatch patch = stack.getComponentsPatch();
                ItemWithComponent key = new ItemWithComponent(stack.getItem(), patch);
                itemCompMap.merge(key, stack.getCount(), Integer::sum);
            }
        }

        List<Map.Entry<ItemWithComponent, Integer>> sortedItems = new ArrayList<>(itemCompMap.entrySet());

        switch (sortOrder) {
            case SortOrder.MOD:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<ItemWithComponent, Integer> entry) -> BuiltInRegistries.ITEM.getKey(entry.getKey().item()).toString())
                        .thenComparing(entry -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));
                break;
            case SortOrder.NAME:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<ItemWithComponent, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())
                        .thenComparing(entry -> entry.getKey().getCustomName())
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));
                break;
            default:
                sortedItems.sort(
                        Map.Entry.<ItemWithComponent, Integer>comparingByValue().reversed()
                                .thenComparing(entry -> entry.getKey().toString())
                );
        }

        NonNullList<ItemStack> compactedList = NonNullList.withSize(endIndex - startIndex, ItemStack.EMPTY);
        int idx = 0;

        for (Map.Entry<ItemWithComponent, Integer> entry : sortedItems) {
            ItemWithComponent key = entry.getKey();
            Item item = key.item();
            DataComponentPatch patch = key.patch();
            int totalCount = entry.getValue();
            int maxStackSize = new ItemStack(item, 1).getMaxStackSize();

            while (totalCount > 0) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                if (!patch.isEmpty()) stack.applyComponents(patch);
                compactedList.set(idx, stack);
                totalCount -= stackSize;
                idx++;
            }
        }

        for (int i = 0; i < compactedList.size(); i++) {
            ItemStack stack = compactedList.get(i);
            Slot slot = menu.getSlot(i + startIndex);
            slot.set(stack);
            if (i + startIndex < containerSlotCount) {
                player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.getStateId(), i + startIndex, stack));
            }
        }
    }

    public static Direction getAttackedBlockFace(BlockState state, Level level, BlockPos pos, Player player, @Nullable BlockEntity blockEntity) {
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;

        VoxelShape shape = state.getShape(level, pos);

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(reach));

        if (blockEntity != null) {
            AABB bounds = (shape.isEmpty() ? Shapes.block().bounds() : shape.bounds()).move(pos);
            Direction sub = SableCompat.getAttackedFaceInSubLevel(blockEntity, bounds, reach, player);
            if (sub != null) return sub;
        }

        BlockHitResult hit = shape.clip(eye, end, pos);

        return hit != null ? hit.getDirection() : null;
    }

    public static Direction nearestBlockFace(Vec3 point, AABB bounds) {
        Direction best = Direction.UP;
        double minDist = Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            double plane = 0;
            double coord = 0;

            switch (dir) {
                case DOWN -> { plane = bounds.minY; coord = point.y; }
                case UP -> { plane = bounds.maxY; coord = point.y; }
                case NORTH -> { plane = bounds.minZ; coord = point.z; }
                case SOUTH -> { plane = bounds.maxZ; coord = point.z; }
                case WEST -> { plane = bounds.minX; coord = point.x; }
                case EAST -> { plane = bounds.maxX; coord = point.x; }
            }

            double dist = Math.abs(coord - plane);
            if (dist < minDist) {
                minDist = dist;
                best = dir;
            }
        }

        return best;
    }

}
