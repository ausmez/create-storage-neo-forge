package net.fxnt.fxntstorage.compat.constructionwand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ConstructionWandVersion {

    private static Boolean kots;

    public static boolean isKots() {
        if (kots == null)
            kots = detectKots();
        return kots;
    }

    private static boolean detectKots() {
        try {
            Class<?> handlerInterface = Class.forName("thetadev.constructionwand.api.IContainerHandler");
            handlerInterface.getMethod("getSignature", Player.class, ItemStack.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }
}
