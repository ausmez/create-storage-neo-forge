package net.fxnt.fxntstorage.controller;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

import java.util.List;
import java.util.Objects;

public class StorageInterfaceEntity extends SmartBlockEntity {
    private int tickCount = 0;
    public StorageControllerEntity controller = null;

    public StorageInterfaceEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void setController(StorageControllerEntity controller) {
        // Check if already has controller to prevent switching networks constantly
        if (!checkController()) {
            if (level != null) level.invalidateCapabilities(this.getBlockPos());
            this.controller = controller;
        }
    }

    private boolean checkController() {
        // Check controller still exists
        if (controller != null) {
            BlockEntity controllerCheck = Objects.requireNonNull(this.getLevel()).getBlockEntity(controller.getBlockPos());
            return controllerCheck == controller;
        }
        return false;
    }

    public void forgetController() {
        if (controller == null) return;
        controller = null;
        if (level != null) level.invalidateCapabilities(this.getBlockPos());
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (tickCount++ < ConfigManager.ServerConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) return;
        tickCount = 0;

        if (controller != null && !checkController()) {
            forgetController();
        }
    }

    // Create funnels/chutes hold on to the handler and only refresh every 64 ticks
    // Hand out one stable handler that resolves the current controller on every call
    public IItemHandlerModifiable getItemHandler() {
        return liveHandler;
    }

    protected IItemHandlerModifiable currentHandler() {
        return controller != null ? controller.getItemHandler() : EMPTY;
    }

    private static final IItemHandlerModifiable EMPTY = new EmptyItemHandler();

    private final IItemHandlerModifiable liveHandler = new IItemHandlerModifiable() {
        @Override
        public int getSlots() {
            return currentHandler().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandlerModifiable handler = currentHandler();
            return slot >= 0 && slot < handler.getSlots() ? handler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandlerModifiable handler = currentHandler();
            return slot >= 0 && slot < handler.getSlots() ? handler.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandlerModifiable handler = currentHandler();
            return slot >= 0 && slot < handler.getSlots() ? handler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandlerModifiable handler = currentHandler();
            return slot >= 0 && slot < handler.getSlots() ? handler.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = currentHandler();
            return slot >= 0 && slot < handler.getSlots() && handler.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = currentHandler();
            if (slot >= 0 && slot < handler.getSlots()) handler.setStackInSlot(slot, stack);
        }
    };
}
