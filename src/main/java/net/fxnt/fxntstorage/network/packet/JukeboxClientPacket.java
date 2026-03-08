package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.jukebox.ClientJukeboxHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record JukeboxClientPacket(Action action, Source source, Optional<BlockPos> pos, Optional<ResourceLocation> song,
                                  boolean muted) {

    public enum Action {PLAY, STOP}

    public enum Source {PLAYER, BLOCK}

    public static void encode(JukeboxClientPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.source);
        buffer.writeOptional(packet.pos, FriendlyByteBuf::writeBlockPos);
        buffer.writeOptional(packet.song, FriendlyByteBuf::writeResourceLocation);
        buffer.writeBoolean(packet.muted);
    }

    public static JukeboxClientPacket decode(FriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        Source source = buffer.readEnum(Source.class);
        Optional<BlockPos> pos = buffer.readOptional(FriendlyByteBuf::readBlockPos);
        Optional<ResourceLocation> song = buffer.readOptional(FriendlyByteBuf::readResourceLocation);
        boolean muted = buffer.readBoolean();
        return new JukeboxClientPacket(action, source, pos, song, muted);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(JukeboxClientPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();

        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            switch (packet.action()) {
                case PLAY -> handlePlay(packet, player);
                case STOP -> handleStop(packet, player);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handlePlay(JukeboxClientPacket packet, Player player) {
        if (packet.source() == Source.PLAYER) {
            if (ClientJukeboxHandler.isPlayerPlaying(player)) return;
            packet.song().ifPresent(jukeboxSong -> {
                ClientJukeboxHandler.PlayerPlayback playback = new ClientJukeboxHandler.PlayerPlayback(player.getUUID(), null, jukeboxSong, packet.muted());
                ClientJukeboxHandler.playPlayer(playback);
            });
        } else {
            packet.pos().ifPresent(blockPos -> {
                if (ClientJukeboxHandler.isBlockPlaying(blockPos)) return;
                packet.song().ifPresent(jukeboxSong -> {
                    ClientJukeboxHandler.BlockPlayback playback = new ClientJukeboxHandler.BlockPlayback(blockPos, null, player.level().dimension(), jukeboxSong, packet.muted());
                    ClientJukeboxHandler.playBlock(playback);
                });
            });
        }
    }

    private static void handleStop(JukeboxClientPacket packet, Player player) {
        if (packet.source() == Source.PLAYER) {
            ClientJukeboxHandler.stopPlayer(player.getUUID());
        } else {
            packet.pos().ifPresent(ClientJukeboxHandler::stopBlock);
        }
    }
}
