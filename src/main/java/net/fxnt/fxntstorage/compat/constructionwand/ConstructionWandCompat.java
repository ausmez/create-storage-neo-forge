package net.fxnt.fxntstorage.compat.constructionwand;

import net.fxnt.fxntstorage.FXNTStorage;
import thetadev.constructionwand.ConstructionWand;
import thetadev.constructionwand.api.IContainerHandler;

import java.lang.reflect.Proxy;

public class ConstructionWandCompat {
    public static void init() {
        if (ConstructionWand.instance == null || ConstructionWand.instance.containerManager == null) {
            FXNTStorage.LOGGER.warn("Construction Wand was not constructed yet, skipping container handler registration");
            return;
        }
        register(new HandlerBackpack());
        register(new HandlerStorageBox());
    }

    private static void register(IWandContainer container) {
        Object handler = Proxy.newProxyInstance(
                IContainerHandler.class.getClassLoader(),
                new Class[]{IContainerHandler.class},
                new WandHandlerAdapter(container));
        ConstructionWand.instance.containerManager.register((IContainerHandler) handler);
    }
}
