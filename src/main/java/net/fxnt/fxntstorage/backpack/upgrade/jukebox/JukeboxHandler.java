package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.JukeboxClientPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JukeboxHandler {
    private static final Map<UUID, PlayerPlayback> playerSounds = new ConcurrentHashMap<>();
    private static final Map<BlockKey, BlockPlayback> blockSounds = new ConcurrentHashMap<>();

    public record PlayerPlayback(UUID playerId, ResourceLocation song, boolean muted) {
        public PlayerPlayback toggleMuted() {
            return new PlayerPlayback(playerId, song, !muted);
        }
    }

    public record BlockPlayback(BlockPos pos, ResourceKey<Level> dimension, ResourceLocation song, boolean muted,
                                Set<UUID> listeners) {
        public BlockPlayback toggleMuted() {
            return new BlockPlayback(pos, dimension, song, !muted, listeners);
        }
    }

    // PLAYER API
    public static void playPlayer(ServerPlayer player, ResourceLocation song) {
        stopPlayer(player);

        PlayerPlayback playback = new PlayerPlayback(player.getUUID(), song, false);
        playerSounds.put(player.getUUID(), playback);

        JukeboxBuffHandler.applyMusicBuffsFromPlayer(player, song);
        PacketDistributor.sendToPlayer(player,
                new JukeboxClientPacket(JukeboxClientPacket.Action.PLAY, JukeboxClientPacket.Source.PLAYER,
                        Optional.empty(), Optional.of(playback.song()), playback.muted()));
    }

    public static void stopPlayer(ServerPlayer player) {
        PlayerPlayback removed = playerSounds.remove(player.getUUID());

        if (removed != null) {
            JukeboxBuffHandler.removePlayerBuff(player, removed.song);
            PacketDistributor.sendToPlayer(player,
                    new JukeboxClientPacket(JukeboxClientPacket.Action.STOP, JukeboxClientPacket.Source.PLAYER,
                            Optional.empty(), Optional.empty(), false));
        }
    }

    public static boolean isPlayerPlaying(ServerPlayer player) {
        return playerSounds.containsKey(player.getUUID());
    }

    public static boolean isPlayerMuted(ServerPlayer player) {
        if (playerSounds.containsKey(player.getUUID())) {
            return playerSounds.get(player.getUUID()).muted();
        }
        return false;
    }

    public static void togglePlayerMuted(ServerPlayer player) {
        playerSounds.computeIfPresent(player.getUUID(), ((uuid, playback) -> playback.toggleMuted()));
    }

    // BLOCK API
    public static void playBlock(ServerPlayer player, BlockPos pos, ResourceLocation song) {
        ServerLevel level = player.serverLevel();
        stopBlock(level, pos);

        BlockPlayback playback = new BlockPlayback(pos, level.dimension(), song, false, new HashSet<>());
        blockSounds.put(BlockKey.of(level, pos), playback);

        JukeboxBuffHandler.applyMusicBuffsFromBlock(player, song);
        PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.get(),
                new JukeboxClientPacket(JukeboxClientPacket.Action.PLAY, JukeboxClientPacket.Source.BLOCK, Optional.of(playback.pos()), Optional.of(playback.song()), false));
    }

    public static void stopBlock(Level level, BlockPos pos) {
        BlockKey key = BlockKey.of(level, pos);
        BlockPlayback removed = blockSounds.remove(key);

        if (removed != null) {
            PacketDistributor.sendToPlayersNear((ServerLevel) level, null, pos.getX(), pos.getY(), pos.getZ(), ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.get(),
                    new JukeboxClientPacket(JukeboxClientPacket.Action.STOP, JukeboxClientPacket.Source.BLOCK, Optional.of(pos), Optional.empty(), false));
        }
    }

    public static boolean isBlockPlaying(Level level, BlockPos pos) {
        BlockKey key = BlockKey.of(level, pos);

        return blockSounds.containsKey(key);
    }

    public static boolean isBlockMuted(Level level, BlockPos pos) {
        BlockKey key = BlockKey.of(level, pos);

        if (blockSounds.containsKey(key)) {
            return blockSounds.get(key).muted();
        }
        return false;
    }

    public static void syncBlocksToPlayers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        int range = ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.get();

        for (BlockPlayback playback : blockSounds.values()) {
            if (!playback.dimension().equals(level.dimension())) continue;

            BlockPos pos = playback.pos();
            double maxDist = range * range;

            if (player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            ) > maxDist) continue;

            JukeboxBuffHandler.applyMusicBuffsFromBlock(player, playback.song());

            PacketDistributor.sendToPlayer(player,
                    new JukeboxClientPacket(
                            JukeboxClientPacket.Action.PLAY,
                            JukeboxClientPacket.Source.BLOCK,
                            Optional.of(pos),
                            Optional.of(playback.song()),
                            playback.muted()
                    ));
        }
    }

    public static void toggleBlockMuted(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockKey key = BlockKey.of(level, pos);

        blockSounds.computeIfPresent(key, ((block, playback) -> playback.toggleMuted()));
    }

    public record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
        public BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos.immutable();
        }

        public static BlockKey of(Level level, BlockPos pos) {
            return new BlockKey(level.dimension(), pos);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockKey(ResourceKey<Level> dimension1, BlockPos pos1))) return false;
            return dimension.equals(dimension1)
                    && pos.equals(pos1);
        }
    }
}
