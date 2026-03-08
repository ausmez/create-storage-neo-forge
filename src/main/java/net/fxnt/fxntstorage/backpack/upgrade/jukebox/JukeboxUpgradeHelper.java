package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class JukeboxUpgradeHelper {

    private static Optional<ItemStack> getDiscFromHandler(IItemHandler handler) {
        if (handler == null) {
            return Optional.empty();
        }

        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        ItemStack disc = handler.getStackInSlot(layout.jukeboxDiscs().getStartIndex());
        return disc.isEmpty() ? Optional.empty() : Optional.of(disc);
    }

    public static Optional<ItemStack> getEquippedMusicDisc(Player player) {
        ItemStack backpackStack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpackStack.isEmpty()) {
            return Optional.empty();
        }

        BackpackContainer container = new BackpackContainer(player, backpackStack);
        return getDiscFromHandler(container.getItemHandler());
    }

    public static Optional<ItemStack> getBlockMusicDisc(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BackpackEntity backpack) {
            return getDiscFromHandler(backpack.getItemHandler());
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> getMusicDisc(@Nullable Player player, @Nullable Level level, @Nullable BlockPos pos) {
        if (pos == null) {
            if (player == null) {
                return Optional.empty();
            }
            return getEquippedMusicDisc(player);
        }

        if (level == null) {
            return Optional.empty();
        }

        return getBlockMusicDisc(level, pos);
    }
}
