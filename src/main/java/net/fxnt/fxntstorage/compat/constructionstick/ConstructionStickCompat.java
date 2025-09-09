package net.fxnt.fxntstorage.compat.constructionstick;

import mrbysco.constructionstick.ConstructionStick;

public class ConstructionStickCompat {
    public static void init() {
        ConstructionStick.containerManager.register(new HandlerBackpack());
        ConstructionStick.containerManager.register(new HandlerStorageBox());
    }

}
