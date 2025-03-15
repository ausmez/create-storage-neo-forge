package net.fxnt.fxntstorage.backpacks.upgrades;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JetpackManager {
    private static final Map<UUID, JetpackHandler> playerJetpackHandlers = new HashMap<>();

    // Method to get the JetpackHandler for a player
    public static JetpackHandler getJetpackHandler(Player player) {
        return playerJetpackHandlers.computeIfAbsent(player.getUUID(), id -> new JetpackHandler(player));
    }

    // Called when a player joins the server
    public static void onPlayerJoin(ServerPlayer player) {
        getJetpackHandler(player);  // Ensures the JetpackHandler is instantiated
    }

    // Called when a player leaves the server
    public static void onPlayerLeave(ServerPlayer player) {
        playerJetpackHandlers.remove(player.getUUID());  // Cleanup when player leaves
    }
}
