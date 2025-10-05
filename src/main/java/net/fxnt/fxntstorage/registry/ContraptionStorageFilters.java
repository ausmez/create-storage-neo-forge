package net.fxnt.fxntstorage.registry;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public class ContraptionStorageFilters {
    private static final Map<Contraption, ContraptionStorageFilters> CONTRAPTION_REGISTRY =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static ContraptionStorageFilters getOrCreate(Contraption contraption) {
        return CONTRAPTION_REGISTRY.computeIfAbsent(contraption, c -> {
            return new ContraptionStorageFilters();
        });
    }

    public static void cleanupContraption(Contraption contraption) {
        CONTRAPTION_REGISTRY.remove(contraption);
    }

    // Per-contraption data
    private final Map<FilterItemStack, Set<FilteredMountedStorage>> filters = new HashMap<>();

    public void register(FilteredMountedStorage storage, ItemStack filterItem) {
        if (filterItem.isEmpty()) return;
        register(storage, FilterItemStack.of(filterItem));
    }

    public void register(FilteredMountedStorage storage, FilterItemStack filterItemStack) {
        if (filterItemStack == null || filterItemStack.isEmpty()) return;
        filters.computeIfAbsent(filterItemStack, k -> new HashSet<>()).add(storage);
    }

    public void unregister(FilteredMountedStorage storage) {
        for (Iterator<Map.Entry<FilterItemStack, Set<FilteredMountedStorage>>> it = filters.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<FilterItemStack, Set<FilteredMountedStorage>> entry = it.next();
            entry.getValue().remove(storage);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    public boolean matches(Level level, ItemStack stack) {
        for (FilterItemStack filter : filters.keySet()) {
            if (filter.test(level, stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    public interface FilteredMountedStorage {
    }

}
