package net.fxnt.fxntstorage.controller;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class StorageControllerHighlight {
    private static final Map<BlockPos, Set<BlockPos>> STORAGE_NETWORK_HIGHLIGHT = new HashMap<>();

    public static void set(BlockPos controllerPos, Set<BlockPos> boxes) {
        STORAGE_NETWORK_HIGHLIGHT.put(controllerPos, boxes);
    }

    public static Set<BlockPos> get(BlockPos controllerPos) {
        return STORAGE_NETWORK_HIGHLIGHT.getOrDefault(controllerPos, Set.of());
    }

    public static void remove(BlockPos controllerPos) {
        STORAGE_NETWORK_HIGHLIGHT.remove(controllerPos);
    }

    public static Map<BlockPos, Set<BlockPos>> getAll() {
        return STORAGE_NETWORK_HIGHLIGHT;
    }

    public static void removeAll() {
        STORAGE_NETWORK_HIGHLIGHT.clear();
    }
}
