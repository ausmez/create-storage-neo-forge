package net.fxnt.fxntstorage.container.util;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class EnumProperties {
    public enum StorageUsed implements StringRepresentable {
        EMPTY, HAS_ITEMS, SLOTS_FILLED, FULL;

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

    }

}
