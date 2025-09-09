package net.fxnt.fxntstorage.backpack.main;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.HighStackCountSync;
import net.fxnt.fxntstorage.network.packet.SetCarriedPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public class BackpackMenu extends AbstractContainerMenu {
    public IBackpackContainer container;

    public final Player player;
    public byte backpackType; // 1 = On Back, 2 = From Hand, 3 = BlockEntity

    public final int ITEM_SLOT_COUNT = BackpackBlock.getItemSlotCount();
    public final int TOOL_SLOT_COUNT = BackpackBlock.getToolSlotCount();
    public final int UPGRADE_SLOT_COUNT = BackpackBlock.getUpgradeSlotCount();
    public final int TOTAL_SLOT_COUNT = ITEM_SLOT_COUNT + TOOL_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    public boolean ctrlKeyDown = false;

    public BackpackMenu(MenuType<?> type, int containerId, Inventory playerInventory, IBackpackContainer container, byte backpackType) {
        super(type, containerId);
        this.player = playerInventory.player;
        this.backpackType = backpackType;
        this.container = container;

        initSlots();
    }

    public void initSlots() {
        Inventory playerInventory = player.getInventory();

        final IBackpackContainer finalContainer = container;
        ItemStackHandler itemHandler = container.getItemHandler();

        // Add Container Slots
        int index = 0;
        for (int i = 0; i < ITEM_SLOT_COUNT; i++) {
            addSlot(new BackpackSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
                @Override
                public boolean mayPlace(@NotNull ItemStack pStack) {
                    if ((pStack.getItem() instanceof BackpackItem)) return false;
                    return super.mayPlace(pStack);
                }

                @Override
                public int getMaxStackSize() {
                    return finalContainer.getStackMultiplier() * 64;
                }

                @Override
                public int getMaxStackSize(@NotNull ItemStack stack) {
                    return Math.max(finalContainer.getStackMultiplier() * stack.getMaxStackSize(), stack.getMaxStackSize());
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    finalContainer.setDataChanged();
                }
            });
            index++;
        }

        // Add Tool Slots
        for (int i = 0; i < TOOL_SLOT_COUNT; i++) {
            addSlot(new ToolSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
                @Override
                public boolean mayPlace(@NotNull ItemStack pStack) {
                    if ((pStack.getItem() instanceof BackpackItem)) return false;
                    return super.mayPlace(pStack);
                }

                @Override
                public void onTake(@NotNull Player pPlayer, @NotNull ItemStack pStack) {
                    super.onTake(pPlayer, pStack);
                }

                @Override
                public int getMaxStackSize(@NotNull ItemStack stack) {
                    return Math.min(super.getMaxStackSize(stack), stack.getMaxStackSize());
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    finalContainer.setDataChanged();
                }
            });
            index++;
        }

        // Add Upgrade Slots
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            addSlot(new UpgradeSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
                @Override
                public boolean mayPlace(@NotNull ItemStack pStack) {
                    if (pStack.is(ModTags.Items.BACKPACK_UPGRADE)) {
                        UpgradeItem item = (UpgradeItem) pStack.getItem();
                        return isUniqueUpgrade(itemHandler, item);
                    }
                    return false;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    finalContainer.setDataChanged();
                }
            });
            index++;
        }

        // Set slot yOffset for player inventory based on scaled GUI screen size
        int yOffset = 0;

        // Add Player Inventory Slots
        int xOffset = 61;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, y * 9 + x + 9, xOffset + Util.SLOT_SIZE * x, yOffset + y * Util.SLOT_SIZE));
            }
        }

        // Add Hot bar Slots
        yOffset += (Util.SLOT_SIZE * 3) + 4;
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, xOffset + i * Util.SLOT_SIZE, yOffset));
        }

    }

    @Override
    public void setSynchronizer(@NotNull ContainerSynchronizer pSynchronizer) {
        // Vanilla synchronizer transfers stack counts as bytes, need to override
        // and transmit stack counts as VarInt to allow for stacks > 127
        if (player instanceof ServerPlayer serverPlayer) {
            super.setSynchronizer(new HighStackCountSync(serverPlayer));
        } else {
            super.setSynchronizer(pSynchronizer);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (backpackType == Util.BACKPACK_IN_HAND) {
            ItemStack selectedStack = player.getInventory().getSelected();
            return selectedStack.getItem() instanceof BackpackItem;
        } else if (backpackType == Util.BACKPACK_ON_BACK) {
            return BackpackHelper.isWearingBackpack(player);
        }
        return true;
    }

    private boolean isUniqueUpgrade(IItemHandler itemHandler, Item upgradeItem) {
        for (int i = Util.UPGRADE_SLOT_START_RANGE; i < Util.UPGRADE_SLOT_END_RANGE; i++) {
            if (itemHandler.getStackInSlot(i).getItem() == upgradeItem) {
                return false;
            }
        }
        return true;
    }

    public SortOrder getSortOrder() {
        return container.getSortOrder();
    }

    public void setSortOrder(SortOrder order) {
        container.setSortOrder(order);
    }

    public int getSlotsSize() {
        return slots.size();
    }

    public @NotNull Slot getSlot(int slotIndex) {
        return slots.get(slotIndex);
    }

    public Slot getPlayerSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + slotIndex);
    }

    public Slot getHotbarSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + 27 + slotIndex);
    }

    @Override
    public void clicked(int pSlotId, int pButton, @NotNull ClickType pClickType, @NotNull Player pPlayer) {
        // Prevent moving backpack while it is open
        if (pSlotId >= 0 && backpackType == Util.BACKPACK_IN_HAND) {
            int selectedHotBarSlot = pPlayer.getInventory().selected;
            ItemStack selectedStack = pPlayer.getInventory().getSelected();
            if (pSlotId == getSlotsSize() - 36 + 27 + selectedHotBarSlot && selectedStack.getItem() instanceof BackpackItem)
                return;
        }

        if (pSlotId >= Util.UPGRADE_SLOT_START_RANGE && pSlotId < Util.UPGRADE_SLOT_END_RANGE) {
            toggleUpgrade(pSlotId, ctrlKeyDown);
            if (player.level().isClientSide) {
                if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL))
                    return;
            } else {
                if (ctrlKeyDown) return;
            }
        }

        // This variable is a hack to differentiate between automation and player interaction for extractItem/insertItem
        container.setPlayerInteraction(true);
        if (pClickType == ClickType.PICKUP && pButton == 1 || pClickType == ClickType.QUICK_MOVE && pButton == 0) {
            // Override right-click Pickup and left-click Quick Move behavior to handle large stack sizes
            ClickAction clickaction = pButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (pSlotId == -999) {
                if (!getCarried().isEmpty()) {
                    player.drop(getCarried().split(1), true);
                }
            } else if (pClickType == ClickType.QUICK_MOVE) {
                if (pSlotId < 0) return;

                Slot slot = slots.get(pSlotId);
                if (!slot.mayPickup(player)) return;

                for (ItemStack stack = quickMoveStack(player, pSlotId); !stack.isEmpty() && ItemStack.isSameItem(slot.getItem(), stack); stack = ItemStack.EMPTY) {
                }
            } else {
                if (pSlotId < 0) return;

                Slot slot = slots.get(pSlotId);
                ItemStack slotItem = slot.getItem();
                ItemStack carried = getCarried();
                player.updateTutorialInventoryAction(carried, slot.getItem(), clickaction);

                if (!super.tryItemClickBehaviourOverride(player, clickaction, slot, slotItem, carried)) {
                    if (slotItem.isEmpty()) {
                        if (!carried.isEmpty()) {
                            setCarried(slot.safeInsert(carried, 1));
                        }
                    } else if (slot.mayPickup(player)) {
                        if (carried.isEmpty()) {
                            Optional<ItemStack> tryRemove = slot.tryRemove(
                                    slotItem.getCount() > slotItem.getMaxStackSize()
                                            ? (slotItem.getMaxStackSize() + 1) / 2
                                            : (slotItem.getCount() + 1) / 2
                                    , Integer.MAX_VALUE, player);
                            tryRemove.ifPresent((stack) -> {
                                setCarried(stack);
                                slot.onTake(player, stack);
                            });
                        } else if (slot.mayPlace(carried)) {
                            if (ItemStack.isSameItemSameTags(slotItem, carried)) {
                                setCarried(slot.safeInsert(carried, 1));
                            } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                                setCarried(slotItem);
                                slot.setByPlayer(carried);
                            }
                        } else if (ItemStack.isSameItemSameTags(slotItem, carried)) {
                            Optional<ItemStack> tryRemove = slot.tryRemove(slotItem.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            tryRemove.ifPresent((stack) -> {
                                carried.grow(stack.getCount());
                                slot.onTake(player, stack);
                            });
                        }
                    }
                }

                slot.setChanged();
            }
        } else {
            super.clicked(pSlotId, pButton, pClickType, player);
        }
        container.setPlayerInteraction(false);
    }

    private boolean isUpgradeItem(@NotNull ItemStack itemStack) {
        return itemStack.is(ModTags.Items.BACKPACK_UPGRADE);
    }

    private boolean isToolItem(@NotNull ItemStack itemStack) {
        // Items that, when shift-clicked, get transferred into tool storage area
        return itemStack.is(Tags.Items.TOOLS)
                || itemStack.canPerformAction(ToolActions.SHEARS_HARVEST)
                || itemStack.is(Items.BRUSH)
                || itemStack.is(Items.WARPED_FUNGUS_ON_A_STICK)
                || itemStack.is(Items.CARROT_ON_A_STICK)
                || itemStack.is(Items.ELYTRA)
                || itemStack.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag)
                || itemStack.is(AllTags.AllItemTags.WRENCH.tag)
                || itemStack.is(AllItems.CARDBOARD_SWORD.asItem())
                || itemStack.is(AllItems.POTATO_CANNON.asItem());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack slotItem = slot.getItem();

        // If item is an upgrade item, put it into upgrade slot (Only from player inventory)
        if (isUpgradeItem(slotItem)) {
            if (player instanceof ServerPlayer serverPlayer)
                ModNetwork.sendToPlayer(serverPlayer, new SetCarriedPacket(ItemStack.EMPTY));

            if (index > Util.UPGRADE_SLOT_END_RANGE) {
                // Are there any free upgrade slots
                for (int i = Util.UPGRADE_SLOT_START_RANGE; i < Util.UPGRADE_SLOT_END_RANGE; i++) {
                    if (slots.get(i).getItem().isEmpty()) {
                        slots.get(i).safeInsert(slotItem);
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                toggleUpgrade(index, false);
            }
        }

        // If item is a tool item, put it into a tool slot (Only from player inventory)
        // WRENCH / SHEARS / BOWS / FISHING RODS / SHIELDS / BRUSH / * ON A STICK
        if (isToolItem(slotItem) && index >= Util.UPGRADE_SLOT_END_RANGE) {
            // Create a mapping between tool tags and the corresponding tool slot index offset
            Map<TagKey<Item>, Integer> toolSlotMap = Map.of(
                    ItemTags.SWORDS, 0,
                    ItemTags.PICKAXES, 1,
                    ItemTags.AXES, 2,
                    ItemTags.SHOVELS, 3,
                    ItemTags.HOES, 4
            );

            // Find the appropriate slot based on the tool type
            for (Map.Entry<TagKey<Item>, Integer> entry : toolSlotMap.entrySet()) {
                if (slotItem.is(entry.getKey())) {
                    int slotIndex = Util.TOOL_SLOT_START_RANGE + entry.getValue();
                    if (slots.get(slotIndex).getItem().isEmpty()) {
                        slots.get(slotIndex).safeInsert(slotItem);
                        return ItemStack.EMPTY;  // Exit early once the tool is placed
                    }
                }
            }

            // If no specific tool slot was available, search in the general range
            for (int i = Util.TOOL_SLOT_START_RANGE + toolSlotMap.size(); i < Util.TOOL_SLOT_END_RANGE; i++) {
                if (slots.get(i).getItem().isEmpty()) {
                    slots.get(i).safeInsert(slotItem);
                    return ItemStack.EMPTY;  // Exit once the tool is placed
                }
            }
        }

        // General Quick Move for all other items
        if (index >= 0 && index < slots.size()) {
            ItemStack itemStack = ItemStack.EMPTY;
            if (slot.hasItem()) {
                ItemStack itemStack2 = slot.getItem();
                itemStack = itemStack2.copy();
                if (index < Util.UPGRADE_SLOT_END_RANGE) {
                    // Move itemStack2 from container to player inventory, stacking items right->left
                    if (!moveItemStack(itemStack2, Util.UPGRADE_SLOT_END_RANGE, Util.UPGRADE_SLOT_END_RANGE + 36, true)) {
                        return ItemStack.EMPTY;
                    }
                    // Move itemStack2 from player inventory to container, stacking items left->right
                } else {
                    if (moveItemStack(itemStack2, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE, false)) {
                        return ItemStack.EMPTY;
                    }
                    // Next, try transferring to tool slot range, starting at offset 5
                    if (moveItemStack(itemStack2, Util.TOOL_SLOT_START_RANGE + 5, Util.TOOL_SLOT_END_RANGE, false)) {
                        return ItemStack.EMPTY;
                    }
                    // Finally, try transferring to the main tool slots as a last resort
                    if (moveItemStack(itemStack2, Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (itemStack2.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (itemStack2.getCount() == itemStack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.onTake(player, itemStack2);
            }
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    public boolean moveItemStack(ItemStack newStack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        while (!newStack.isEmpty()) {
            if (reverseDirection) {
                if (i < startIndex) {
                    break;
                }
            } else if (i >= endIndex) {
                break;
            }

            Slot slot = this.slots.get(i);
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(newStack, itemstack)) {
                int j = itemstack.getCount() + newStack.getCount();
                int k = slot.getMaxStackSize(itemstack);
                if (j <= k) {
                    newStack.setCount(0);
                    itemstack.setCount(j);
                    slot.setChanged();
                    flag = true;
                } else if (itemstack.getCount() < k) {
                    newStack.shrink(k - itemstack.getCount());
                    itemstack.setCount(k);
                    slot.setChanged();
                    flag = true;
                }
            }

            if (reverseDirection) {
                --i;
            } else {
                ++i;
            }
        }

        if (!newStack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot1 = this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(newStack)) {
                    int l = slot1.getMaxStackSize(newStack);
                    slot1.setByPlayer(newStack.split(Math.min(newStack.getCount(), l)));
                    slot1.setChanged();
                    flag = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    public void toggleUpgrade(int slotId, boolean ctrlKeyDown) {
        ItemStackHandler itemStackHandler = container.getItemHandler();

        ItemStack itemStack = itemStackHandler.getStackInSlot(slotId);
        if (itemStack.isEmpty()) {
            return; // Check if slot is valid and has an item
        }

        // Check if the item is a valid upgrade item
        if (itemStack.is(ModTags.Items.BACKPACK_UPGRADE) && itemStack.getItem() instanceof UpgradeItem upgradeItem) {
            String itemName = upgradeItem.getUpgradeName();
            String baseItemName = itemName
                    .replace("backpack_", "")
                    .replace("_upgrade", "")
                    .replace("_deactivated", "");

            if (ctrlKeyDown) {
                // Toggle between activated and deactivated versions
                if (itemName.contains("_deactivated")) {
                    itemStackHandler.setStackInSlot(slotId, getActivatedItem(baseItemName));
                } else {
                    itemStackHandler.setStackInSlot(slotId, getDeactivatedItem(baseItemName));
                }

                // Notify the container that the item has changed
                container.setDataChanged();
            } else {
                // If trying to pick up a deactivated item, toggle back to activated version
                if (itemName.contains("_deactivated")) {
                    ItemStack stack = getActivatedItem(baseItemName);
                    itemStackHandler.setStackInSlot(slotId, stack);
                    if (player instanceof ServerPlayer serverPlayer)
                        ModNetwork.sendToPlayer(serverPlayer, new SetCarriedPacket(stack));
                }
            }
        }
    }

    // Helper methods for toggleUpgrade() to retrieve item stacks
    private ItemStack getActivatedItem(String baseItemName) {
        return switch (baseItemName) {
            case "magnet" -> new ItemStack(ModItems.BACKPACK_MAGNET_UPGRADE.get());
            case "pickblock" -> new ItemStack(ModItems.BACKPACK_PICKBLOCK_UPGRADE.get());
            case "itempickup" -> new ItemStack(ModItems.BACKPACK_ITEMPICKUP_UPGRADE.get());
            case "flight" -> new ItemStack(ModItems.BACKPACK_FLIGHT_UPGRADE.get());
            case "refill" -> new ItemStack(ModItems.BACKPACK_REFILL_UPGRADE.get());
            case "feeder" -> new ItemStack(ModItems.BACKPACK_FEEDER_UPGRADE.get());
            case "toolswap" -> new ItemStack(ModItems.BACKPACK_TOOLSWAP_UPGRADE.get());
            case "falldamage" -> new ItemStack(ModItems.BACKPACK_FALLDAMAGE_UPGRADE.get());
            case "oremining" -> new ItemStack(ModItems.BACKPACK_OREMINING_UPGRADE.get());
            case "torchdeployer" -> new ItemStack(ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE.get());
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack getDeactivatedItem(String baseItemName) {
        return switch (baseItemName) {
            case "magnet" -> new ItemStack(ModItems.BACKPACK_MAGNET_UPGRADE_DEACTIVATED.get());
            case "pickblock" -> new ItemStack(ModItems.BACKPACK_PICKBLOCK_UPGRADE_DEACTIVATED.get());
            case "itempickup" -> new ItemStack(ModItems.BACKPACK_ITEMPICKUP_UPGRADE_DEACTIVATED.get());
            case "flight" -> new ItemStack(ModItems.BACKPACK_FLIGHT_UPGRADE_DEACTIVATED.get());
            case "refill" -> new ItemStack(ModItems.BACKPACK_REFILL_UPGRADE_DEACTIVATED.get());
            case "feeder" -> new ItemStack(ModItems.BACKPACK_FEEDER_UPGRADE_DEACTIVATED.get());
            case "toolswap" -> new ItemStack(ModItems.BACKPACK_TOOLSWAP_UPGRADE_DEACTIVATED.get());
            case "falldamage" -> new ItemStack(ModItems.BACKPACK_FALLDAMAGE_UPGRADE_DEACTIVATED.get());
            case "oremining" -> new ItemStack(ModItems.BACKPACK_OREMINING_UPGRADE_DEACTIVATED.get());
            case "torchdeployer" -> new ItemStack(ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE_DEACTIVATED.get());
            default -> ItemStack.EMPTY;
        };
    }

    public void sortBackpackItems(int startIndex, int endIndex, SortOrder sortOrder) {
        int stackMultiplier = container.getStackMultiplier();
        ServerPlayer sp = (ServerPlayer) player;

        // Create a map to track all items (with or without NBT)
        Map<Util.ItemWithNBT, Integer> itemCompMap = new HashMap<>();

        // Add all items in the container from startIndex to endIndex into the map
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = getSlot(i).getItem();
            if (!stack.isEmpty()) {
                CompoundTag tag = stack.getTag();
                Util.ItemWithNBT key = new Util.ItemWithNBT(stack.getItem(), tag);
                itemCompMap.merge(key, stack.getCount(), Integer::sum);
            }
        }

        // Create a list of entries and sort them
        List<Map.Entry<Util.ItemWithNBT, Integer>> sortedItems = new ArrayList<>(itemCompMap.entrySet());

        // COUNT=0 NAME=1 TAG=2
        switch (sortOrder) {
            case MOD:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(entry.getKey().item())).toString())  // Sort by registry name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            case NAME:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
                        .thenComparing(entry -> entry.getKey().getDisplayNameString()) // Then by custom name
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            default:
                // Default to COUNT
                sortedItems.sort(
                        Map.Entry.<Util.ItemWithNBT, Integer>comparingByValue().reversed()
                                .thenComparing(entry -> entry.getKey().toString())
                );
        }

        NonNullList<ItemStack> compactedList = NonNullList.withSize(endIndex - startIndex, ItemStack.EMPTY);

        // Special handling for tool slots
        if (startIndex == Util.TOOL_SLOT_START_RANGE && endIndex == Util.TOOL_SLOT_END_RANGE) {
            List<Class<? extends TieredItem>> toolOrder =
                    List.of(SwordItem.class, PickaxeItem.class, AxeItem.class, ShovelItem.class, HoeItem.class);

            for (int t = 0; t < toolOrder.size(); t++) {
                Class<? extends TieredItem> toolClass = toolOrder.get(t);

                Map.Entry<Util.ItemWithNBT, Integer> best = sortedItems.stream()
                        .filter(e -> toolClass.isInstance(e.getKey().item()))
                        .max(Comparator.comparingInt(e -> getVanillaTierIndex(new ItemStack(e.getKey().item()))))
                        .orElse(null);

                if (best != null) {
                    Util.ItemWithNBT key = best.getKey();

                    ItemStack stack = new ItemStack(key.item(), 1);
                    CompoundTag tag = key.tag();
                    if (tag != null && !tag.isEmpty()) stack.setTag(tag.copy());
                    compactedList.set(t, stack);

                    int remaining = best.getValue() - 1;
                    sortedItems.remove(best);
                    if (remaining > 0) {
                        sortedItems.add(Map.entry(key, remaining)); // put leftovers back
                    }
                }
            }
        }

        // Fill general slots starting after reserved
        int idx = (startIndex == Util.TOOL_SLOT_START_RANGE ? 5 : 0);
        for (Map.Entry<Util.ItemWithNBT, Integer> entry : sortedItems) {
            Util.ItemWithNBT key = entry.getKey();
            Item item = key.item();
            CompoundTag tag = key.tag();
            int totalCount = entry.getValue();

            ItemStack tempStack = new ItemStack(item, 1);
            int maxStackSize = (endIndex == Util.ITEM_SLOT_END_RANGE)
                    ? stackMultiplier * tempStack.getMaxStackSize()
                    : tempStack.getMaxStackSize();

            while (totalCount > 0 && idx < compactedList.size()) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                if (tag != null && !tag.isEmpty()) stack.setTag(tag.copy());
                compactedList.set(idx++, stack);
                totalCount -= stackSize;
            }

            // fallback: backfill empty reserved slots
            if (totalCount > 0 && startIndex == Util.TOOL_SLOT_START_RANGE) {
                for (int t = 0; t < 5 && totalCount > 0; t++) {
                    if (compactedList.get(t).isEmpty()) {
                        int stackSize = Math.min(totalCount, maxStackSize);
                        ItemStack stack = new ItemStack(item, stackSize);
                        if (tag != null && !tag.isEmpty()) stack.setTag(tag.copy());
                        compactedList.set(t, stack);
                        totalCount -= stackSize;
                    }
                }
            }
        }

        // Place sorted items back
        for (int i = 0; i < compactedList.size(); i++) {
            ItemStack stack = compactedList.get(i);
            Slot slot = player.containerMenu.getSlot(i + startIndex);
            slot.set(stack);

            if (startIndex >= Util.UPGRADE_SLOT_END_RANGE) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(
                        player.containerMenu.containerId, getStateId(), i + startIndex, stack));
            }
        }
    }

    public void setTag(CompoundTag tag) {
        if (!player.level().isClientSide) return;
        container.setTag(tag);
    }

    public static int getVanillaTierIndex(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof TieredItem tiered) {
            String tier = tiered.getTier().toString();

            return switch (tier) {
                case "WOOD" -> 0;
                case "STONE" -> 1;
                case "IRON" -> 2;
                case "GOLD" -> 3;
                case "DIAMOND" -> 4;
                case "NETHERITE" -> 5;
                default -> -1;
            };
        }
        return -1;
    }

}
