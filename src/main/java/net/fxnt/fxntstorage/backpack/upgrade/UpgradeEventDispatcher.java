package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.UUID;

public class UpgradeEventDispatcher {

    @Nullable
    private static UpgradeContext buildPlayerContext(Player player) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpack.isEmpty()) return null;

        IBackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
        return UpgradeContext.forWornBackpack(player, backpack, container);
    }

    public static boolean dispatchPlayerTouchItem(Player player, ItemEntity itemEntity,
                                                  @Nullable UUID target, int pickupDelay) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return false;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onPlayerTouchItem(context, itemEntity, target, pickupDelay)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean dispatchLivingFall(Player player, LivingFallEvent event) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return false;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onLivingFall(context, event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void dispatchLeftClickBlock(Player player, InteractionHand hand, BlockPos pos) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onLeftClickBlock(context, hand, pos)) {
                    return;
                }
            }
        }
    }

    public static void dispatchAttackEntity(Player player, InteractionHand hand, LivingEntity target) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onAttackEntity(context, hand, target)) {
                    return;
                }
            }
        }
    }

    public static boolean dispatchBlockBreak(Player player, BlockEvent.BreakEvent event) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return false;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onBlockBreak(context, event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void dispatchPickBlock(Player player, ItemStack pickedItem) {
        UpgradeContext context = buildPlayerContext(player);
        if (context == null) return;

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                if (upgrade.onPickBlock(context, pickedItem)) {
                    return;
                }
            }
        }
    }

    public static void dispatchBackpackEquipped(Player player, ItemStack backpack) {
        IBackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
        UpgradeContext context = UpgradeContext.forWornBackpack(player, backpack, container);

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            if (UpgradeHelper.hasActiveUpgrade(context.itemHandler(), upgrade.getType())) {
                upgrade.onBackpackEquipped(context);
            }
        }
    }

    public static void dispatchBackpackUnequipped(Player player, ItemStack backpack) {
        IBackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
        UpgradeContext context = UpgradeContext.forWornBackpack(player, backpack, container);

        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            // Cleanup - always call regardless of hasActiveUpgrade
            upgrade.onBackpackUnequipped(context);
        }
    }
}
