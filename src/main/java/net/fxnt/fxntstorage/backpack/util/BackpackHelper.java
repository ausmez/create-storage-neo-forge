package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BackpackHelper {
    private static final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

    public static boolean isBackpackCuriosSlotVisible(Player player) {
        if (player == null) return false;

        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof BackpackItem))
                .map(slotResult -> slotResult.slotContext().visible())
                .orElse(false);
    }

    public static boolean isWearingBackpack(Player player, boolean checkVisibility) {
        ItemStack itemStack = getEquippedBackpackStack(player);
        if (FXNTStorage.CURIOS_LOADED && checkVisibility
                && !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem)) {
            return BackpackHelper.isBackpackCuriosSlotVisible(player);
        }
        return !itemStack.isEmpty();
    }

    public static boolean isWearingBackpack(Player player) {
        return isWearingBackpack(player, false);
    }

    @OnlyIn(Dist.CLIENT)
    public static ItemStack getEquippedBackpackStack(LocalPlayer player) {
        if (player == null) return ItemStack.EMPTY;

        // Check chest slot first
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof BackpackItem) {
            return chestStack;
        }

        // Check Curios
        if (FXNTStorage.CURIOS_LOADED) {
            Optional<ItemStack> backSlot = CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.getStacksHandler("back"))
                    .map(handler -> handler.getStacks().getStackInSlot(0));
            return backSlot
                    .filter(stack -> stack.getItem() instanceof BackpackItem)
                    .orElse(ItemStack.EMPTY);
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getEquippedBackpackStack(LivingEntity player) {
        if (player == null) return ItemStack.EMPTY;

        // If Curios is not loaded or not present, check the chest slot directly
        if (!FXNTStorage.CURIOS_LOADED) return checkChestSlot(player);

        // Get the Curios item handler
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);

        // If Curios capability is present, check the "back" slot for a BackpackItem
        if (curios.isPresent()) {
            Optional<ICurioStacksHandler> stacksHandler = curios.get().getStacksHandler("back");

            if (stacksHandler.isPresent()) {
                IDynamicStackHandler stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof BackpackItem) {
                        return stack;
                    }
                }
            }
        }

        // Fallback to chest slot if no backpack found
        return checkChestSlot(player);
    }

    private static ItemStack checkChestSlot(@NotNull LivingEntity player) {
        ItemStack chestSlotItem = player.getItemBySlot(EquipmentSlot.CHEST);
        return (chestSlotItem.getItem() instanceof BackpackItem) ? chestSlotItem : ItemStack.EMPTY;
    }

    public static boolean equipBackpack(Player player, ItemStack backpack) {
        if (player == null || backpack.isEmpty()) {
            return false; // Nothing to equip or player is null
        }

        // If Curios is not installed or the "back" slot is unavailable, try chest slot
        if (!FXNTStorage.CURIOS_LOADED) return equipInChestSlot(player, backpack);

        // Get the Curios item handler
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);

        if (curios.isPresent()) {
            ICuriosItemHandler curiosInv = curios.get();
            Optional<ICurioStacksHandler> stacksHandler = curiosInv.getStacksHandler("back");

            if (stacksHandler.isPresent()) {
                IDynamicStackHandler stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).isEmpty()) {
                        curiosInv.setEquippedCurio("back", i, backpack);
                        return true;
                    }
                }
            }
        }

        return equipInChestSlot(player, backpack);
    }

    // Helper method to equip the backpack in the chest slot
    private static boolean equipInChestSlot(@NotNull Player player, ItemStack backpack) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chestStack.isEmpty()) {
            // Equip the backpack in the chest slot if it's empty
            player.setItemSlot(EquipmentSlot.CHEST, backpack);
            return true;
        }

        // Chest slot is full, try to place the backpack in the player's inventory
        return equipInPlayerInventory(player, backpack);
    }

    // Helper method to equip the backpack in the player's inventory if no other slots are available
    private static boolean equipInPlayerInventory(@NotNull Player player, ItemStack backpack) {
        // Try to find an empty slot in the player's main inventory (slots 0 to 35)
        int freeInventorySlot = player.getInventory().getFreeSlot();

        if (freeInventorySlot > -1) {
            player.getInventory().setItem(freeInventorySlot, backpack);
            return true;
        }

        // No space available in the player's inventory
        return false;
    }

    public static boolean itemEntityToBackpack(@NotNull IBackpackContainer container, @NotNull ItemEntity itemEntity, @Nullable Player player) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return false;

        final IItemHandlerModifiable handler = container.getItemHandler();

        boolean changed = false;
        int maxStackSize = container.getStackMultiplier() * stack.getMaxStackSize();
        List<Integer> changedSlots = new ArrayList<>();

        // If matching slot stack exists
        if (!stack.isDamageableItem() && !stack.isBarVisible()) {
            for (int i : layout.items().range()) {
                ItemStack slotStack = handler.getStackInSlot(i);
                if (slotStack.isEmpty()) continue;

                if (!ItemStack.isSameItemSameComponents(stack, slotStack)) continue;

                int space = maxStackSize - slotStack.getCount();
                if (space <= 0) continue;

                int toMove = Math.min(space, stack.getCount());
                slotStack.grow(toMove);
                stack.shrink(toMove);

                changedSlots.add(i);

                changed = true;
            }
        }

        // If matching slot doesn't exist
        if (!stack.isEmpty()) {
            for (int i : layout.items().range()) {
                ItemStack slotStack = handler.getStackInSlot(i);
                if (!slotStack.isEmpty()) continue;

                int toMove = Math.min(stack.getCount(), maxStackSize);
                ItemStack inserted = stack.split(toMove);
                handler.setStackInSlot(i, inserted);
                changedSlots.add(i);

                changed = true;
                break;
            }
        }

        if (changed) {
            container.setDataChanged();

            if (player instanceof ServerPlayer sp && sp.containerMenu instanceof BackpackMenu menu
                    && menu.container == container) {
                for (int slotIndex : changedSlots) {
                    ItemStack slotStack = handler.getStackInSlot(slotIndex);
                    sp.connection.send(new ClientboundContainerSetSlotPacket(
                            menu.containerId,
                            menu.getStateId(),
                            slotIndex,
                            slotStack
                    ));
                }
            }
        }

        return changed;
    }

    public static void openBackpackFromInventory(@NotNull ServerPlayer player, BackpackMenu.BackpackType backpackType) {
        if (player.level().isClientSide) return;

        ItemStack itemStack = ItemStack.EMPTY;
        IBackpackContainer container = null;

        switch (backpackType) {
            case WORN -> {
                itemStack = BackpackHelper.getEquippedBackpackStack(player);
                container = BackpackContainer.Cache.getOrCreateWornBackpack(player, itemStack);
            }
            case ITEM -> {
                itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                container = new BackpackContainer(player, itemStack);
            }
        }

        // No backpack equipped in either back, chest, or hand
        if (itemStack.isEmpty() || container == null) return;

        ItemStack backpack = itemStack;
        IBackpackContainer finalContainer = container;
        player.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return backpack.getHoverName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
                return new BackpackMenu(
                        ModMenuTypes.BACKPACK_MENU.get(),
                        containerId,
                        player.getInventory(),
                        finalContainer,
                        backpackType,
                        null
                );
            }
        }, buf -> buf.writeEnum(backpackType));
    }
}
