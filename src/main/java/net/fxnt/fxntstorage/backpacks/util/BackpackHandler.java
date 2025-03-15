package net.fxnt.fxntstorage.backpacks.util;

import net.fxnt.fxntstorage.backpacks.main.BackpackBlockMenu;
import net.fxnt.fxntstorage.backpacks.main.BackpackContainer;
import net.fxnt.fxntstorage.backpacks.main.BackpackEntity;
import net.fxnt.fxntstorage.backpacks.main.BackpackItemMenu;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class BackpackHandler {

    public static void openBackpackFromInventory(@NotNull ServerPlayer player, byte backpackType) {
        if (player.level().isClientSide) return;

        ItemStack itemStack = ItemStack.EMPTY;
        if (backpackType == Util.BACKPACK_ON_BACK) {
            itemStack = BackpackHelper.getEquippedBackpackStack(player);
        } else if (backpackType == Util.BACKPACK_IN_HAND) {
            itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        }

        // No backpack equipped in either back, chest, or hand
        if (itemStack.isEmpty()) return;

        ItemStack backpack = itemStack;
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return backpack.getHoverName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
                return new BackpackItemMenu(i, player.getInventory(), new BackpackContainer(backpack, player), backpackType);
            }
        }, buf -> buf.writeItem(backpack).writeByte(backpackType));

    }

    public static void openBackpackFromBlock(@NotNull ServerPlayer player, BackpackEntity blockEntity) {
        if (player.level().isClientSide) return;

        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return blockEntity.getDisplayName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
                return new BackpackBlockMenu(i, inventory, blockEntity);
            }
        }, buf -> buf.writeBlockPos(blockEntity.getBlockPos()));

    }

}