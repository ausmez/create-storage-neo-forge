package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.fxnt.fxntstorage.config.ConfigManager.ClientConfig;
import net.fxnt.fxntstorage.config.ConfigManager.ClientConfig.SimpleStorageBoxConnectedTextures;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SimpleStorageBoxCTBehaviour extends ConnectedTextureBehaviour.Base {

    private final CTSpriteShiftEntry shift;
    private final Supplier<? extends Block> matchingTrim;

    public SimpleStorageBoxCTBehaviour(CTSpriteShiftEntry shift, Supplier<? extends Block> matchingTrim) {
        this.shift = shift;
        this.matchingTrim = matchingTrim;
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (ClientConfig.SIMPLE_STORAGE_CONNECTED_TEXTURES.get() == SimpleStorageBoxConnectedTextures.OFF)
            return null;
        // The front / display face must never connect
        if (state.hasProperty(SimpleStorageBox.FACING) && direction == state.getValue(SimpleStorageBox.FACING))
            return null;
        return shift;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
        // Same-wood / trim / blocked decision (the 6-arg logic below)
        if (!connectsTo(state, other, reader, pos, otherPos, face))
            return false;

        return !other.hasProperty(SimpleStorageBox.FACING) || other.getValue(SimpleStorageBox.FACING) != face;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face) {
        if (isBeingBlocked(state, reader, pos, otherPos, face))
            return false;

        SimpleStorageBoxConnectedTextures mode = ClientConfig.SIMPLE_STORAGE_CONNECTED_TEXTURES.get();
        if (mode == SimpleStorageBoxConnectedTextures.OFF)
            return false;

        Block otherBlock = other.getBlock();
        // Same-wood box to box (each wood is a distinct block)
        if (otherBlock == state.getBlock())
            return true;

        // Same-wood Storage Trim, only when selected
        if (mode == SimpleStorageBoxConnectedTextures.BOXES_AND_TRIM) {
            Block trim = matchingTrim.get();
            return trim != null && otherBlock == trim;
        }

        return false;
    }
}
