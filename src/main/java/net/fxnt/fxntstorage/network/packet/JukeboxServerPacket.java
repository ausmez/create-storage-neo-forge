package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxPlayable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Optional;

public record JukeboxServerPacket(Action action, Source source, Optional<BlockPos> pos) implements CustomPacketPayload {

    public enum Action {PLAY, STOP, TOGGLE_PLAYING, TOGGLE_MUTED}

    public enum Source {PLAYER, BLOCK}

    public static final Type<JukeboxServerPacket> TYPE = new Type<>(FXNTStorage.modLoc("jukebox_server"));

    public static final StreamCodec<FriendlyByteBuf, JukeboxServerPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Action.class), JukeboxServerPacket::action,
            NeoForgeStreamCodecs.enumCodec(Source.class), JukeboxServerPacket::source,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), JukeboxServerPacket::pos,
            JukeboxServerPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof BackpackMenu menu) {
                switch (action()) {
                    case PLAY -> handlePlay(player);
                    case STOP -> handleStop(player);
                    case TOGGLE_PLAYING -> handleTogglePlaying(player);
                    case TOGGLE_MUTED -> handleToggleMuted(player);
                }
                menu.updateBackpackDataFromContainer();
            }
        });
    }

    private void handlePlay(ServerPlayer player) {
        if (source() == Source.PLAYER) {
            playMusic(player, null);
        } else {
            pos().ifPresent(blockPos -> playMusic(player, blockPos));
        }
    }

    private void handleStop(ServerPlayer player) {
        if (source() == Source.PLAYER) {
            JukeboxHandler.stopPlayer(player);
        } else {
            pos().ifPresent(blockPos -> JukeboxHandler.stopBlock(player.level(), blockPos));
        }
    }

    private void handleTogglePlaying(ServerPlayer player) {
        if (source() == Source.PLAYER) {
            if (JukeboxHandler.isPlayerPlaying(player)) {
                JukeboxHandler.stopPlayer(player);
            } else {
                playMusic(player, null);
            }
        } else {
            pos().ifPresent(blockPos -> {
                if (JukeboxHandler.isBlockPlaying(player.serverLevel(), blockPos)) {
                    JukeboxHandler.stopBlock(player.level(), blockPos);
                } else {
                    playMusic(player, blockPos);
                }
            });
        }
    }

    private void handleToggleMuted(ServerPlayer player) {
        if (source() == Source.PLAYER) {
            JukeboxHandler.togglePlayerMuted(player);
        } else {
            pos().ifPresent(blockPos -> JukeboxHandler.toggleBlockMuted(player, blockPos));
        }
    }

    private void playMusic(ServerPlayer player, @Nullable BlockPos blockPos) {
        JukeboxUpgradeHelper.getMusicDisc(player, player.level(), blockPos).ifPresent(musicDisc -> {
            JukeboxPlayable playable = musicDisc.get(DataComponents.JUKEBOX_PLAYABLE);
            if (playable != null) {
                ResourceLocation songId = playable.song().key().location();

                if (blockPos == null) {
                    JukeboxHandler.playPlayer(player, songId);
                } else {
                    JukeboxHandler.playBlock(player, blockPos, songId);
                }
            }
        });
    }
}