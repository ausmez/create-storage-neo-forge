package net.fxnt.fxntstorage.backpack.upgrade.itempickup;

import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ItemPickupUpgrade extends AbstractUpgrade {

    public ItemPickupUpgrade() {
        super(UpgradeType.ITEMPICKUP);
    }

    @Override
    public boolean onPlayerTouchItem(UpgradeContext context, ItemEntity itemEntity, @Nullable UUID target, int pickupDelay) {
        Player player = context.player();

        if (pickupDelay > 0) return false;
        if (target != null && !target.equals(player.getUUID())) return false;

        ItemStack itemStack = itemEntity.getItem();
        Item item = itemStack.getItem();
        int i = itemStack.getCount();
        if (pickupDelay == 0 && (target == null || target.equals(player.getUUID())) &&
                BackpackHelper.itemEntityToBackpack(context.container(), itemEntity, player)) {

            player.take(itemEntity, i);
            if (itemStack.isEmpty()) {
                itemEntity.discard();
                itemStack.setCount(i);
            }
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
            player.onItemPickup(itemEntity);
            return true;
        }

        return false;
    }
}
