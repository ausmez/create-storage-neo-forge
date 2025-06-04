package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JetpackManager {
    private static final Map<UUID, JetpackHandler> playerJetpackHandlers = new HashMap<>();

    public static JetpackHandler getJetpackHandler(@NotNull Player player) {
        return playerJetpackHandlers.computeIfAbsent(player.getUUID(), id -> new JetpackHandler(player));
    }

    // Called when a player joins the server
    public static void onPlayerJoin(ServerPlayer player) {
        getJetpackHandler(player);  // Ensures the JetpackHandler is instantiated
    }

    // Called when a player leaves the server
    public static void onPlayerLeave(@NotNull ServerPlayer player) {
        playerJetpackHandlers.remove(player.getUUID());  // Cleanup when player leaves
    }
}
