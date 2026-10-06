package net.fxnt.fxntstorage.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.math.VoxelShaper;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@SuppressWarnings("deprecation")
public class BackpackBlock extends BaseEntityBlock {
    public static final MapCodec<BackpackBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockBehaviour.propertiesCodec(),
                    Codec.INT.fieldOf("max_stack_size").forGetter(BackpackBlock::getStackMultiplier)
            ).apply(instance, BackpackBlock::new));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Simplified outline of the default model (body, lid, front pocket, handle)
    private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(Shapes.or(
            Block.box(3, 0, 5, 13, 11, 11),
            Block.box(3.5, 11, 6, 12.5, 13, 11),
            Block.box(4, 0.5, 3, 12, 8, 5),
            Block.box(6, 13, 8, 7, 14, 9),
            Block.box(9, 13, 8, 10, 14, 9),
            Block.box(6, 13.75, 8, 10, 14.25, 9)
    ).optimize(), Direction.NORTH);

    private static volatile Map<Block, VoxelShaper> modelShapes = Map.of();

    private static final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    public final int stackMultiplier;

    public BackpackBlock(Properties pProperties, int stackMultiplier) {
        super(pProperties.strength(0.2f, 600.0f));
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        this.stackMultiplier = stackMultiplier;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.BACKPACK_ENTITY.get();
        BackpackEntity blockEntity = new BackpackEntity(type, pPos, pState);
        blockEntity.setData(layout.getTotalSlots(), stackMultiplier);
        return blockEntity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.BACKPACK_ENTITY.get(), (world, blockPos, blockState, blockEntity) -> blockEntity.serverTick(world));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof BackpackEntity be) {
            be.saveExtraComponents(stack);
            be.readInventory(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            if (stack.has(DataComponents.CUSTOM_NAME))
                be.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
            SortOrder order = Optional.ofNullable(stack.get(ModDataComponents.INVENTORY_SORT_ORDER)).orElse(SortOrder.COUNT);
            be.setSortOrder(order);
        }
    }

    public int getStackMultiplier() {
        return this.stackMultiplier;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isCrouching() && !BackpackHelper.isWearingBackpack(player)) {
            // Equip the backpack to the back or chest slot
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof BackpackEntity backpackEntity)) {
                return InteractionResult.FAIL;
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.5F, 1.0F);

            ItemStack itemStack = new ItemStack(BackpackItem.byBlock(this));
            itemStack = saveEntityToStack(backpackEntity, itemStack);
            level.removeBlock(pos, false);

            boolean equipped = BackpackHelper.equipBackpack(player, itemStack);

            return (equipped) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BackpackEntity backPackEntity) {
            player.openMenu(backPackEntity, buf -> buf.writeEnum(BackpackMenu.BackpackType.BLOCK).writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    private ItemStack saveEntityToStack(BackpackEntity blockEntity, ItemStack itemStack) {
        itemStack = blockEntity.saveToItemStack(itemStack);
        return itemStack;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return outlineShape(state);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return outlineShape(state);
    }

    private VoxelShape outlineShape(BlockState state) {
        return modelShapes.getOrDefault(this, SHAPE).get(state.getValue(FACING));
    }

    public static void setModelShapes(Map<Block, VoxelShaper> shapes) {
        modelShapes = Map.copyOf(shapes);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BackpackEntity backpackEntity) {
            return backpackEntity.calcRedstoneFromInventory();
        }
        return 0;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null; // Return null for Spectator players trying to open menu
    }
}
