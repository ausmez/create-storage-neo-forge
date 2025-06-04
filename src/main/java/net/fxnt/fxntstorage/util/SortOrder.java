package net.fxnt.fxntstorage.util;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum SortOrder implements StringRepresentable {
    COUNT, MOD, NAME;

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
