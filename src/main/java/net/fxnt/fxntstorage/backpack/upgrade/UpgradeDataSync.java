package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.world.inventory.ContainerData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class UpgradeDataSync implements ContainerData {
    public enum Field {
        FEEDER_ALLOW_CHORUS_FRUIT(0, "feeder_allow_chorus_fruit"),
        FEEDER_DISPLAY_MESSAGE(1, "feeder_display_message"),
        JETPACK_OVERLAY(2, "jetpack_overlay"),
        JETPACK_BOBBING(3, "jetpack_bobbing"),
        JUKEBOX_PLAYING(4, "jukebox_playing"),
        JUKEBOX_MUTED(5, "jukebox_muted"),
        MAGNET_IGNORE_FAN(6, "magnet_ignore_fan"),
        OREMINING_ORES_ONLY(7, "oremining_ores_only"),
        OREMINING_PREVIEW_ORE_VEIN(8, "oremining_preview_ore_vein"),
        TOOLSWAP_PREFER_SWORD(9, "toolswap_prefer_sword"),
        TOOLSWAP_PREFER_SILKTOUCH(10, "toolswap_prefer_silktouch"),
        EXPANDED_PANELS(11, "expanded_panels"),
        WORKSHOP_PROCESSING(12, "workshop_processing");

        private final int index;
        private final String id;

        Field(int index, String id) {
            this.index = index;
            this.id = id;
        }

        public int getIndex() {
            return index;
        }

        public String getId() {
            return id;
        }

        private static final Field[] BY_INDEX;
        private static final Map<String, Field> BY_ID = new HashMap<>();

        static {
            int max = -1;
            for (Field field : values()) {
                max = Math.max(max, field.index);
            }

            BY_INDEX = new Field[max + 1];

            for (Field field : values()) {
                if (BY_INDEX[field.index] != null) {
                    throw new IllegalStateException(
                            "Duplicate Field index: " + field.index
                    );
                }
                BY_INDEX[field.index] = field;

                if (BY_ID.put(field.id, field) != null) {
                    throw new IllegalStateException(
                            "Duplicate Field id: " + field.id
                    );
                }
            }
        }

        public static Field fromIndex(int index) {
            if (index < 0 || index >= BY_INDEX.length) return null;
            return BY_INDEX[index];
        }
    }

    private static final int DATA_SIZE = Field.values().length;

    private final int[] data = new int[DATA_SIZE];
    private final Map<Field, IntSupplier> serverGetters = new EnumMap<>(Field.class);
    private final Map<Field, BiConsumer<Integer, Boolean>> serverSettersBool = new EnumMap<>(Field.class);
    private final Map<Field, BiConsumer<Integer, Integer>> serverSettersInt = new EnumMap<>(Field.class);

    public UpgradeDataSync() {
    }

    public void registerBoolean(Field field, BooleanSupplier getter, BiConsumer<Integer, Boolean> setter) {
        serverGetters.put(field, () -> getter.getAsBoolean() ? 1 : 0);
        if (setter != null) {
            serverSettersBool.put(field, setter);
        }
    }

    public void registerBoolean(Field field, BooleanSupplier supplier) {
        registerBoolean(field, supplier, null);
    }

    public void registerInteger(Field field, IntSupplier getter, BiConsumer<Integer, Integer> setter) {
        serverGetters.put(field, getter);
        if (setter != null) {
            serverSettersInt.put(field, setter);
        }
    }

    public void updateFromSuppliers() {
        for (Map.Entry<Field, IntSupplier> entry : serverGetters.entrySet()) {
            data[entry.getKey().getIndex()] = entry.getValue().getAsInt();
        }
    }

    public void setLocalBoolValue(Field field, boolean value) {
        data[field.getIndex()] = value ? 1 : 0;
    }

    public void setLocalIntValue(Field field, int value) {
        data[field.getIndex()] = value;
    }

    public boolean getBoolean(Field field) {
        return data[field.getIndex()] != 0;
    }

    public int getInteger(Field field) {
        return data[field.getIndex()];
    }

    @Override
    public int get(int index) {
        if (index >= 0 && index < DATA_SIZE) {
            return data[index];
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        if (index >= 0 && index < DATA_SIZE) {
            int oldValue = data[index];
            data[index] = value;

            // Notify server-side setters when value changes from client
            // This happens during ContainerData synchronization from client to server
            if (oldValue != value) {
                Field field = Field.fromIndex(index);

                if (field != null) {

                    if (serverSettersBool.containsKey(field)) {
                        serverSettersBool.get(field).accept(index, value != 0);
                    }

                    if (serverSettersInt.containsKey(field)) {
                        serverSettersInt.get(field).accept(index, value);
                    }
                }
            }
        }
    }

    @Override
    public int getCount() {
        return DATA_SIZE;
    }

    public static class Builder {
        private final UpgradeDataSync sync = new UpgradeDataSync();

        public Builder withBoolean(Field field, BooleanSupplier supplier) {
            sync.registerBoolean(field, supplier);
            return this;
        }

        public Builder withBoolean(Field field, BooleanSupplier getter, BiConsumer<Integer, Boolean> setter) {
            sync.registerBoolean(field, getter, setter);
            return this;
        }

        public Builder withInteger(Field field, IntSupplier getter, BiConsumer<Integer, Integer> setter) {
            sync.registerInteger(field, getter, setter);
            return this;
        }

        public UpgradeDataSync build() {
            return sync;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
