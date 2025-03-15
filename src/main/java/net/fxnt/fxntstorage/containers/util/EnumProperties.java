package net.fxnt.fxntstorage.containers.util;

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

    public enum Variant implements StringRepresentable {
        ACACIA, BAMBOO, BIRCH, CHERRY, CRIMSON, DARK_OAK, JUNGLE, MANGROVE, OAK, SPRUCE, WARPED;

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
