package net.fxnt.fxntstorage.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SyncClientSettingsPacket(CompoundTag settings) {

    public static void encode(@NotNull SyncClientSettingsPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.settings);
    }

    public static @NotNull SyncClientSettingsPacket decode(@NotNull FriendlyByteBuf buffer) {
        CompoundTag settings = buffer.readNbt();
        return new SyncClientSettingsPacket(settings);
    }

}
