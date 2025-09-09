package net.fxnt.fxntstorage.compat.constructionwand;

import thetadev.constructionwand.ConstructionWand;

public class ConstructionWandCompat {
    public static void init() {
        ConstructionWand.instance.containerManager.register(new HandlerBackpack());
        ConstructionWand.instance.containerManager.register(new HandlerStorageBox());
    }

}
