package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
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
import net.neoforged.neoforge.network.PacketDistributor;
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
                switch (player.containerMenu) {
                    case CraftingMenu openMenu -> inputSlots = openMenu.slots.subList(1, 10);
                    case StonecutterMenu openMenu -> inputSlots = openMenu.slots.subList(0, 1);
                    case InventoryMenu openMenu -> inputSlots = openMenu.slots.subList(1, 5);
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

                boolean twoByTwo = false;
                boolean symmetrical = false;
                if (recipe instanceof ShapedRecipe sr) {
                    twoByTwo = sr.getHeight() <= 2 && sr.getWidth() <= 2
                            && player.containerMenu instanceof InventoryMenu;
                    symmetrical = net.minecraft.Util.isSymmetrical(sr.getWidth(), sr.getHeight(), sr.getIngredients());
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
                Slot output = handler.getSlot(0); // Should always be the Result slot
                if (action() == 1) {
                    handler.clicked(output.getContainerSlot(), 0, ClickType.PICKUP, player);
                } else if (action() == 2) {
                    handler.clicked(output.getContainerSlot(), 0, ClickType.QUICK_MOVE, player);
                }

                player.containerMenu.broadcastChanges();
                if (container != null) container.setDataChanged();
                if (itemHandler != null) {
                    PacketDistributor.sendToPlayer(player, new SyncItemStackPacket(backpack.getComponents()));
                }
            }
        });
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
