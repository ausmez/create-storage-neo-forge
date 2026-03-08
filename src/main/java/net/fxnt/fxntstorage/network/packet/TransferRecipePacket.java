package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record TransferRecipePacket(ResourceLocation recipeId, List<Integer> recipeList, boolean maxTransfer,
                                   byte action) {

    public static void encode(TransferRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
        buffer.writeCollection(packet.recipeList, FriendlyByteBuf::writeInt);
        buffer.writeBoolean(packet.maxTransfer);
        buffer.writeByte(packet.action);
    }

    public static TransferRecipePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        List<Integer> recipeList = buffer.readList(FriendlyByteBuf::readInt);
        boolean maxTransfer = buffer.readBoolean();
        byte action = buffer.readByte();
        return new TransferRecipePacket(recipeId, recipeList, maxTransfer, action);
    }

    public static void handle(TransferRecipePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                Level level = player.level();
                ResourceLocation recipeId = packet.recipeId();

                Optional<? extends Recipe<?>> optional = level.getRecipeManager().byKey(recipeId);
                if (optional.isEmpty()) {
                    FXNTStorage.LOGGER.warn("Recipe not found: {}", recipeId);
                    return;
                }

                Recipe<?> recipe = optional.get();
                Inventory playerInv = player.getInventory();

                IItemHandlerModifiable itemHandler = null;
                IBackpackContainer container = null;
                ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);

                if (!backpack.isEmpty()) {
                    container = new BackpackContainer(player, backpack);
                    itemHandler = container.getItemHandler();
                }

                List<Slot> inputSlots;
                if (player.containerMenu instanceof CraftingMenu openMenu) {
                    inputSlots = openMenu.slots.subList(1, 10);
                } else if (player.containerMenu instanceof StonecutterMenu openMenu) {
                    inputSlots = openMenu.slots.subList(0, 1);
                } else if (player.containerMenu instanceof InventoryMenu openMenu) {
                    inputSlots = openMenu.slots.subList(1, 5);
                } else {
                    return;
                }

                // Clear crafting grid first
                for (Slot slot : inputSlots) {
                    if (!slot.mayPickup(player)) continue;
                    if (slot.hasItem()) {
                        ItemStack stack = slot.remove(slot.getItem().getCount());
                        if (!playerInv.add(stack)) {
                            player.drop(stack, false);
                        }
                    }
                }

                List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
                List<Integer> recipeMap = packet.recipeList();

                int maxCraftable = 1;
                if (packet.maxTransfer()) {
                    maxCraftable = getMaxCraftableItems(ingredients, playerInv, itemHandler);
                    if (!FMLEnvironment.production)
                        player.displayClientMessage(Component.literal("§a" + maxCraftable + "§r maximum craftable"), false);
                }

                boolean twoByTwo = false;
                boolean symmetrical = false;
                if (recipe instanceof ShapedRecipe sr) {
                    twoByTwo = sr.getHeight() <= 2 && sr.getWidth() <= 2
                            && player.containerMenu instanceof InventoryMenu;
                    symmetrical = Util.isSymmetrical(sr.getWidth(), sr.getHeight(), sr.getIngredients());
                }

                // Calculate centering offset for symmetrical recipes (only for 3x3 crafting)
                int offsetX = 0;
                int offsetY = 0;
                boolean isShaped = recipe instanceof ShapedRecipe;
                boolean doCenter = false;

                if (!twoByTwo && isShaped && symmetrical) {
                    ShapedRecipe sr = (ShapedRecipe) recipe;
                    offsetX = (3 - sr.getWidth()) / 2;
                    offsetY = (3 - sr.getHeight()) / 2;
                    doCenter = true;
                }

                // Loop through each ingredient and place it in the corresponding slot
                for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
                    if (ingredientIndex >= recipeMap.size()) break;

                    Ingredient ingredient = ingredients.get(ingredientIndex);
                    if (ingredient.isEmpty()) continue;

                    // Base position is always from recipeMap (1-based -> 0-based)
                    int gridSlotPosition = recipeMap.get(ingredientIndex) - 1;

                    int slotPosition;

                    if (twoByTwo) {
                        // Map 3x3 indices to 2x2 indices
                        switch (gridSlotPosition) {
                            case 0 -> slotPosition = 0;
                            case 1 -> slotPosition = 1;
                            case 3 -> slotPosition = 2;
                            case 4 -> slotPosition = 3;
                            default -> {
                                continue;
                            }
                        }
                    } else if (doCenter) {
                        // Centering for symmetrical shaped recipes on a 3x3 grid.
                        // Convert from 3x3 coords, apply offset, convert back.
                        int x = gridSlotPosition % 3;
                        int y = gridSlotPosition / 3;
                        x += offsetX;
                        y += offsetY;
                        slotPosition = y * 3 + x;
                    } else {
                        // Normal (non-symmetrical) 3x3 shaped or shapeless: use the map as-is
                        slotPosition = gridSlotPosition;
                    }

                    if (slotPosition < 0 || slotPosition >= inputSlots.size()) continue;

                    ItemStack collected = ItemStack.EMPTY;
                    int remaining = maxCraftable;

                    // Try player inventory
                    for (int j = 0; j < playerInv.getContainerSize() && remaining > 0; j++) {
                        ItemStack stack = playerInv.getItem(j);
                        if (!stack.isEmpty() && ingredient.test(stack)) {
                            int extractAmount = Math.min(stack.getCount(), remaining);
                            ItemStack extracted = stack.split(extractAmount);

                            if (collected.isEmpty()) collected = extracted.copy();
                            else collected.grow(extracted.getCount());

                            playerInv.setItem(j, stack);
                            remaining -= extracted.getCount();
                        }
                    }

                    // Try backpack
                    if (remaining > 0 && itemHandler != null) {
                        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
                        for (int j = 0; j < layout.items().getEndIndex() && remaining > 0; j++) {
                            ItemStack stack = itemHandler.getStackInSlot(j);
                            if (!stack.isEmpty() && ingredient.test(stack)) {
                                int extractAmount = Math.min(stack.getCount(), remaining);
                                ItemStack extracted = stack.split(extractAmount);

                                if (collected.isEmpty()) collected = extracted.copy();
                                else collected.grow(extracted.getCount());

                                itemHandler.setStackInSlot(j, stack);
                                remaining -= extracted.getCount();
                            }
                        }
                    }

                    if (collected.isEmpty() || collected.getCount() < maxCraftable) {
                        return; // missing item(s)
                    }

                    inputSlots.get(slotPosition).setByPlayer(collected);
                }

                AbstractContainerMenu handler = player.containerMenu;
                Slot output = handler.getSlot(0); // Should always be the Result slot of the crafting table
                byte action = packet.action();
                if (action == 1) {
                    handler.clicked(output.getContainerSlot(), 0, ClickType.PICKUP, player);
                } else if (action == 2) {
                    handler.clicked(output.getContainerSlot(), 0, ClickType.QUICK_MOVE, player);
                }

                player.containerMenu.broadcastChanges();
                if (container != null) container.setDataChanged();
            }

        });
        context.get().setPacketHandled(true);
    }

    private static int getMaxCraftableItems(List<Ingredient> ingredients, Inventory inventory, @Nullable IItemHandler backpack) {
        int maxCrafts = 64;
        Map<Ingredient, Integer> ingredientCounts = new HashMap<>();
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ingredientCounts.merge(ingredient, 1, Integer::sum);
            }
        }

        for (Map.Entry<Ingredient, Integer> entry : ingredientCounts.entrySet()) {
            Ingredient ingredient = entry.getKey();
            int requiredPerCraft = entry.getValue();
            int available = 0;
            int ingredientMaxStackSize = Item.MAX_STACK_SIZE;

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    available += stack.getCount();
                    ingredientMaxStackSize = Math.min(ingredientMaxStackSize, stack.getMaxStackSize());
                }
            }

            if (backpack != null) {
                for (int i = 0; i < backpack.getSlots(); i++) {
                    ItemStack stack = backpack.getStackInSlot(i);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        available += stack.getCount();
                        ingredientMaxStackSize = Math.min(ingredientMaxStackSize, stack.getMaxStackSize());
                    }
                }
            }

            int craftsForIngredient = Math.min(available / requiredPerCraft, ingredientMaxStackSize);
            maxCrafts = Math.min(maxCrafts, craftsForIngredient);
            if (maxCrafts == 0) {
                break;
            }
        }

        return maxCrafts;
    }
}
