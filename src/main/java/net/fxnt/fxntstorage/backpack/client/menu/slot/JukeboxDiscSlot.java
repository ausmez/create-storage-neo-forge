package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

public class JukeboxDiscSlot extends SlotItemHandler {
    private final IBackpackContainer backpack;
    private final Player player;
    private final BackpackMenu.BackpackType backpackType;
    private final BlockPos blockPos;
    private final int contraptionId;

    private final BooleanSupplier hasUpgrade;
    private final BooleanSupplier isPanelExpanded;
    private final Runnable updateContainerData;
    private final Runnable stopClientPlayback;

    public JukeboxDiscSlot(IBackpackContainer backpack, int index, int xPosition, int yPosition,
                           Player player, BackpackMenu.BackpackType backpackType, @Nullable BlockPos blockPos,
                           int contraptionId, BooleanSupplier hasUpgrade, BooleanSupplier isPanelExpanded,
                           Runnable updateContainerData, Runnable stopClientPlayback) {
        super(backpack.getItemHandler(), index, xPosition, yPosition);
        this.backpack = backpack;
        this.player = player;
        this.backpackType = backpackType;
        this.blockPos = blockPos;
        this.contraptionId = contraptionId;
        this.hasUpgrade = hasUpgrade;
        this.isPanelExpanded = isPanelExpanded;
        this.updateContainerData = updateContainerData;
        this.stopClientPlayback = stopClientPlayback;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        if (!player.level().isClientSide()) {
            ItemStack current = getItem();
            if (!current.isEmpty() && !stack.isEmpty()) {
                handleStopPlayback();
            }
        }
        super.set(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        backpack.setDataChanged();

        if (getItem().isEmpty()) {
            handleStopPlayback();
        }
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        handleStopPlayback();
    }

    @Override
    public boolean isActive() {
        return hasUpgrade.getAsBoolean() && isPanelExpanded.getAsBoolean();
    }

    private void handleStopPlayback() {
        if (!player.level().isClientSide()) {
            if (backpackType == BackpackMenu.BackpackType.WORN) {
                JukeboxHandler.stopPlayer((ServerPlayer) player);
            } else if (backpackType == BackpackMenu.BackpackType.BLOCK && blockPos != null) {
                JukeboxHandler.stopBlock(player.level(), blockPos);
            } else if (backpackType == BackpackMenu.BackpackType.CONTRAPTION
                    && contraptionId >= 0 && player instanceof ServerPlayer sp) {
                JukeboxHandler.stopEntity(sp, contraptionId);
            }
            updateContainerData.run();
        } else {
            stopClientPlayback.run();
        }
    }
}
