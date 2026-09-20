package net.fxnt.fxntstorage.compat.thirst;

import cn.mlus.thirst.api.ThirstHelper;
import cn.mlus.thirst.content.purity.WaterPurity;
import cn.mlus.thirst.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ThirstCompat {

    public static final int MIN_PURITY = WaterPurity.MIN_PURITY;
    public static final int MAX_PURITY = WaterPurity.MAX_PURITY;

    private ThirstCompat() {
    }

    public static boolean isDrinkable(ItemStack stack, Player player) {
        if (stack.isEmpty()) return false;
        return ThirstHelper.itemRestoresThirst(stack) && ThirstHelper.playerRestoresThirst(stack, player) && ThirstHelper.getThirst(stack) > 0;
    }

    public static boolean isPurityEnabled() {
        return WaterPurity.isEnabled();
    }

    public static int getPurity(ItemStack stack) {
        return WaterPurity.getPurity(stack);
    }

    public static int getPurityColor(int purity) {
        return WaterPurity.getPurityColor(purity);
    }

    public static String getPurityText(int purity) {
        return WaterPurity.getPurityText(purity);
    }

    public static boolean isThirstBelow(Player player, int threshold) {
        return player.getData(ModAttachment.PLAYER_THIRST).getThirst() < threshold;
    }
}
