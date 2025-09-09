package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record CrossbowChargedPacket() {

    public static void encode(@NotNull CrossbowChargedPacket packet, @NotNull FriendlyByteBuf buffer) {
    }

    public static @NotNull CrossbowChargedPacket decode(@NotNull FriendlyByteBuf buffer) {
        return new CrossbowChargedPacket();
    }

}
