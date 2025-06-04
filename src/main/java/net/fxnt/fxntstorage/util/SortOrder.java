package net.fxnt.fxntstorage.util;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum SortOrder implements StringRepresentable {
    COUNT, MOD, NAME;

    public static final Codec<SortOrder> CODEC = StringRepresentable.fromEnum(SortOrder::values);
    public static final StreamCodec<FriendlyByteBuf, SortOrder> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(SortOrder.class);

    public SortOrder next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public Component getDisplayName() {
        return Component.literal(this.name().substring(0, 1));
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
