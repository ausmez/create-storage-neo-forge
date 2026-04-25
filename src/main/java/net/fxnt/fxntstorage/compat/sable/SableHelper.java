package net.fxnt.fxntstorage.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SableHelper {
    static boolean isInPlotGrid(BlockEntity blockEntity) {
        return Sable.HELPER.isInPlotGrid(blockEntity);
    }

    static Direction getAttackedFaceInSubLevel(BlockEntity blockEntity, AABB bounds, double reach, Player player) {
        SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);
        if (subLevel == null) return null;

        var pose = subLevel.logicalPose();

        Vec3 eye = pose.transformPositionInverse(player.getEyePosition(1.0f));
        Vec3 look = pose.transformNormalInverse(player.getViewVector(1.0f));

        return bounds.clip(eye, eye.add(look.scale(reach)))
                .map(hit -> Util.nearestBlockFace(hit, bounds))
                .orElse(null);
    }
}
