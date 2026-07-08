package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.fxnt.fxntstorage.config.ConfigManager.ClientConfig;
import net.fxnt.fxntstorage.config.ConfigManager.ClientConfig.SimpleStorageBoxConnectedTextures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class StorageTrimCTBehaviour extends EncasedCTBehaviour {

    private final Supplier<? extends Block> matchingBox;

    public StorageTrimCTBehaviour(CTSpriteShiftEntry shift, Supplier<? extends Block> matchingBox) {
        super(shift);
        this.matchingBox = matchingBox;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
        // Trim / casing / box decision (the 6-arg logic below)
        if (!connectsTo(state, other, reader, pos, otherPos, face))
            return false;

        Block box = matchingBox.get();
        return box == null || other.getBlock() != box
                || !other.hasProperty(SimpleStorageBox.FACING)
                || other.getValue(SimpleStorageBox.FACING) != face;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face) {
        // Preserve existing trim<->trim and trim<->Create-casing connectivity
        if (super.connectsTo(state, other, reader, pos, otherPos, face))
            return true;

        if (ClientConfig.SIMPLE_STORAGE_CONNECTED_TEXTURES.get() != SimpleStorageBoxConnectedTextures.BOXES_AND_TRIM)
            return false;
        if (isBeingBlocked(state, reader, pos, otherPos, face))
            return false;

        Block box = matchingBox.get();
        return box != null && other.getBlock() == box;
    }
}
