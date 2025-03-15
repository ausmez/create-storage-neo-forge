package net.fxnt.fxntstorage.backpacks.main;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllTags;
import net.fxnt.fxntstorage.backpacks.upgrades.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.HighStackCountSync;
import net.fxnt.fxntstorage.network.backpack.client.ClientboundSetCarriedPacket;
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
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        ItemStackHandler itemHandler = container.getItemHandler();

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
                    if (pStack.is(ModTags.Items.BACK_PACK_UPGRADE)) {
                        UpgradeItem item = (UpgradeItem) pStack.getItem();
                        return isUniqueUpgrade(item.getUpgradeName());
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
        return true; //TODO manual override!!
    }

    private boolean isUniqueUpgrade(String upgradeItem) {
        return !(new BackpackOnBackUpgradeHandler(player).hasUpgrade(upgradeItem));
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
            if (ctrlKeyDown) return; // If we are on the SERVER and ctrl key is down, return
            if (this.player.level().isClientSide) {
                if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL))
                    return;
            }
        }

        // This variable is a hack to differentiate between automation and player interaction for extractItem/insertItem
        this.container.setPlayerInteraction(true);
        this.doThisClick(pSlotId, pButton, pClickType, player);
        this.container.setPlayerInteraction(false);

    }

    private boolean isUpgradeItem(@NotNull ItemStack itemStack) {
        return itemStack.is(ModTags.Items.BACK_PACK_UPGRADE);
    }

    private boolean isToolItem(@NotNull ItemStack itemStack) {
        // Items that, when shift-clicked, get transferred into tool storage area
        return itemStack.is(ItemTags.TOOLS)
                || itemStack.is(Tags.Items.TOOLS)
                || itemStack.canPerformAction(ToolActions.SWORD_SWEEP)
                || itemStack.canPerformAction(ToolActions.SHEARS_HARVEST)
                || itemStack.is(Items.BRUSH)
                || itemStack.is(Items.WARPED_FUNGUS_ON_A_STICK)
                || itemStack.is(Items.CARROT_ON_A_STICK)
                || itemStack.is(Items.ELYTRA)
                || itemStack.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag)
                || itemStack.is(AllTags.AllItemTags.WRENCH.tag);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
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

        // From container to player/
        if ((!fromContainer && !newStack.isDamageableItem() && !newStack.hasTag() && !newStack.hasCustomHoverName() && !newStack.isBarVisible())
                || (fromContainer && newStack.isStackable())) {
            while (!newStack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack slotStack = slot.getItem();
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(newStack, slotStack)) {
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

    private void doThisClick(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        Inventory inventory = player.getInventory();

        if (clickType == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;

            // Args : clickedButton, Returns (0 : start drag, 1 : add slot, 2 : end drag)

            this.quickcraftStatus = getQuickcraftHeader(button);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(button);
                if (isValidQuickcraftType(this.quickcraftType, player)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot = this.slots.get(slotId);
                ItemStack carriedItemStack = this.getCarried();
                // Adjusted Method
                if (fxnt$canItemQuickReplace(slot, carriedItemStack, true)
                        && slot.mayPlace(carriedItemStack)
                        && (this.quickcraftType == 2 || carriedItemStack.getCount() > this.quickcraftSlots.size())
                        && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int j = this.quickcraftSlots.iterator().next().index;
                        this.resetQuickCraft();
                        this.doThisClick(j, this.quickcraftType, ClickType.PICKUP, player);
                        return;
                    }

                    ItemStack carriedItemStackCopy = this.getCarried().copy();
                    if (carriedItemStackCopy.isEmpty()) {
                        this.resetQuickCraft();
                        return;
                    }

                    int k = this.getCarried().getCount();

                    for (Slot slot2 : this.quickcraftSlots) {
                        ItemStack newCarriedItemStack = this.getCarried();
                        if (slot2 != null
                                && fxnt$canItemQuickReplace(slot2, newCarriedItemStack, true)
                                && slot2.mayPlace(newCarriedItemStack)
                                && (this.quickcraftType == 2 || newCarriedItemStack.getCount() >= this.quickcraftSlots.size())
                                && this.canDragTo(slot2)) {
                            int l = slot2.hasItem() ? slot2.getItem().getCount() : 0;

                            // Get Max Stack Size
                            int m = Math.min(carriedItemStackCopy.getMaxStackSize(), slot2.getMaxStackSize(carriedItemStackCopy));
                            //FXNTStorage.LOGGER.info("Quick Craft Math Min {} {} {}",m, carriedItemStackCopy.getMaxStackSize(), slot2.getMaxStackSize(carriedItemStackCopy));
                            int n = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots, this.quickcraftType, carriedItemStackCopy) + l, m);
                            k -= n - l;
                            slot2.setByPlayer(carriedItemStackCopy.copyWithCount(n));
                        }
                    }

                    carriedItemStackCopy.setCount(k);
                    this.setCarried(carriedItemStackCopy);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickAction clickAction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (slotId == -999) {
                // Clicked Outside
                if (!this.getCarried().isEmpty()) {
                    if (clickAction == ClickAction.PRIMARY) {
                        player.drop(this.getCarried(), true);
                        //FXNTStorage.LOGGER.info("Set Carried -1");
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                //FXNTStorage.LOGGER.info("QUICK MOVE TYPE");
                // Quick Move Stack
                if (slotId < 0) return;

                Slot slot = this.slots.get(slotId);

                if (!slot.mayPickup(player)) return;

                ItemStack itemStack = this.quickMoveStack(player, slotId);
                // Not sure how to deal with double click so instead use right click to move entire stack from container
                if (slotId >= Util.ITEM_SLOT_END_RANGE || clickAction == ClickAction.SECONDARY) {

                    int freeContainerSlot = -1;

                    // Free slot (need to determine if sending to tool / upgrade or main container slots
                    int slotRangeStart = 0;
                    int slotRangeEnd = Util.ITEM_SLOT_END_RANGE;
                    if (isUpgradeItem(itemStack)) {
                        slotRangeStart = Util.UPGRADE_SLOT_START_RANGE;
                        slotRangeEnd = Util.UPGRADE_SLOT_END_RANGE;
                    } else if (isToolItem((itemStack))) {
                        slotRangeStart = Util.TOOL_SLOT_START_RANGE;
                        slotRangeEnd = Util.TOOL_SLOT_END_RANGE;
                    }

//                    FXNTStorage.LOGGER.debug("slotRangeStart=[{}], slotRangeEnd=[{}]", slotRangeStart, slotRangeEnd);

                    for (int i = slotRangeStart; i < slotRangeEnd; ++i) {
                        if (container.getItemHandler().getStackInSlot(i).isEmpty()) {
                            freeContainerSlot = i;
                            break;
                        }
                    }

                    boolean freeSlots = slotId < Util.ITEM_SLOT_END_RANGE ? player.getInventory().getFreeSlot() > -1 : freeContainerSlot > -1;
                    while (freeSlots && !itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
                        itemStack = this.quickMoveStack(player, slotId);
                        freeSlots = slotId < Util.ITEM_SLOT_END_RANGE ? player.getInventory().getFreeSlot() > -1 : freeContainerSlot > -1;
                    }
                }
            } else {
                // PICKUP TYPE
//                FXNTStorage.LOGGER.info("PICKUP TYPE");
                if (slotId < 0) return;

                Slot slot = getSlot(slotId);
                ItemStack itemStack = slot.getItem();
                ItemStack carriedItemStack = getCarried();

                if (!tryItemClickBehaviourOverride(player, clickAction, slot, itemStack, carriedItemStack)) {
                    if (itemStack.isEmpty()) {
                        if (!carriedItemStack.isEmpty()) {
                            int o = clickAction == ClickAction.PRIMARY ? carriedItemStack.getCount() : 1;
//                            FXNTStorage.LOGGER.info("Do Click Safe Insert 1 {}", carriedItemStack.toString());
                            setCarried(safeInsertOverride(carriedItemStack, o, slot, slotId));
                        }
                    } else if (slot.mayPickup(player)) {
                        if (carriedItemStack.isEmpty()) {
                            int o = clickAction == ClickAction.PRIMARY ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                            Optional<ItemStack> optional = slot.tryRemove(o, slot.getItem().getMaxStackSize(), player);
//                            FXNTStorage.LOGGER.info("Set Carried 1");
                            optional.ifPresent(stack -> {
                                setCarried(stack);
                                slot.onTake(player, stack);
                            });
                        } else if (slot.mayPlace(carriedItemStack)) {
//                            FXNTStorage.LOGGER.info("Slot Max Stack Size {}", slot.getMaxStackSize(carriedItemStack));
                            if (ItemStack.isSameItemSameTags(itemStack, carriedItemStack)) {
                                int o = clickAction == ClickAction.PRIMARY ? carriedItemStack.getCount() : 1;
//                                FXNTStorage.LOGGER.info("Do Click Safe Insert 2 {}", carriedItemStack.toString());
                                setCarried(safeInsertOverride(carriedItemStack, o, slot, slotId));

                            } else if (carriedItemStack.getCount() <= slot.getMaxStackSize(carriedItemStack)) {
//                                FXNTStorage.LOGGER.info("Set Carried 2");

                                // Prevent swapping with item that has more than a stack
                                int amountToPickup = slot.getItem().getCount();
                                int defaultStackSize = slot.getItem().getMaxStackSize();

                                if (amountToPickup <= defaultStackSize) {
                                    setCarried(itemStack);
                                    slot.setByPlayer(carriedItemStack);
                                }
                            }
                        } else if (ItemStack.isSameItemSameTags(itemStack, carriedItemStack)) {
                            Optional<ItemStack> optional2 = slot.tryRemove(itemStack.getCount(), carriedItemStack.getMaxStackSize() - carriedItemStack.getCount(), player);
                            optional2.ifPresent(stack -> {
                                carriedItemStack.grow(stack.getCount());
                                slot.onTake(player, stack);
                            });
                        }
                    }
                }

                slot.setChanged();
            }
        } else if (clickType == ClickType.SWAP) {
            //FXNTStorage.LOGGER.info("SWAP");
            Slot thisSlot = this.slots.get(slotId);
            ItemStack inventoryItemStack = inventory.getItem(button);
            ItemStack thisSlotItem = thisSlot.getItem();

            if ((!inventoryItemStack.isEmpty() || !thisSlotItem.isEmpty())) {
                if (inventoryItemStack.isEmpty()) {
                    if (thisSlot.mayPickup(player)) {
                        // DROP ITEM INTO PLAYER INVENTORY
                        //FXNTStorage.LOGGER.info("DROP ITEM INTO PLAYER INVENTORY");
                        int itemMaxSize = thisSlotItem.getItem().getMaxStackSize(thisSlotItem);
                        int amountToMove = thisSlotItem.getCount();
                        //FXNTStorage.LOGGER.info("Max Allowed {} Got {}", itemMaxSize, amountToMove);
                        if (amountToMove <= itemMaxSize) {
                            inventory.setItem(button, thisSlotItem);
                            thisSlot.setByPlayer(ItemStack.EMPTY);
                            thisSlot.onTake(player, thisSlotItem);
                        } else {
                            // THIS WORKS
                            //FXNTStorage.LOGGER.info("Too Big");
                            ItemStack newStack = thisSlotItem.copyWithCount(amountToMove - itemMaxSize);
                            inventory.setItem(button, thisSlotItem.copyWithCount(itemMaxSize));
                            thisSlot.setByPlayer(newStack);
                            //thisSlot.getItem().setCount(amountToMove - itemMaxSize);
                            thisSlot.onTake(player, thisSlotItem);
                        }
                    }
                } else if (thisSlotItem.isEmpty()) {
                    if (thisSlot.mayPlace(inventoryItemStack)) {
                        //FXNTStorage.LOGGER.info("This Slot May Pace Max Size {}", thisSlot.getMaxStackSize(inventoryItemStack));
                        int p = thisSlot.getMaxStackSize(inventoryItemStack);
                        if (inventoryItemStack.getCount() > p) {
                            thisSlot.setByPlayer(inventoryItemStack.split(p));
                        } else {
                            inventory.setItem(button, ItemStack.EMPTY);
                            thisSlot.setByPlayer(inventoryItemStack);
                        }
                    }
                } else if (thisSlot.mayPickup(player) && thisSlot.mayPlace(inventoryItemStack)) {

                    //FXNTStorage.LOGGER.info("This Slot May Pickup Max Size {}", thisSlot.getMaxStackSize(inventoryItemStack));
                    int p = thisSlot.getMaxStackSize(inventoryItemStack);
                    if (inventoryItemStack.getCount() > p) {
                        // TODO HAVEN'T BEEN ABLE TO TRIGGER THIS SECTION
                        //FXNTStorage.LOGGER.info("TODO SWAP NOT TRIGGERED YET");
                        thisSlot.setByPlayer(inventoryItemStack.split(p));
                        thisSlot.onTake(player, thisSlotItem);
                        if (!inventory.add(thisSlotItem)) {
                            player.drop(thisSlotItem, true);
                        }
                    } else {
                        int itemMaxSize = thisSlotItem.getItem().getMaxStackSize(thisSlotItem);
                        int amountToMove = thisSlotItem.getCount();
                        // WORKS
                        //FXNTStorage.LOGGER.info("Else Max Size {} Got {}", itemMaxSize, amountToMove);
                        if (amountToMove <= itemMaxSize) {
                            inventory.setItem(button, thisSlotItem);
                            thisSlot.setByPlayer(inventoryItemStack);
                            thisSlot.onTake(player, thisSlotItem);
                        } else {
                            // WORKS
                            //FXNTStorage.LOGGER.warn("Fail as inventory slot too big to move");
                        }
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && slotId >= 0) {
            // CREATIVE MODE
            //FXNTStorage.LOGGER.info("CLONE");
            Slot thisSlot = this.slots.get(slotId);
            if (thisSlot.hasItem()) {
                ItemStack inventoryItemStack = thisSlot.getItem();

                //FXNTStorage.LOGGER.info("Inventory Stack Max Size {}", inventoryItemStack.getMaxStackSize());
                this.setCarried(inventoryItemStack.copyWithCount(inventoryItemStack.getMaxStackSize()));
            }
        } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && slotId >= 0) {
            // THROW ITEM WORKS!
            //FXNTStorage.LOGGER.info("THROW");
            Slot thisSlot = this.slots.get(slotId);
            int j = button == 0 ? 1 : thisSlot.getItem().getCount();
            //ItemStack itemStack = thisSlot.safeTake(j, Integer.MAX_VALUE, player);
            ItemStack itemStack = thisSlot.safeTake(j, thisSlot.getItem().getMaxStackSize(), player);
            player.drop(itemStack, true);
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            //FXNTStorage.LOGGER.info("PICK UP ALL");
            Slot thisSlot = this.slots.get(slotId);
            ItemStack inventoryItemStack = this.getCarried();
            if (!inventoryItemStack.isEmpty() && (!thisSlot.hasItem() || !thisSlot.mayPickup(player))) {
                int k = button == 0 ? 0 : this.slots.size() - 1;
                int p = button == 0 ? 1 : -1;

                for (int o = 0; o < 2; ++o) {
                    //FXNTStorage.LOGGER.info("Inventory Stack Max Size {}", inventoryItemStack.getMaxStackSize());
                    for (int q = k; q >= 0 && q < this.slots.size() && inventoryItemStack.getCount() < inventoryItemStack.getMaxStackSize(); q += p) {
                        Slot slot4 = this.slots.get(q);
                        if (slot4.hasItem() && this.fxnt$canItemQuickReplace(slot4, inventoryItemStack, true) && slot4.mayPickup(player) && this.canTakeItemForPickAll(inventoryItemStack, slot4)) {
                            ItemStack itemStack5 = slot4.getItem();

                            //FXNTStorage.LOGGER.info("Item Stack 5 Max Size {}", itemStack5.getMaxStackSize());
                            if (o != 0 || itemStack5.getCount() != itemStack5.getMaxStackSize()) {
                                //FXNTStorage.LOGGER.info("Inventory Stack Max Size {}", inventoryItemStack.getMaxStackSize());
                                ItemStack itemStack6 = slot4.safeTake(itemStack5.getCount(), inventoryItemStack.getMaxStackSize() - inventoryItemStack.getCount(), player);
                                inventoryItemStack.grow(itemStack6.getCount());
                            }
                        }
                    }
                }
            }
        }
    }

    public ItemStack safeInsertOverride(@NotNull ItemStack pStack, int pIncrement, Slot pSlot, int pSlotId) {
        if (!pStack.isEmpty() && pSlot.mayPlace(pStack)) {
            ItemStack itemstack = pSlot.getItem();
            int maxSlotStackSize = (pSlotId < Util.ITEM_SLOT_END_RANGE) ? Math.max(container.getStackMultiplier() * pStack.getMaxStackSize(), pStack.getMaxStackSize()) : Math.min(container.getStackMultiplier() * pStack.getMaxStackSize(), pStack.getMaxStackSize());
            int availableSpace = maxSlotStackSize - itemstack.getCount();

            // Determine the number of items to insert
            int i = Math.min(Math.min(pIncrement, pStack.getCount()), availableSpace);

            if (itemstack.isEmpty()) {
                pSlot.setByPlayer(pStack.split(i));
            } else if (ItemStack.isSameItemSameTags(itemstack, pStack)) {
                pStack.shrink(i);
                itemstack.grow(i);
                pSlot.setByPlayer(itemstack);
            }

        }
        return pStack;
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    private boolean tryItemClickBehaviourOverride(@NotNull Player player, ClickAction action, Slot slot, ItemStack clickedItem, @NotNull ItemStack carriedItem) {
        FeatureFlagSet featureFlagSet = player.level().enabledFeatures();
        if (carriedItem.isItemEnabled(featureFlagSet) && carriedItem.overrideStackedOnOther(slot, action, player)) {
            return true;
        } else {
            return clickedItem.isItemEnabled(featureFlagSet) && clickedItem.overrideOtherStackedOnMe(carriedItem, slot, action, player, this.createCarriedSlotAccess());
        }
    }

    public boolean fxnt$canItemQuickReplace(@Nullable Slot slot, ItemStack stack, boolean stackSizeMatters) {
        boolean flag = slot == null || !slot.hasItem();
        if (!flag && ItemStack.isSameItemSameTags(stack, slot.getItem())) {

            int maxSlotSize = stack.getMaxStackSize();
            if (slot instanceof UpgradeSlot) {
                maxSlotSize = UpgradeSlot.getMaxStackSizeStatic();
            }
            return slot.getItem().getCount() + (stackSizeMatters ? 0 : stack.getCount()) <= maxSlotSize;
        } else {
            return flag;
        }
    }

    public boolean toggleUpgrade(int slotId, boolean ctrlKeyDown) {
        ItemStackHandler itemStackHandler = this.container.getItemHandler();

        ItemStack itemStack = itemStackHandler.getStackInSlot(slotId);
        if (itemStack.isEmpty()) {
            return false; // Check if slot is valid and has an item
        }

        // Check if the item is a valid upgrade item
        if (itemStack.is(ModTags.Items.BACK_PACK_UPGRADE) && itemStack.getItem() instanceof UpgradeItem upgradeItem) {
            String itemName = upgradeItem.getUpgradeName();
            String baseItemName = itemName
                    .replace("back_pack_", "")
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
                    ModNetwork.sendToPlayer((ServerPlayer) player, new ClientboundSetCarriedPacket(stack));
                }
            }
        }
        return false;
    }

    // Helper methods for toggleUpgrade() to retrieve item stacks
    private ItemStack getActivatedItem(String baseItemName) {
        return switch (baseItemName) {
            case "magnet" -> new ItemStack(ModItems.BACK_PACK_MAGNET_UPGRADE.get());
            case "pickblock" -> new ItemStack(ModItems.BACK_PACK_PICKBLOCK_UPGRADE.get());
            case "itempickup" -> new ItemStack(ModItems.BACK_PACK_ITEMPICKUP_UPGRADE.get());
            case "flight" -> new ItemStack(ModItems.BACK_PACK_FLIGHT_UPGRADE.get());
            case "refill" -> new ItemStack(ModItems.BACK_PACK_REFILL_UPGRADE.get());
            case "feeder" -> new ItemStack(ModItems.BACK_PACK_FEEDER_UPGRADE.get());
            case "toolswap" -> new ItemStack(ModItems.BACK_PACK_TOOLSWAP_UPGRADE.get());
            case "falldamage" -> new ItemStack(ModItems.BACK_PACK_FALLDAMAGE_UPGRADE.get());
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack getDeactivatedItem(String baseItemName) {
        return switch (baseItemName) {
            case "magnet" -> new ItemStack(ModItems.BACK_PACK_MAGNET_UPGRADE_DEACTIVATED.get());
            case "pickblock" -> new ItemStack(ModItems.BACK_PACK_PICKBLOCK_UPGRADE_DEACTIVATED.get());
            case "itempickup" -> new ItemStack(ModItems.BACK_PACK_ITEMPICKUP_UPGRADE_DEACTIVATED.get());
            case "flight" -> new ItemStack(ModItems.BACK_PACK_FLIGHT_UPGRADE_DEACTIVATED.get());
            case "refill" -> new ItemStack(ModItems.BACK_PACK_REFILL_UPGRADE_DEACTIVATED.get());
            case "feeder" -> new ItemStack(ModItems.BACK_PACK_FEEDER_UPGRADE_DEACTIVATED.get());
            case "toolswap" -> new ItemStack(ModItems.BACK_PACK_TOOLSWAP_UPGRADE_DEACTIVATED.get());
            case "falldamage" -> new ItemStack(ModItems.BACK_PACK_FALLDAMAGE_UPGRADE_DEACTIVATED.get());
            default -> ItemStack.EMPTY;
        };
    }

    public void sortBackpackItems(int startIndex, int endIndex, byte sortOrder) {
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
            case 1:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            case 2:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(entry.getKey().item())).toString())  // Sort by registry name (ascending)
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
        int idx = 0;

        // Rebuild the item stack list based on sorted entries
        for (Map.Entry<Util.ItemWithNBT, Integer> entry : sortedItems) {
            Util.ItemWithNBT key = entry.getKey();
            Item item = key.item();
            CompoundTag tag = key.tag();
            int totalCount = entry.getValue();

//            int maxStackSize = (endIndex == Util.ITEM_SLOT_END_RANGE) ? stackMultiplier * item.getMaxStackSize() : item.getMaxStackSize();
            ItemStack tempStack = new ItemStack(item, 1);
            int maxStackSize = (endIndex == Util.ITEM_SLOT_END_RANGE) ? stackMultiplier * tempStack.getMaxStackSize() : tempStack.getMaxStackSize();

            while (totalCount > 0) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                if (tag != null) {
                    stack.setTag(tag);
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

    public void setTag(CompoundTag tag) {
        if (!player.level().isClientSide) return;
        this.container.setTag(tag);
    }

}
