package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.backpack.upgrade.UpgradeEventDispatcher;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public class CuriosEventHandler {

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getIdentifier().equals("back")) return;

        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();

        if (isBackpack(oldStack) && !isBackpack(newStack)) {
            EventHandler.stopJukeboxIfPlaying(player);
            UpgradeEventDispatcher.dispatchBackpackUnequipped(player, oldStack);
        }

        if (!isBackpack(oldStack) && isBackpack(newStack)) {
            UpgradeEventDispatcher.dispatchBackpackEquipped(player, newStack);
        }
    }

    private static boolean isBackpack(ItemStack stack) {
        return stack.is(ModTags.Items.BACKPACK_ITEM);
    }
}