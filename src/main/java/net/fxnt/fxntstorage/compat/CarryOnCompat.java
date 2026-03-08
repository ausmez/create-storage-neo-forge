package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModCompats;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.registries.ForgeRegistries;

public class CarryOnCompat {
    private static final String BLACKLIST_BLOCK = "blacklistBlock";
    private static final String BLACKLIST_BLOCK_ENTITY = "blacklistEntity";

    public static void sendIMC() {
        ForgeRegistries.BLOCKS.getKeys().stream().filter(id -> id.getNamespace().equals(FXNTStorage.MOD_ID))
                .forEach(id -> InterModComms.sendTo(ModCompats.CARRY_ON, BLACKLIST_BLOCK, id::toString));
        InterModComms.sendTo(ModCompats.CARRY_ON, BLACKLIST_BLOCK_ENTITY, () -> FXNTStorage.MOD_ID + ":*");
    }
}
