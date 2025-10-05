package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

import java.util.Objects;

public class StorageInterfaceEntity extends BlockEntity {
    private int tickCount = 0;
    public StorageControllerEntity controller = null;

    public StorageInterfaceEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
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
        controller = null;
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        if (tickCount++ < ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) return;
        tickCount = 0;

        if (controller != null && !checkController()) {
            forgetController();
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return controller != null ? controller.getItemHandler() : new EmptyItemHandler();
    }

}
