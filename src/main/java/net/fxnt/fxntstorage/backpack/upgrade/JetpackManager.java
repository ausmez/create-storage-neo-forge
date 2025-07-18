package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JetpackManager {
    private static final Map<UUID, JetpackHandler> serverHandlers = new HashMap<>();
    private static final Map<UUID, JetpackHandler> clientHandlers = new HashMap<>();

    public static JetpackHandler getJetpackHandler(@NotNull Player player) {
        return (player.level().isClientSide)
                ? clientHandlers.computeIfAbsent(player.getUUID(), uuid -> new JetpackHandler(player))
                : serverHandlers.computeIfAbsent(player.getUUID(), uuid -> new JetpackHandler(player));
    }

    // Called when a player joins the server
    public static void onPlayerJoin(Player player) {
        getJetpackHandler(player);
    }

    // Called when a player leaves the server
    public static void onPlayerLeave(@NotNull Player player) {
        serverHandlers.remove(player.getUUID());
        clientHandlers.remove(player.getUUID());
    }
}
