package net.fxnt.fxntstorage.compat.sable;

import net.fxnt.fxntstorage.init.ModCompats;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

public class SableCompat {
    private static final boolean LOADED = ModList.get().isLoaded(ModCompats.SABLE);

    public static boolean isInPlotGrid(BlockEntity be) {
        if (!LOADED) return false;
        return SableHelper.isInPlotGrid(be);
    }

    public static Direction getAttackedFaceInSubLevel(BlockEntity blockEntity, AABB bounds, double reach, Player player) {
        if (!LOADED) return null;
        return SableHelper.getAttackedFaceInSubLevel(blockEntity, bounds, reach, player);
    }
}
