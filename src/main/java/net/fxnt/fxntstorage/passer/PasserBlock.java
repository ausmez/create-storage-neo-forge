package net.fxnt.fxntstorage.passer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.fxnt.fxntstorage.cache.PasserShapeCache;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PasserBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<PasserBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockBehaviour.propertiesCodec(),
                    Codec.BOOL.fieldOf("is_smart").forGetter(PasserBlock::getIsSmart)
            ).apply(instance, PasserBlock::new));
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED;
    public final boolean isSmart;

    static {
        POWERED = BlockStateProperties.POWERED;
    }

    public PasserBlock(Properties pProperties, boolean isSmart) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.DOWN)
                .setValue(POWERED, false)
        );
        this.isSmart = isSmart;
    }

    public boolean getIsSmart() {
        return isSmart;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        if (this.isSmart) {
            BlockEntityType<?> type = ModBlockEntities.SMART_PASSER_ENTITY.get();
            return new PasserSmartEntity(type, pPos, pState);
        } else {
            BlockEntityType<?> type = ModBlockEntities.PASSER_ENTITY.get();
            return new PasserEntity(type, pPos, pState);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> blockEntityType) {
        if (this.isSmart) {
            return createTickerHelper(blockEntityType, ModBlockEntities.SMART_PASSER_ENTITY.get(), (type, world, pos, entity) -> entity.serverTick(type, world));
        } else {
            return createTickerHelper(blockEntityType, ModBlockEntities.PASSER_ENTITY.get(), (type, world, pos, entity) -> entity.serverTick(type, world));
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Block pNeighborBlock, @NotNull BlockPos pNeighborPos, boolean pMovedByPiston) {
        super.neighborChanged(pState, pLevel, pPos, pNeighborBlock, pNeighborPos, pMovedByPiston);
        if (!pLevel.isClientSide && this.isSmart) {
            if (!pLevel.getBlockTicks().willTickThisTick(pPos, this)) {
                pLevel.scheduleTick(pPos, this, 0);
            }
        }
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        boolean previouslyPowered = pState.getValue(POWERED);
        if (previouslyPowered != pLevel.hasNeighborSignal(pPos)) {
            pLevel.setBlock(pPos, pState.cycle(POWERED), Block.UPDATE_CLIENTS);
        }
        super.tick(pState, pLevel, pPos, pRandom);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }

}
