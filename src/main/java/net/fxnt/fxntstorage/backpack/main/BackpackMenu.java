package net.fxnt.fxntstorage.backpack.main;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.packet.SetCarriedPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BackpackMenu extends AbstractContainerMenu {
    public IBackpackContainer container;

    public final Player player;
    public byte backpackType; // 1 = On Back, 2 = From Hand, 3 = BlockEntity

    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    private final int itemSlotCount = BackpackBlock.getItemSlotCount();
    private final int toolSlotCount = BackpackBlock.getToolSlotCount();
    private final int upgradeSlotCount = BackpackBlock.getUpgradeSlotCount();
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

        final IBackpackContainer finalContainer = this.container;
        IItemHandler itemHandler = container.getItemHandler();

        // Add Container Slots
        int index = 0;
        for (int i = 0; i < itemSlotCount; i++) {
            this.addSlot(new BackpackSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
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
        for (int i = 0; i < toolSlotCount; i++) {
            this.addSlot(new ToolSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
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
        for (int i = 0; i < upgradeSlotCount; i++) {
            this.addSlot(new UpgradeSlot(itemHandler, index, index * Util.SLOT_SIZE, 0) {
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
                this.addSlot(new Slot(playerInventory, y * 9 + x + 9, xOffset + Util.SLOT_SIZE * x, yOffset + y * Util.SLOT_SIZE));
            }
        }

        // Add Hot bar Slots
        yOffset += (Util.SLOT_SIZE * 3) + 4;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, xOffset + i * Util.SLOT_SIZE, yOffset));
        }

    }

//    @Override
//    public void setSynchronizer(@NotNull ContainerSynchronizer pSynchronizer) {
//        // Vanilla synchronizer transfers stack counts as short, need to override
//        // and transmit stack counts as VarInt to allow for stacks > 32,767
//        if (player instanceof ServerPlayer serverPlayer) {
//            super.setSynchronizer(new HighStackCountSync(serverPlayer));
//        } else {
//            super.setSynchronizer(pSynchronizer);
//        }
//    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (backpackType == Util.BACKPACK_IN_HAND) {
            ItemStack selectedStack = player.getInventory().getSelected();
            return selectedStack.getItem() instanceof BackpackItem;
        } else if (backpackType == Util.BACKPACK_ON_BACK) {
            return BackpackHelper.isWearingBackpack(player);
        }
        return true; //TODO manual override!!
    }

    private boolean isUniqueUpgrade(IItemHandler itemHandler, Item upgradeItem) {
        for (int i = Util.UPGRADE_SLOT_START_RANGE; i < Util.UPGRADE_SLOT_END_RANGE; ++i) {
            if (itemHandler.getStackInSlot(i).getItem() == upgradeItem) {
                return false;
            }
        }
        return true;
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
            if (!this.player.level().isClientSide) {
                toggleUpgrade(pSlotId, ctrlKeyDown); // Only need to run this on the SERVER
            }
            if (ctrlKeyDown) return; // If we are on the SERVER and ctrl hotKey is down, return
            if (this.player.level().isClientSide) {
                if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL))
                    return;
            }
        }

        // This variable is a hack to differentiate between automation and player interaction for extractItem/insertItem
        this.container.setPlayerInteraction(true);
//        this.doThisClick(pSlotId, pButton, pClickType, player);
        FXNTStorage.LOGGER.debug("ClickType {}", pClickType);
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
        this.container.setPlayerInteraction(false);

    }

    private boolean isUpgradeItem(ItemStack itemStack) {
        return itemStack.is(ModTags.Items.BACKPACK_UPGRADE);
    }

    private boolean isToolItem(ItemStack itemStack) {
        // Items that, when shift-clicked, get transferred into tool storage area
        return itemStack.is(Tags.Items.TOOLS)
                || itemStack.is(Tags.Items.MELEE_WEAPON_TOOLS)
                || itemStack.is(Tags.Items.TOOLS_SHEAR)
                || itemStack.is(Items.BRUSH)
                || itemStack.is(Items.WARPED_FUNGUS_ON_A_STICK)
                || itemStack.is(Items.CARROT_ON_A_STICK)
                || itemStack.is(Items.ELYTRA)
                || itemStack.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag)
                || itemStack.is(AllTags.AllItemTags.WRENCH.tag)
                || itemStack.is(AllItems.CARDBOARD_SWORD);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        ItemStack slotItem = slot.getItem();

        // If item is an upgrade item, put it into upgrade slot (Only from player inventory)
        if (isUpgradeItem(slotItem) && index > Util.UPGRADE_SLOT_END_RANGE) {
            // Are there any free upgrade slots
            for (int i = Util.UPGRADE_SLOT_START_RANGE; i < Util.UPGRADE_SLOT_END_RANGE; i++) {
                if (this.slots.get(i).getItem().isEmpty()) {
                    this.slots.get(i).safeInsert(slotItem);
                    return ItemStack.EMPTY;
                }
            }
        }

        // If item is a tool item, put it into a tool slot (Only from player inventory)
        // WRENCH / SHEARS / BOWS / FISHING RODS / SHIELDS / BRUSH / * ON A STICK
        if (isToolItem(slotItem) && index > Util.UPGRADE_SLOT_END_RANGE) {
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
                    if (this.slots.get(slotIndex).getItem().isEmpty()) {
                        this.slots.get(slotIndex).safeInsert(slotItem);
                        return ItemStack.EMPTY;  // Exit early once the tool is placed
                    }
                }
            }

            // If no specific tool slot was available, search in the general range
            for (int i = Util.TOOL_SLOT_START_RANGE + 5; i < Util.TOOL_SLOT_END_RANGE; i++) {
                if (this.slots.get(i).getItem().isEmpty()) {
                    this.slots.get(i).safeInsert(slotItem);
                    return ItemStack.EMPTY;  // Exit once the tool is placed
                }
            }
        }

        // General Quick Move for all other items
        if (index >= 0 && index < this.slots.size()) {
            ItemStack itemStack = ItemStack.EMPTY;
            if (slot.hasItem()) {
                ItemStack itemStack2 = slot.getItem();
                itemStack = itemStack2.copy();
                if (index < Util.UPGRADE_SLOT_END_RANGE) {
                    // Move itemStack2 from container to player inventory, stacking items right->left
                    if (!this.moveItemStack(itemStack2, Util.UPGRADE_SLOT_END_RANGE, Util.UPGRADE_SLOT_END_RANGE + 36, true, true)) {
                        return ItemStack.EMPTY;
                    }
                    // Move itemStack2 from player inventory to container, stacking items left->right
                } else {
                    if (this.moveItemStack(itemStack2, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE, false, false)) {
                        return ItemStack.EMPTY;
                    }
                    // Next, try transferring to tool slot range, starting at offset 5
                    if (this.moveItemStack(itemStack2, Util.TOOL_SLOT_START_RANGE + 5, Util.TOOL_SLOT_END_RANGE, false, false)) {
                        return ItemStack.EMPTY;
                    }
                    // Finally, try transferring to the main tool slots as a last resort
                    if (this.moveItemStack(itemStack2, Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE, false, false)) {
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

    public boolean moveItemStack(ItemStack newStack, int startIndex, int endIndex, boolean reverseDirection, boolean fromContainer) {
        boolean bl = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        // From player to container //
        if ((!fromContainer && !newStack.isDamageableItem() && newStack.getComponentsPatch().isEmpty() && !newStack.isBarVisible())
                || (fromContainer && newStack.isStackable())) {
            while (!newStack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack slotStack = slot.getItem();
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(newStack, slotStack)) {
                    // Check if the slot is an UpgradeSlot and adjust the logic accordingly
                    if (slot instanceof UpgradeSlot && slotStack.getCount() >= 1) {
                        // Prevent any further stacking if it's an UpgradeSlot with an item already
                        break; // Break the loop to prevent combining items
                    }

                    int totalCount = slotStack.getCount() + newStack.getCount();
                    int maxPutSize = (fromContainer || i > Util.ITEM_SLOT_END_RANGE) ? Math.min(newStack.getMaxStackSize(), container.getStackMultiplier() * newStack.getMaxStackSize()) : Math.max(newStack.getMaxStackSize(), container.getStackMultiplier() * newStack.getMaxStackSize());
                    int availableSpace = maxPutSize - slotStack.getCount();

                    if (availableSpace > 0) {

                        if (totalCount <= maxPutSize) {
                            newStack.setCount(0);
                            slotStack.setCount(totalCount);
                            slot.setChanged();
                            bl = true;
                        } else if (availableSpace < newStack.getMaxStackSize()) {
                            newStack.shrink(availableSpace);
                            slotStack.setCount(maxPutSize);
                            slot.setChanged();
                            bl = true;
                        }

                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        // From container to player //
        if (!newStack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (reverseDirection ? i >= startIndex : i < endIndex) {
                Slot slot = this.slots.get(i);
                ItemStack slotStack = slot.getItem();

                if (slotStack.isEmpty() && slot.mayPlace(newStack)) {

                    int maxPutSize = (fromContainer) ? Math.min(newStack.getMaxStackSize(), this.container.getStackMultiplier() * newStack.getMaxStackSize()) : Math.max(newStack.getMaxStackSize(), this.container.getStackMultiplier() * newStack.getMaxStackSize());
                    int availableSpace = maxPutSize - slotStack.getCount();
                    //if (newStack.getCount() > availableSpace) {

                    if (fromContainer && !newStack.isStackable()) {
                        // If item is non-stackable only put 1 in
                        ItemStack inputStack = newStack.split(1);
                        slot.setByPlayer(inputStack);
                    } else {
                        // From Player (Stack Can Be Any Size)
                        if (newStack.getCount() > availableSpace) {
                            ItemStack inputStack = newStack.split(maxPutSize);
                            slot.setByPlayer(inputStack);
                        } else {
                            ItemStack inputStack = newStack.split(newStack.getCount());
                            slot.setByPlayer(inputStack);
                        }
                    }

                    slot.setChanged();
                    bl = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }

    public boolean toggleUpgrade(int slotId, boolean ctrlKeyDown) {
        IItemHandlerModifiable itemStackHandler = this.container.getItemHandler();

        ItemStack itemStack = itemStackHandler.getStackInSlot(slotId);
        if (itemStack.isEmpty()) {
            return false; // Check if slot is valid and has an item
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
                return true;
            } else {
                // If trying to pick up a deactivated item, toggle back to activated version
                if (itemName.contains("_deactivated")) {
                    ItemStack stack = getActivatedItem(baseItemName);
                    itemStackHandler.setStackInSlot(slotId, stack);
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SetCarriedPacket(stack));
//                    ModNetwork.sendToPlayer((ServerPlayer) player, new ClientboundSetCarriedPacket(stack));
                }
            }
        }
        return false;
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
            default -> ItemStack.EMPTY;
        };
    }

    public void sortBackpackItems(int startIndex, int endIndex, byte sortOrder) {
        int stackMultiplier = container.getStackMultiplier();
        ServerPlayer sp = (ServerPlayer) player;

        // Create a map to track all items (with or without Components)
        Map<Util.ItemWithComponent, Integer> itemCompMap = new HashMap<>();

        // Add all items in the container from startIndex to endIndex into the map
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = getSlot(i).getItem();
            if (!stack.isEmpty()) {
                // TODO: use stack.isStackable?
                DataComponentPatch patch = stack.getComponentsPatch();
                Util.ItemWithComponent key = new Util.ItemWithComponent(stack.getItem(), patch);
                itemCompMap.merge(key, stack.getCount(), Integer::sum);
            }
        }

        // Create a list of entries and sort them
        List<Map.Entry<Util.ItemWithComponent, Integer>> sortedItems = new ArrayList<>(itemCompMap.entrySet());

        // COUNT=0 NAME=1 TAG=2
        switch (sortOrder) {
            case 1:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithComponent, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            case 2:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithComponent, Integer> entry) -> Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(entry.getKey().item())).toString())  // Sort by registry name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            default:
                // Default to COUNT
                sortedItems.sort(
                        Map.Entry.<Util.ItemWithComponent, Integer>comparingByValue().reversed()
                                .thenComparing(entry -> entry.getKey().toString())
                );
        }

        NonNullList<ItemStack> compactedList = NonNullList.withSize(endIndex - startIndex, ItemStack.EMPTY);
        int idx = 0;

        // Rebuild the item stack list based on sorted entries
        for (Map.Entry<Util.ItemWithComponent, Integer> entry : sortedItems) {
            Util.ItemWithComponent key = entry.getKey();
            Item item = key.item();
            DataComponentPatch patch = key.patch();
            int totalCount = entry.getValue();

//            int maxStackSize = (endIndex == Util.ITEM_SLOT_END_RANGE) ? stackMultiplier * item.getMaxStackSize() : item.getMaxStackSize();
            ItemStack tempStack = new ItemStack(item, 1);
            int maxStackSize = (endIndex == Util.ITEM_SLOT_END_RANGE) ? stackMultiplier * tempStack.getMaxStackSize() : tempStack.getMaxStackSize();

            while (totalCount > 0) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                if (!patch.isEmpty()) {
                    stack.applyComponents(patch);
                }
                compactedList.set(idx, stack);
                totalCount -= stackSize;
                idx++;
            }
        }

        // Place the sorted items back into the inventory
        for (int i = 0; i < compactedList.size(); i++) {
            ItemStack stack = compactedList.get(i);
            Slot slot = player.containerMenu.getSlot(i + startIndex);
            slot.set(stack);

            if (startIndex >= Util.UPGRADE_SLOT_END_RANGE) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(player.containerMenu.containerId, getStateId(), i + startIndex, stack));
            }
        }
    }

    public void setContents(DataComponentPatch componentPatch) {
//        if (!player.level().isClientSide) return;
        this.container.setContents(componentPatch);
    }

    public void setChanged() {
        this.container.setDataChanged();
    }

}
