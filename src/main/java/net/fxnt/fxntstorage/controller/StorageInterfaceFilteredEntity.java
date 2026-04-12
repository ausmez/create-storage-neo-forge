package net.fxnt.fxntstorage.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.Icons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;

import java.util.ArrayList;
import java.util.List;

public class StorageInterfaceFilteredEntity extends StorageInterfaceEntity {
    protected ScrollOptionBehaviour<FilterScope> includeEmptyStorage;
    private FilteringBehaviour filter;
    private float percentageUsed = 0;

    public StorageInterfaceFilteredEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(filter = new FilteringBehaviour(this, new FilteringBox()));
        includeEmptyStorage = new ScrollOptionBehaviour<>(FilterScope.class,
                Component.translatable(FXNTStorage.MOD_ID + ".storage_interface_filtered.filter_scope"), this, new IncludeEmptyStorageValueBox());
        includeEmptyStorage.value = 0;
        behaviours.add(includeEmptyStorage);
    }

    @Override
    public IItemHandlerModifiable getItemHandler() {
        if (controller == null)
            return new EmptyHandler();

        IItemHandlerModifiable handler = controller.getItemHandler();

        if (filter == null || filter.getFilter().isEmpty())
            return handler;

        ScrollValueBehaviour behaviour = getBehaviour(ScrollOptionBehaviour.TYPE);
        return new FilteredItemHandler(handler, filter, behaviour);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        super.serverTick(level, pos, state);

        float oldPercentageUsed = percentageUsed;
        percentageUsed = calculatePercentageUsed();

        if (percentageUsed != oldPercentageUsed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public float calculatePercentageUsed() {
        int totalSpace = 0;
        int usedSpace = 0;

        IItemHandler handler = getItemHandler();

        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            int maxStackSize = handler.getSlotLimit(i);

            totalSpace += maxStackSize;
            usedSpace += stack.getCount();
        }
        if (totalSpace == 0) return 0;

        return ((float) usedSpace / totalSpace) * 100;
    }

    public static class FilteringBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8f, 5f, 15.5f);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(DirectedDirectionalBlock.FACING);
            if (getSide() == Direction.UP && facing.getAxis().isHorizontal()) {
                // Move box from y=5 to y=8 AFTER rotation
                TransformStack.of(ms).translate(0, 3f / 16f, 0).rotateZDegrees(180);
            }
            if (facing.getAxis() == Direction.Axis.Y)
                return;
            if (getSide() != Direction.UP)
                return;
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }

    }

    public static class FilteredItemHandler implements IItemHandlerModifiable {
        private final IItemHandlerModifiable source;
        private final FilteringBehaviour filter;
        private final List<Integer> filteredSlots = new ArrayList<>();

        public FilteredItemHandler(IItemHandlerModifiable source, FilteringBehaviour filter, ScrollValueBehaviour behaviour) {
            this.source = source;
            this.filter = filter;

            boolean includeEmpty = behaviour.getValue() == 0;

            // Precompute the slots that pass the filter
            for (int i = 0; i < source.getSlots(); i++) {
                ItemStack stack = source.getStackInSlot(i);
                if (filter.test(stack) || (stack.isEmpty() && includeEmpty)) {
                    filteredSlots.add(i);
                }
            }
        }

        @Override
        public int getSlots() {
            return filteredSlots.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= source.getSlots())
                return ItemStack.EMPTY;
            return source.getStackInSlot(filteredSlots.get(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!filter.test(stack)) return stack;
            return source.insertItem(filteredSlots.get(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return source.extractItem(filteredSlots.get(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return source.getSlotLimit(filteredSlots.get(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return filter.test(stack) && source.isItemValid(filteredSlots.get(slot), stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            source.setStackInSlot(filteredSlots.get(slot), stack);
        }
    }

    private static class IncludeEmptyStorageValueBox extends CenteredSideValueBoxTransform {
        public IncludeEmptyStorageValueBox() {
            super((s, d) -> d == Direction.UP);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(StorageInterfaceFiltered.FACING);
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing));
        }

        @Override
        public int getOverrideColor() {
            return 0x592424;
        }
    }

    public enum FilterScope implements INamedIconOptions {
        INCLUDE_EMPTY(Icons.I_INCLUDE_EMPTY),
        EXCLUDE_EMPTY(Icons.I_EXCLUDE_EMPTY);

        private final String translationKey;
        private final AllIcons icon;

        FilterScope(AllIcons icon) {
            this.icon = icon;
            this.translationKey = FXNTStorage.MOD_ID + ".storage_interface_filtered." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

}
