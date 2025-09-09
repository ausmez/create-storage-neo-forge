package net.fxnt.fxntstorage.network.handler;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorage;
import net.fxnt.fxntstorage.network.packet.*;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.fxnt.fxntstorage.util.EventHandler;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ServerPayloadHandler {
    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

    public static ServerPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleCrossbowChargedPacket(final CrossbowChargedPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> EventHandler.consumeArrowFromBackpack(context.player()));
    }

    public void handleKeyPressedPacket(final KeyPressedPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            switch (packet.hotKey()) {
                case Util.JETPACK_KEY_PRESS -> JetpackManager.getJetpackHandler(player).flyingOnKeyPress();
                case Util.JETPACK_KEY_RELEASE -> JetpackManager.getJetpackHandler(player).flyingOnKeyRelease();
                case Util.OPEN_BACKPACK -> BackpackHandler.openBackpackFromInventory(player, Util.BACKPACK_ON_BACK);
                case Util.CLOSE_BACKPACK -> {
                    if (player.containerMenu instanceof BackpackMenu) player.closeContainer();
                }
                case Util.BACKPACK_MENU_CTRL -> {
                    if (player.containerMenu instanceof BackpackMenu backpackMenu) {
                        backpackMenu.ctrlKeyDown = packet.pressed();
                    }
                }
                case Util.TOGGLE_HOVER -> {
                    JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                    if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                        jetpackHandler.toggleHover();
                    }
                }
                case Util.MINE_ALL_BLOCKS -> player.getPersistentData()
                        .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)
                        .putBoolean("MineAllBlocks", packet.pressed());
            }
        });
    }

    public void handlePickBlockUpgradePacket(final PickBlockUpgradePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            new BackpackOnBackUpgradeHandler(player).applyPickBlockUpgrade(packet.stack());
        });
    }

    public void handlePlayerInputPacket(final PlayerInputPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            JetpackManager.getJetpackHandler(player).processPlayerInputPacket(packet.forwardImpulse(), packet.leftImpulse());
        });
    }

    public void handleSortInventoryPacket(final SortInventoryPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            // Backpack sorting
            if (packet.invType() == Util.INV_TYPE_BACKPACK && player.containerMenu instanceof BackpackMenu menu)
                menu.sortBackpackItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());

            if (packet.invType() == Util.INV_TYPE_STORAGE_BOX) {
                // StorageBox sorting
                if (player.containerMenu instanceof StorageBoxMenu menu)
                    menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());

                // StorageBoxMounted sorting
                if (player.containerMenu instanceof StorageBoxMountedMenu menu)
                    menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
            }
        });
    }

    public void handleSyncClientSettingsPacket(final SyncClientSettingsPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            CompoundTag settings = packet.settings();

            ListTag listTag = settings.getList("prefersSilkTouchList", Tag.TAG_STRING);
            ListTag prefersSilkTouchList = new ListTag();
            prefersSilkTouchList.addAll(listTag);

            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("AllowChorusFruit", settings.getBoolean("allowChorusFruit"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("DisplayFeederMessage", settings.getBoolean("displayFeederMessage"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("IgnoreFanProcessing", settings.getBoolean("ignoreFanProcessing"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("PreferSilkTouch", settings.getBoolean("preferSilkTouch"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).put("PrefersSilkTouchList", prefersSilkTouchList);
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putInt("TorchDeployerCooldown", settings.getInt("torchDeployerCooldown"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putInt("TorchDeployerLightLevel", settings.getInt("torchDeployerLightLevel"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putString("TorchDeployerLightSource", settings.getString("torchDeployerLightSource"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackHoverBobbing", settings.getBoolean("jetpackHoverBobbing"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("CheckBackpackForProjectiles", settings.getBoolean("checkBackpackForProjectiles"));
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("CheckBackpackForToolboxItems", settings.getBoolean("checkBackpackForToolboxItems"));
        });
    }

    public void handleSetSortOrderPacket(SetSortOrderPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if (player.containerMenu instanceof BackpackMenu menu) {
                menu.container.setSortOrder(packet.sortOrder());
                menu.container.setDataChanged();
            }

            if (player.containerMenu instanceof StorageBoxMenu menu) {
                menu.setSortOrder(packet.sortOrder());
            }

            if (player.containerMenu instanceof StorageBoxMountedMenu menu) {
                menu.setSortOrder(packet.sortOrder());
            }
        });
    }

    public void handleSetMountedStorageDirtyPacket(SetMountedStorageDirtyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            Entity entity = player.level().getEntity(packet.entityId());
            if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(packet.localPos());
                if (storage instanceof SimpleStorageBoxMountedStorage) {
                    ((SimpleStorageBoxMountedStorage) storage).markDirty();
                } else if (storage instanceof StorageBoxMountedStorage) {
                    ((StorageBoxMountedStorage) storage).markDirty();
                }
            }
        });
    }

    public void handleTransferRecipePacket(TransferRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            Level level = player.level();
            Optional<RecipeHolder<?>> optional = level.getRecipeManager().byKey(packet.recipeId());
            if (optional.isEmpty()) {
                FXNTStorage.LOGGER.warn("Recipe not found: {}", packet.recipeId());
                return;
            }

            Recipe<?> recipe = optional.get().value();
            Inventory playerInv = player.getInventory();

            IItemHandlerModifiable itemHandler = null;
            IBackpackContainer container = null;
            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);

            if (!backpack.isEmpty()) {
                container = new BackpackContainer(backpack, player);
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
                    for (int j = 0; j < Util.ITEM_SLOT_END_RANGE && remaining > 0; j++) {
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
            if (packet.action() == 1) {
                handler.clicked(output.getContainerSlot(), 0, ClickType.PICKUP, player);
            } else if (packet.action() == 2) {
                handler.clicked(output.getContainerSlot(), 0, ClickType.QUICK_MOVE, player);
            }

            player.containerMenu.broadcastChanges();
            if (container != null) container.setDataChanged();
            if (itemHandler != null) {
                PacketDistributor.sendToPlayer(player, new SyncItemStackPacket(backpack.getComponents()));
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

    public void handleJetpackFlyingPacket(JetpackFlyingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(packet.flying(), packet.hovering());
        });
    }

}
