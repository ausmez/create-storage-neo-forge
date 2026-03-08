package net.fxnt.fxntstorage.controller;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StorageInterface extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<StorageInterface> CODEC = simpleCodec(StorageInterface::new);

    public StorageInterface(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityType<?> type = ModBlockEntities.STORAGE_INTERFACE_ENTITY.get();
        return new StorageInterfaceEntity(type, pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.STORAGE_INTERFACE_ENTITY.get(), (l, p, s, entity) -> {
            if (entity instanceof StorageInterfaceEntity) {
                entity.serverTick(l, p, s);
            }
        });
    }
}
