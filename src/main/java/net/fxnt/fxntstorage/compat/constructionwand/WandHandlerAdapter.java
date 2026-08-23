package net.fxnt.fxntstorage.compat.constructionwand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class WandHandlerAdapter implements InvocationHandler {

    private final IWandContainer container;

    public WandHandlerAdapter(IWandContainer container) {
        this.container = container;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "matches" -> {
                return container.matches((Player) args[0], (ItemStack) args[args.length - 1]);
            }
            case "getSignature" -> {
                return container.signature((Player) args[0], (ItemStack) args[1]);
            }
            case "countItems" -> {
                int offset = args.length - 3;
                return container.countItems((Player) args[0], (ItemStack) args[1 + offset], (ItemStack) args[2 + offset]);
            }
            case "useItems" -> {
                int offset = args.length - 4;
                return container.useItems((Player) args[0], (ItemStack) args[1 + offset], (ItemStack) args[2 + offset],
                        (Integer) args[3 + offset]);
            }
            case "equals" -> {
                return proxy == args[0];
            }
            case "hashCode" -> {
                return System.identityHashCode(container);
            }
            case "toString" -> {
                return container.toString();
            }
            default -> throw new UnsupportedOperationException(
                    "Unsupported Construction Wand container handler method: " + method);
        }
    }
}
