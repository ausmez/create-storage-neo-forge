package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TransferRecipePacket(ResourceLocation recipeId, List<Integer> recipeList, boolean maxTransfer,
                                   byte action) {

    public static void encode(@NotNull TransferRecipePacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
        buffer.writeCollection(packet.recipeList, FriendlyByteBuf::writeInt);
        buffer.writeBoolean(packet.maxTransfer);
        buffer.writeByte(packet.action);
    }

    public static @NotNull TransferRecipePacket decode(@NotNull FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        List<Integer> recipeList = buffer.readList(FriendlyByteBuf::readInt);
        boolean maxTransfer = buffer.readBoolean();
        byte action = buffer.readByte();
        return new TransferRecipePacket(recipeId, recipeList, maxTransfer, action);
    }

}
