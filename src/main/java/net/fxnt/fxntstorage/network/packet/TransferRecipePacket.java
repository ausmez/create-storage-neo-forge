package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TransferRecipePacket(ResourceLocation recipeId, List<Integer> recipeList,
                                   boolean maxTransfer, int action) implements CustomPacketPayload {
    public static final Type<TransferRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "transfer_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferRecipePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TransferRecipePacket::recipeId,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), TransferRecipePacket::recipeList,
            ByteBufCodecs.BOOL, TransferRecipePacket::maxTransfer,
            ByteBufCodecs.INT, TransferRecipePacket::action,
            TransferRecipePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {

                Level level = player.level();
                Optional<RecipeHolder<?>> optional = level.getRecipeManager().byKey(recipeId());
                if (optional.isEmpty()) {
                    FXNTStorage.LOGGER.warn("Recipe not found: {}", recipeId());
                    return;
                }

                Recipe<?> recipe = optional.get().value();
                Inventory playerInv = player.getInventory();

                IItemHandlerModifiable itemHandler = null;
                IBackpackContainer container = null;
                ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);

                if (!backpack.isEmpty()) {
                    container = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
                    itemHandler = container.getItemHandler();
                }

                List<Slot> inputSlots;
                Slot resultSlot;
                switch (player.containerMenu) {
                    case BackpackMenu menu -> {
                        // The backpack's own crafting-upgrade grid. Only fillable while the panel is open
                        if (!menu.isCraftingPanelReady()) return;
                        Slot craftingResult = menu.getCraftingResultSlot();
                        if (craftingResult == null) return;
                        inputSlots = menu.getCraftingMatrixSlots();
                        resultSlot = craftingResult;
                        // Source ingredients from the backpack actually being viewed (worn or placed),
                        // not necessarily the equipped one
                        container = menu.container;
                        itemHandler = container.getItemHandler();
                    }
                    case CraftingMenu openMenu -> {
                        inputSlots = openMenu.slots.subList(1, 10);
                        resultSlot = openMenu.getSlot(0);
                    }
                    case StonecutterMenu openMenu -> {
                        inputSlots = openMenu.slots.subList(0, 1);
                        resultSlot = openMenu.getSlot(0);
                    }
                    case InventoryMenu openMenu -> {
                        inputSlots = openMenu.slots.subList(1, 5);
                        resultSlot = openMenu.getSlot(0);
                    }
                    default -> {
                        return;
                    }
                }

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
                List<Integer> recipeMap = recipeList();

                int maxCraftable = 1;
                if (maxTransfer()) {
                    maxCraftable = getMaxCraftableItems(ingredients, playerInv, itemHandler);
                    if (!FMLEnvironment.production)
                        player.displayClientMessage(Component.literal("§a" + maxCraftable + "§r maximum craftable"), false);
                }

                if (recipe instanceof ShapedRecipe shaped) {
                    int gridW = (player.containerMenu instanceof InventoryMenu) ? 2 : 3; // square grid
                    int width = shaped.getWidth();
                    int height = shaped.getHeight();

                    if (width > gridW || height > gridW) return;

                    int offsetX = (gridW - width) / 2;
                    int offsetY = (gridW - height) / 2;

                    NonNullList<Ingredient> grid = shaped.getIngredients();
                    for (int k = 0; k < grid.size(); k++) {
                        Ingredient ingredient = grid.get(k);
                        if (ingredient.isEmpty()) continue;

                        int x = k % width + offsetX;
                        int y = k / width + offsetY;
                        int slotPosition = y * gridW + x;
                        if (slotPosition < 0 || slotPosition >= inputSlots.size()) continue;

                        if (!collectAndPlace(ingredient, slotPosition, maxCraftable, playerInv, itemHandler, inputSlots)) {
                            return; // missing item(s)
                        }
                    }
                } else {
                    // Shapeless / non-shaped (incl. stonecutter): use the client-sent positions as-is.
                    for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
                        if (ingredientIndex >= recipeMap.size()) break;

                        Ingredient ingredient = ingredients.get(ingredientIndex);
                        if (ingredient.isEmpty()) continue;

                        int slotPosition = recipeMap.get(ingredientIndex) - 1; // 1-based -> 0-based
                        if (slotPosition < 0 || slotPosition >= inputSlots.size()) continue;

                        if (!collectAndPlace(ingredient, slotPosition, maxCraftable, playerInv, itemHandler, inputSlots)) {
                            return; // missing item(s)
                        }
                    }
                }

                AbstractContainerMenu handler = player.containerMenu;
                if (action() == 1) {
                    handler.clicked(resultSlot.index, 0, ClickType.PICKUP, player);
                } else if (action() == 2) {
                    handler.clicked(resultSlot.index, 0, ClickType.QUICK_MOVE, player);
                }

                player.containerMenu.broadcastChanges();
                if (container != null) container.setDataChanged();
            }
        });
    }

    private boolean collectAndPlace(Ingredient ingredient, int slotPosition, int maxCraftable,
                                    Inventory playerInv, @Nullable IItemHandlerModifiable itemHandler,
                                    List<Slot> inputSlots) {
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
            return false; // missing item(s)
        }

        inputSlots.get(slotPosition).setByPlayer(collected);
        return true;
    }

    private int getMaxCraftableItems(List<Ingredient> ingredients, Inventory inventory, @Nullable IItemHandler backpack) {
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
            int ingredientMaxStackSize = Item.DEFAULT_MAX_STACK_SIZE;

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
