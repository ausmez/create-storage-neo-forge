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
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ServerPayloadHandler {
    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

    public static ServerPayloadHandler getInstance() {
        return INSTANCE;
    }

    public static void handleKeyPressedPacket(KeyPressedPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                switch (packet.hotkey()) {
                    case Util.JETPACK_KEY_PRESS -> JetpackHandler.flyingOnKeyPress(player);
                    case Util.JETPACK_KEY_RELEASE -> JetpackHandler.flyingOnKeyRelease(player);
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
                        if (JetpackHandler.calculateJetPackFuel(player) > 0) {
                            jetpackHandler.toggleHover();
                        }
                    }
                    case Util.MINE_ALL_BLOCKS -> player.getPersistentData()
                            .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)
                            .putBoolean("MineAllBlocks", packet.pressed());
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handlePickBlockUpgradePacket(PickBlockUpgradePacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                new BackpackOnBackUpgradeHandler(player).applyPickBlockUpgrade(packet.stack());
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handlePlayerInputPacket(PlayerInputPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                JetpackHandler.processPlayerInputPacket(player, packet.forwardImpulse(), packet.leftImpulse());
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSetMountedStorageDirtyPacket(SetMountedStorageDirtyPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                Entity entity = player.level().getEntity(packet.contraptionId());

                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(packet.localPos());
                    if (storage instanceof SimpleStorageBoxMountedStorage) {
                        ((SimpleStorageBoxMountedStorage) storage).markDirty();
                    } else if (storage instanceof StorageBoxMountedStorage) {
                        ((StorageBoxMountedStorage) storage).markDirty();
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSetSortOrderPacket(SetSortOrderPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
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
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSortInventoryPacket(SortInventoryPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                byte sortType = packet.invType();
                // Backpack sorting
                if (sortType == Util.INV_TYPE_BACKPACK && player.containerMenu instanceof BackpackMenu menu)
                    menu.sortBackpackItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                if (sortType == Util.INV_TYPE_STORAGE_BOX) {
                    // StorageBox sorting
                    if (player.containerMenu instanceof StorageBoxMenu menu)
                        menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                    // StorageBoxMounted sorting
                    if (player.containerMenu instanceof StorageBoxMountedMenu menu)
                        menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSyncClientSettingsPacket(SyncClientSettingsPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
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
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleTransferRecipePacket(TransferRecipePacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
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
                    container = new BackpackContainer(backpack, player);
                    itemHandler = container.getItemHandler();
                }

                List<Slot> inputSlots;
                if (player.containerMenu instanceof CraftingMenu openMenu) {
                    inputSlots = openMenu.slots.subList(1, 10);
                } else if (player.containerMenu instanceof StonecutterMenu openMenu) {
                    inputSlots = openMenu.slots.subList(0, 1);
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

                int ingredientIndex = 0;
                for (Integer entry : recipeMap) {
                    int craftingSlotIndex = entry - 1; // slot in the 3×3 grid (0–8)

                    if (craftingSlotIndex < 0 || craftingSlotIndex >= inputSlots.size()) {
                        return; // invalid data
                    }

                    Ingredient ingredient = ingredients.get(ingredientIndex);
                    if (ingredient.isEmpty()) {
                        ingredientIndex++;
                        continue;
                    }

                    ItemStack collected = ItemStack.EMPTY;
                    int remaining = maxCraftable;

                    // First: Try player inventory
                    for (int j = 0; j < playerInv.getContainerSize(); j++) {
                        ItemStack stack = playerInv.getItem(j);
                        if (!stack.isEmpty() && ingredient.test(stack)) {
                            int extractAmount = Math.min(stack.getCount(), remaining);
                            ItemStack extracted = stack.split(extractAmount);

                            if (collected.isEmpty()) {
                                collected = extracted.copy();
                            } else {
                                collected.grow(extracted.getCount());
                            }

                            playerInv.setItem(j, stack);
                            remaining -= extracted.getCount();

                            if (remaining <= 0) break;
                        }
                    }

                    // Then: Try backpack if needed
                    if (remaining > 0 && itemHandler != null) {
                        for (int j = 0; j < Util.ITEM_SLOT_END_RANGE; j++) {
                            ItemStack stack = itemHandler.getStackInSlot(j);
                            if (!stack.isEmpty() && ingredient.test(stack)) {
                                int extractAmount = Math.min(stack.getCount(), remaining);
                                ItemStack extracted = stack.split(extractAmount);

                                if (collected.isEmpty()) {
                                    collected = extracted.copy();
                                } else {
                                    collected.grow(extracted.getCount());
                                }

                                itemHandler.setStackInSlot(j, stack);
                                remaining -= extracted.getCount();

                                if (remaining <= 0) break;
                            }
                        }
                    }

                    // If we still couldn't gather enough
                    if (collected.isEmpty() || collected.getCount() < maxCraftable) {
                        return; // missing item(s)
                    }

                    inputSlots.get(craftingSlotIndex).set(collected);
                    ingredientIndex++;
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

            // Count matching items in player inventory
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    available += stack.getCount();
                }
            }

            // Count matching items in backpack, if present
            if (backpack != null) {
                for (int i = 0; i < backpack.getSlots(); i++) {
                    ItemStack stack = backpack.getStackInSlot(i);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        available += stack.getCount();
                    }
                }
            }

            // Limit max crafts by this ingredient
            maxCrafts = Math.min(maxCrafts, available / requiredPerCraft);
            if (maxCrafts == 0) {
                break; // Early exit if any ingredient is insufficient
            }
        }

        return maxCrafts;
    }


}
