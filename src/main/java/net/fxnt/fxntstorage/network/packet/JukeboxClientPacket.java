package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.ClientJukeboxHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record JukeboxClientPacket(Action action, Source source, Optional<BlockPos> pos, Optional<ResourceLocation> song,
                                  boolean muted) implements CustomPacketPayload {

    public enum Action {PLAY, STOP}

    public enum Source {PLAYER, BLOCK}

    public static final Type<JukeboxClientPacket> TYPE = new Type<>(FXNTStorage.modLoc("jukebox_client"));

    public static final StreamCodec<FriendlyByteBuf, JukeboxClientPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Action.class), JukeboxClientPacket::action,
            NeoForgeStreamCodecs.enumCodec(Source.class), JukeboxClientPacket::source,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), JukeboxClientPacket::pos,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), JukeboxClientPacket::song,
            ByteBufCodecs.BOOL, JukeboxClientPacket::muted,
            JukeboxClientPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof LocalPlayer player) {
                switch (action()) {
                    case PLAY -> handlePlay(player);
                    case STOP -> handleStop(player);
                }
            }
        });
    }

    private void handlePlay(LocalPlayer player) {
        if (source() == Source.PLAYER) {
            if (ClientJukeboxHandler.isPlayerPlaying(player)) return;
            song().ifPresent(jukeboxSong -> {
                ClientJukeboxHandler.PlayerPlayback playback = new ClientJukeboxHandler.PlayerPlayback(player.getUUID(), null, jukeboxSong, muted());
                ClientJukeboxHandler.playPlayer(playback);
            });

        } else {
            pos().ifPresent(blockPos -> {
                if (ClientJukeboxHandler.isBlockPlaying(blockPos)) return;
                song().ifPresent(jukeboxSong -> {
                    ClientJukeboxHandler.BlockPlayback playback = new ClientJukeboxHandler.BlockPlayback(blockPos, null, player.level().dimension(), jukeboxSong, muted());
                    ClientJukeboxHandler.playBlock(playback);
                });
            });
        }
    }

    private void handleStop(LocalPlayer player) {
        if (source() == Source.PLAYER) {
            ClientJukeboxHandler.stopPlayer(player.getUUID());
        } else {
            pos().ifPresent(ClientJukeboxHandler::stopBlock);
        }
    }
}