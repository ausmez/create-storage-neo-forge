package net.fxnt.fxntstorage.storage_network;

import com.simibubi.create.api.packager.InventoryIdentifier;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;

import java.util.Set;

public record StorageNetworkIdentifier(BlockPos controllerPos, Set<BlockPos> memberPositions) implements InventoryIdentifier {
    @Override
    public boolean contains(BlockFace face) {
        return memberPositions.contains(face.getPos());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StorageNetworkIdentifier other)) return false;
        return controllerPos.equals(other.controllerPos);
    }

    @Override
    public int hashCode() {
        return controllerPos.hashCode();
    }
}
