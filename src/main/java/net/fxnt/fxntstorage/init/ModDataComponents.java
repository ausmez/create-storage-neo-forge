package net.fxnt.fxntstorage.init;

import com.mojang.serialization.Codec;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FXNTStorage.MOD_ID);

    public static final DataComponentType<Boolean> VOID_UPGRADE = register(
            "void_upgrade",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<List<String>> BACKPACK_UPGRADES = register(
            "backpack_upgrades",
            builder -> builder.persistent(Codec.list(Codec.STRING)).networkSynchronized(ByteBufCodecs.fromCodec(Codec.list(Codec.STRING)))
    );

    public static final DataComponentType<Integer> BACKPACK_STACK_MULTIPLIER = register(
            "backpack_stack_multiplier",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<SortOrder> INVENTORY_SORT_ORDER = register(
            "inventory_sort_order",
            builder -> builder.persistent(SortOrder.CODEC).networkSynchronized(SortOrder.STREAM_CODEC)
    );

    public static final DataComponentType<Integer> BACKPACK_ACTIVE_PANELS = register(
            "backpack_active_panel",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Boolean> BACKPACK_PREFER_SWORD = register(
            "backpack_prefer_sword",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_PREFER_SILKTOUCH = register(
            "backpack_prefer_silktouch",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_IGNORE_FAN = register(
            "backpack_ignore_fan",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_JETPACK_BOBBING = register(
            "backpack_jetpack_bobbing",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_JETPACK_OVERLAY = register(
            "backpack_jetpack_overlay",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_FEEDER_CHORUS = register(
            "backpack_feeder_chorus",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_FEEDER_MESSAGE = register(
            "backpack_feeder_message",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_OREMINING_ORES_ONLY = register(
            "backpack_oremining_ores_only",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> BACKPACK_OREMINING_PREVIEW_VEIN = register(
            "backpack_oremining_preview_vein",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    private static final Map<UpgradeDataSync.Field, DataComponentType<Boolean>>
            FIELD_COMPONENT_MAP = Map.ofEntries(
            // Feeder upgrade
            Map.entry(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, BACKPACK_FEEDER_CHORUS),
            Map.entry(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE, BACKPACK_FEEDER_MESSAGE),

            // Jetpack upgrade
            Map.entry(UpgradeDataSync.Field.JETPACK_OVERLAY, BACKPACK_JETPACK_OVERLAY),
            Map.entry(UpgradeDataSync.Field.JETPACK_BOBBING, BACKPACK_JETPACK_BOBBING),

            // Magnet upgrade
            Map.entry(UpgradeDataSync.Field.MAGNET_IGNORE_FAN, BACKPACK_IGNORE_FAN),

            // Ore Mining upgrade
            Map.entry(UpgradeDataSync.Field.OREMINING_ORES_ONLY, BACKPACK_OREMINING_ORES_ONLY),
            Map.entry(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN, BACKPACK_OREMINING_PREVIEW_VEIN),

            // Tool Swap upgrade
            Map.entry(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD, BACKPACK_PREFER_SWORD),
            Map.entry(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH, BACKPACK_PREFER_SILKTOUCH)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENT_TYPES.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

    public static DataComponentType<Boolean> getComponentForField(UpgradeDataSync.Field field) {
        return FIELD_COMPONENT_MAP.get(field);
    }
}
