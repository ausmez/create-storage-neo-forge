package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopUpgrade;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorkshopSoundPacket(WorkshopUpgrade.WorkshopSound sound, BlockPos pos) implements CustomPacketPayload {
    public static final Type<WorkshopSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "workshop_sound"));

    public static final StreamCodec<FriendlyByteBuf, WorkshopSoundPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> WorkshopUpgrade.WorkshopSound.values()[i], WorkshopUpgrade.WorkshopSound::ordinal), WorkshopSoundPacket::sound,
            BlockPos.STREAM_CODEC, WorkshopSoundPacket::pos,
            WorkshopSoundPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!ConfigManager.ClientConfig.WORKSHOP_SOUNDS.get()) return;
            Player player = context.player();
            sound.playLocal(player.level(), pos);
        });
    }
}