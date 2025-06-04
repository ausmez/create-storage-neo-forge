package net.fxnt.fxntstorage.init;

import com.mojang.serialization.Codec;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
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

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENT_TYPES.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
