package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IUpgrade {

    UpgradeType getType();

    List<UpgradeDataSync.Field> getSettings();

    Map<UpgradeDataSync.Field, Boolean> getDefaultSettings();

    default List<UpgradeDataSync.Field> getIntSettings() {
        return List.of();
    }

    default Map<UpgradeDataSync.Field, Integer> getDefaultIntSettings() {
        return Map.of();
    }

    @Nullable
    UpgradePanel createPanel(UpgradeContext context);

    List<Slot> createSlots(UpgradeContext context);

    void onInstalled(UpgradeContext context);

    void onRemoved(UpgradeContext context);

    default void onUninstalled(UpgradeContext context) {
    }

    default Optional<ItemStack> onQuickMove(UpgradeContext context) {
        return Optional.empty();
    }

    void tick(UpgradeContext context);

    default boolean clicked(UpgradeContext context) {
        return false;
    }

    default int getFilterSlotIndex(BackpackSlotLayout layout) {
        return -1;
    }

    default boolean filterAccepts(UpgradeContext context, ItemStack stack) {
        return true;
    }

    default boolean onPlayerTouchItem(UpgradeContext context, ItemEntity itemEntity, @Nullable UUID target, int pickupDelay) {
        return false;
    }

    default boolean onLivingFall(UpgradeContext context, LivingFallEvent event) {
        return false;
    }

    default boolean onAttackEntity(UpgradeContext context, InteractionHand hand, LivingEntity target) {
        return false;
    }

    default boolean onLeftClickBlock(UpgradeContext context, InteractionHand hand, BlockPos blockPos) {
        return false;
    }

    default boolean onBlockBreak(UpgradeContext context, BlockEvent.BreakEvent event) {
        return false;
    }

    default boolean onPickBlock(UpgradeContext context, ItemStack pickedItem) {
        return false;
    }

    default void onBackpackEquipped(UpgradeContext context) {
    }

    default void onBackpackUnequipped(UpgradeContext context) {
    }
}

