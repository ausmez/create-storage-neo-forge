package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.ClientJukeboxHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record JukeboxClientPacket(Action action, Source source, Optional<BlockPos> pos, Optional<ResourceLocation> song,
                                  boolean muted, Optional<Integer> entityId) implements CustomPacketPayload {

    public enum Action {PLAY, STOP}

    public enum Source {PLAYER, BLOCK, ENTITY}

    public static final Type<JukeboxClientPacket> TYPE = new Type<>(FXNTStorage.modLoc("jukebox_client"));

    public static final StreamCodec<FriendlyByteBuf, JukeboxClientPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Action.class), JukeboxClientPacket::action,
            NeoForgeStreamCodecs.enumCodec(Source.class), JukeboxClientPacket::source,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), JukeboxClientPacket::pos,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), JukeboxClientPacket::song,
            ByteBufCodecs.BOOL, JukeboxClientPacket::muted,
            ByteBufCodecs.optional(ByteBufCodecs.INT), JukeboxClientPacket::entityId,
            JukeboxClientPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                switch (action()) {
                    case PLAY -> handlePlay(player);
                    case STOP -> handleStop(player);
                }
            }
        });
    }

    private void handlePlay(Player player) {
        switch (source()) {
            case PLAYER -> {
                if (ClientJukeboxHandler.isPlayerPlaying(player)) return;
                song().ifPresent(song -> {
                    ClientJukeboxHandler.PlayerPlayback pb = new ClientJukeboxHandler.PlayerPlayback(player.getUUID(), null, song, muted());
                    ClientJukeboxHandler.playPlayer(pb);
                });
            }
            case BLOCK -> pos().ifPresent(blockPos -> {
                if (ClientJukeboxHandler.isBlockPlaying(blockPos)) return;
                song().ifPresent(song -> {
                    ClientJukeboxHandler.BlockPlayback pb = new ClientJukeboxHandler.BlockPlayback(blockPos, null, player.level().dimension(), song, muted());
                    ClientJukeboxHandler.playBlock(pb);
                });
            });
            case ENTITY -> entityId().ifPresent(id -> {
                if (ClientJukeboxHandler.isEntityPlaying(id)) return;
                song().ifPresent(song -> {
                    ClientJukeboxHandler.EntityContraptionPlayback pb = new ClientJukeboxHandler.EntityContraptionPlayback(id, null, song, muted());
                    ClientJukeboxHandler.playEntity(pb);
                });
            });
        }
    }

    private void handleStop(Player player) {
        switch (source()) {
            case PLAYER -> ClientJukeboxHandler.stopPlayer(player.getUUID());
            case BLOCK -> pos().ifPresent(ClientJukeboxHandler::stopBlock);
            case ENTITY -> entityId().ifPresent(ClientJukeboxHandler::stopEntity);
        }
    }
}
