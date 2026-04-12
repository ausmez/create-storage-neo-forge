package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record JetpackStateResetPacket() {

    public static void encode(JetpackStateResetPacket packet, FriendlyByteBuf buffer) {
    }

    public static JetpackStateResetPacket decode(FriendlyByteBuf buffer) {
        return new JetpackStateResetPacket();
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(JetpackStateResetPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            JetpackManager.getJetpackHandler(player).endHovering(false);
            JetpackManager.getJetpackHandler(player).flyingOnKeyRelease();
        });
        context.get().setPacketHandled(true);
    }
}
