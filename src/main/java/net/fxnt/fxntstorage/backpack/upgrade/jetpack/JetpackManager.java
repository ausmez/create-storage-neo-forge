package net.fxnt.fxntstorage.backpack.upgrade.jetpack;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.JetpackFuelSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JetpackManager {
    private static final Map<UUID, JetpackHandler> SERVER = new HashMap<>();
    private static final Map<UUID, JetpackHandler> CLIENT = new HashMap<>();

    public static JetpackHandler getJetpackHandler(@NotNull Player player) {
        return (player.level().isClientSide)
                ? CLIENT.computeIfAbsent(player.getUUID(), uuid -> new JetpackHandler(player))
                : SERVER.computeIfAbsent(player.getUUID(), uuid -> new JetpackHandler(player));
    }

    // Called when a player joins the server
    public static void addPlayer(@NotNull Player player) {
        removePlayer(player); // remove stale handler

        JetpackHandler handler = getJetpackHandler(player);
        handler.resetState();

        if (!player.level().isClientSide) {
            // Immediately sync fuel so client isn't stuck at 0
            double fuel = handler.calculateJetPackFuel(player);
            ModNetwork.sendToPlayer((ServerPlayer) player, new JetpackFuelSyncPacket((float) fuel, System.currentTimeMillis()));
        }
    }

    // Called when a player leaves the server
    public static void removePlayer(@NotNull Player player) {
        if (player.level().isClientSide) {
            CLIENT.remove(player.getUUID());
        } else {
            SERVER.remove(player.getUUID());
        }
    }
}
