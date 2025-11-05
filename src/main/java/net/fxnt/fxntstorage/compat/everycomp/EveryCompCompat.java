package net.fxnt.fxntstorage.compat.everycomp;

import net.fxnt.fxntstorage.FXNTStorage;
import net.mehvahdjukaar.every_compat.api.EveryCompatAPI;

public class EveryCompCompat {
    public static void init() {
        EveryCompatAPI.registerModule(new WoodGoodModule(FXNTStorage.MOD_ID));
    }
}
