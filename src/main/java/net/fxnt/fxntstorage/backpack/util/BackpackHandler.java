package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackItemMenu;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return backpack.getHoverName();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new BackpackItemMenu(containerId, player.getInventory(), new BackpackContainer(backpack, player), backpackType);
            }
        }, buf -> {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, backpack)
                    .resultOrPartial(err -> FXNTStorage.LOGGER.error("Failed to encode ItemStack: {}", err))
                    .ifPresent(buf::writeNbt);
            buf.writeByte(backpackType);
        });
    }

}