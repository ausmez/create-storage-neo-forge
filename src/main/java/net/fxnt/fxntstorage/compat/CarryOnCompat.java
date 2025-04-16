package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModCompats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.InterModComms;

public class CarryOnCompat {
    private static final String BLACKLIST_BLOCK = "blacklistBlock";
    private static final String BLACKLIST_BLOCK_ENTITY = "blacklistEntity";

    public static void sendIMC() {
        BuiltInRegistries.BLOCK.keySet().stream()
                .filter(blockId -> blockId.getNamespace().equals(FXNTStorage.MOD_ID))
                .forEach(id -> InterModComms.sendTo(ModCompats.CARRY_ON, BLACKLIST_BLOCK, id::toString));
        InterModComms.sendTo(ModCompats.CARRY_ON, BLACKLIST_BLOCK_ENTITY, () -> FXNTStorage.MOD_ID + ":*");
    }

}
