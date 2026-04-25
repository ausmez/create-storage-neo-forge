package net.fxnt.fxntstorage.passer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
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
    private final boolean isSmart;

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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        if (this.isSmart) {
            return new PasserSmartEntity(ModBlockEntities.SMART_PASSER_ENTITY.get(), pPos, pState);
        } else {
            return new PasserEntity(ModBlockEntities.PASSER_ENTITY.get(), pPos, pState);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> blockEntityType) {
        if (this.isSmart)
            return createTickerHelper(blockEntityType, ModBlockEntities.SMART_PASSER_ENTITY.get(), (type, world, pos, entity) -> entity.tick());
        return createTickerHelper(blockEntityType, ModBlockEntities.PASSER_ENTITY.get(), (type, world, pos, entity) -> entity.tick());
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        super.neighborChanged(pState, pLevel, pPos, pNeighborBlock, pNeighborPos, pMovedByPiston);
        if (!pLevel.isClientSide && this.isSmart) {
            if (!pLevel.getBlockTicks().willTickThisTick(pPos, this)) {
                pLevel.scheduleTick(pPos, this, 0);
            }
        }
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        boolean previouslyPowered = pState.getValue(POWERED);
        if (previouslyPowered != pLevel.hasNeighborSignal(pPos)) {
            pLevel.setBlock(pPos, pState.cycle(POWERED), Block.UPDATE_CLIENTS);
        }
        super.tick(pState, pLevel, pPos, pRandom);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston)
            IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        Direction direction = pState.getValue(FACING);
        return PasserShapeCache.getShape(direction);
    }
}
