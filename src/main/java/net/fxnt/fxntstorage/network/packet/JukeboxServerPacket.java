package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public record JukeboxServerPacket(Action action, Source source, Optional<BlockPos> pos) {

    public enum Action {PLAY, STOP, TOGGLE_PLAYING, TOGGLE_MUTED}

    public enum Source {PLAYER, BLOCK}

    public static void encode(JukeboxServerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.source);
        buffer.writeOptional(packet.pos, FriendlyByteBuf::writeBlockPos);
    }

    public static JukeboxServerPacket decode(FriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        Source source = buffer.readEnum(Source.class);
        Optional<BlockPos> pos = buffer.readOptional(FriendlyByteBuf::readBlockPos);
        return new JukeboxServerPacket(action, source, pos);
    }

    public static void handle(JukeboxServerPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (player.containerMenu instanceof BackpackMenu menu) {
                switch (packet.action()) {
                    case PLAY -> handlePlay(packet, player);
                    case STOP -> handleStop(packet, player);
                    case TOGGLE_PLAYING -> handleTogglePlaying(packet, player);
                    case TOGGLE_MUTED -> handleToggleMuted(packet, player);
                }
                menu.updateBackpackDataFromContainer();
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handlePlay(JukeboxServerPacket packet, ServerPlayer player) {
        if (packet.source() == Source.PLAYER) {
            playMusic(packet, player, null);
        } else {
            packet.pos().ifPresent(blockPos -> playMusic(packet, player, blockPos));
        }
    }

    private static void handleStop(JukeboxServerPacket packet, ServerPlayer player) {
        if (packet.source() == Source.PLAYER) {
            JukeboxHandler.stopPlayer(player);
        } else {
            packet.pos().ifPresent(blockPos -> JukeboxHandler.stopBlock(player.level(), blockPos));
        }
    }

    private static void handleTogglePlaying(JukeboxServerPacket packet, ServerPlayer player) {
        if (packet.source() == Source.PLAYER) {
            if (JukeboxHandler.isPlayerPlaying(player)) {
                JukeboxHandler.stopPlayer(player);
            } else {
                playMusic(packet, player, null);
            }
        } else {
            packet.pos().ifPresent(blockPos -> {
                if (JukeboxHandler.isBlockPlaying(player.serverLevel(), blockPos)) {
                    JukeboxHandler.stopBlock(player.level(), blockPos);
                } else {
                    playMusic(packet, player, blockPos);
                }
            });
        }
    }

    private static void handleToggleMuted(JukeboxServerPacket packet, ServerPlayer player) {
        if (packet.source() == Source.PLAYER) {
            JukeboxHandler.togglePlayerMuted(player);
        } else {
            packet.pos().ifPresent(blockPos -> JukeboxHandler.toggleBlockMuted(player, blockPos));
        }
    }

    private static void playMusic(JukeboxServerPacket packet, ServerPlayer player, @Nullable BlockPos blockPos) {
        JukeboxUpgradeHelper.getMusicDisc(player, player.level(), blockPos).ifPresent(stack -> {
            if (stack.getItem() instanceof RecordItem recordItem) {
                ResourceLocation songId = ForgeRegistries.SOUND_EVENTS.getKey(recordItem.getSound());
                if (songId == null) return;

                if (blockPos == null) {
                    JukeboxHandler.playPlayer(player, songId);
                } else {
                    JukeboxHandler.playBlock(player, blockPos, songId);
                }
            }
        });
    }
}
