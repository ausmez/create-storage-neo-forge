package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TorchDeployerManager {
    private static final Map<UUID, Long> lastPlacementTime = new HashMap<>();

    public static boolean canPlaceTorch(Player player, long cooldownTicks) {
        long gameTime = player.level().getGameTime();
        UUID id = player.getUUID();

        // Joining world
        if (!lastPlacementTime.containsKey(id)) {
            lastPlacementTime.put(id, gameTime);
            return false;
        }

        long last = lastPlacementTime.getOrDefault(id, 0L);

        if (gameTime - last >= cooldownTicks) {
            lastPlacementTime.put(id, gameTime);
            return true;
        }
        return false;
    }

    public static void resetCooldown(Player player) {
        lastPlacementTime.put(player.getUUID(), player.level().getGameTime());
    }

    public static void removePlayer(ServerPlayer player) {
        lastPlacementTime.remove(player.getUUID());
    }

}
