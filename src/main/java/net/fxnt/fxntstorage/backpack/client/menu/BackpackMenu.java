package net.fxnt.fxntstorage.backpack.client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.slot.*;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.mounted.BackpackMountedStorage;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.crafting.BackpackCraftingContainer;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopUpgrade;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.fxnt.fxntstorage.network.packet.SetActivePanelPacket;
import net.fxnt.fxntstorage.network.packet.SetCarriedPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BackpackMenu extends AbstractContainerMenu {
    public IBackpackContainer container;
    public final Player player;
    public final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    protected final UpgradeDataSync upgradeSync;

    private final Map<UpgradeType, List<Slot>> upgradeSlots = new EnumMap<>(UpgradeType.class);

    private boolean ctrlKeyDown = false;
    @Nullable
    protected BlockPos blockPos;
    protected int contraptionId = -1;

    private Runnable upgradeSlotListener = null;
    private final BackpackType type;

    @Nullable
    private BackpackCraftingContainer craftingContainer;
    @Nullable
    private ResultContainer craftingResultContainer;
    @Nullable
    private Slot craftingResultSlot;

    public enum BackpackType {ITEM, WORN, BLOCK, CONTRAPTION}

    private record MenuContext(IBackpackContainer container, BackpackType type, @Nullable BlockPos pos,
                               int contraptionId) {
    }

    public BackpackMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(ModMenuTypes.BACKPACK_MENU.get(), containerId, inv, context(inv, buf));
    }

    private BackpackMenu(MenuType<?> menuType, int containerId, Inventory inv, MenuContext ctx) {
        this(menuType, containerId, inv, ctx.container(), ctx.type(), ctx.pos());
        this.contraptionId = ctx.contraptionId();
    }

    private static MenuContext context(Inventory inv, FriendlyByteBuf buf) {
        BackpackType type = buf.readEnum(BackpackType.class);

        return switch (type) {
            case ITEM -> {
                BackpackContainer container = new BackpackContainer(
                        inv.player, inv.player.getItemInHand(InteractionHand.MAIN_HAND)
                );
                yield new MenuContext(container, type, null, -1);
            }
            case WORN -> {
                BackpackContainer container = BackpackContainer.Cache.getOrCreateWornBackpack(
                        inv.player, BackpackHelper.getEquippedBackpackStack(inv.player)
                );
                yield new MenuContext(container, type, null, -1);
            }
            case BLOCK -> {
                BlockPos pos = buf.readBlockPos();
                BackpackEntity be = (BackpackEntity) inv.player.level().getBlockEntity(pos);
                yield new MenuContext(be, type, pos, -1);
            }
            case CONTRAPTION -> {
                int contraptionId = buf.readInt();
                BlockPos localPos = buf.readBlockPos();
                int stackMultiplier = buf.readInt();
                BackpackMountedStorage storage = null;
                Entity entity = inv.player.level().getEntity(contraptionId);
                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    MountedItemStorage s = contraptionEntity.getContraption().getStorage()
                            .getAllItemStorages().get(localPos);
                    if (s instanceof BackpackMountedStorage bms) storage = bms;
                }
                if (storage == null) storage = BackpackMountedStorage.createEmpty(stackMultiplier);
                yield new MenuContext(storage, type, null, contraptionId);
            }
        };
    }

    public BackpackMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, IBackpackContainer container, BackpackType type, @Nullable BlockPos pos) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.container = container;
        this.blockPos = pos;
        this.type = type;

        // Setup upgrade data sync
        this.upgradeSync = UpgradeDataSync.builder()
                .withBoolean(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, val))
                .withBoolean(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE, val))
                .withBoolean(UpgradeDataSync.Field.JETPACK_OVERLAY,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.JETPACK_OVERLAY),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.JETPACK_OVERLAY, val))
                .withBoolean(UpgradeDataSync.Field.JETPACK_BOBBING,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.JETPACK_BOBBING),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.JETPACK_BOBBING, val))
                .withBoolean(UpgradeDataSync.Field.JUKEBOX_PLAYING, () -> {
                    if (player.level().isClientSide) return false;
                    return switch (type) {
                        case WORN -> JukeboxHandler.isPlayerPlaying((ServerPlayer) player);
                        case BLOCK -> blockPos != null && JukeboxHandler.isBlockPlaying(player.level(), blockPos);
                        case CONTRAPTION -> contraptionId >= 0 && JukeboxHandler.isEntityPlaying(contraptionId);
                        default -> false;
                    };
                })
                .withBoolean(UpgradeDataSync.Field.JUKEBOX_MUTED, () -> {
                    if (player.level().isClientSide) return false;
                    return switch (type) {
                        case WORN -> JukeboxHandler.isPlayerMuted((ServerPlayer) player);
                        case BLOCK -> blockPos != null && JukeboxHandler.isBlockMuted(player.level(), blockPos);
                        case CONTRAPTION -> contraptionId >= 0 && JukeboxHandler.isEntityMuted(contraptionId);
                        default -> false;
                    };
                })
                .withBoolean(UpgradeDataSync.Field.MAGNET_IGNORE_FAN,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.MAGNET_IGNORE_FAN),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.MAGNET_IGNORE_FAN, val))
                .withBoolean(UpgradeDataSync.Field.OREMINING_ORES_ONLY,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY, val))
                .withBoolean(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN, val))
                .withBoolean(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD, val))
                .withBoolean(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH,
                        () -> container.getUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH),
                        (idx, val) -> container.setUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH, val))
                .withInteger(UpgradeDataSync.Field.EXPANDED_PANELS,
                        () -> UpgradeType.toPanelSyncValue(container.getExpandedPanel()),
                        (idx, val) -> {
                            container.setExpandedPanel(UpgradeType.fromPanelSyncValue(val));
                            onUpgradeSlotChanged();
                        })
                .build();
        addDataSlots(upgradeSync);

        initSlots();
        updateBackpackDataFromContainer();
        recomputeCraftingResult();
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        // Prevent moving backpack while it is open
        if (pSlotId >= 0 && type == BackpackType.ITEM) {
            int selectedHotBarSlot = pPlayer.getInventory().selected;
            ItemStack selectedStack = pPlayer.getInventory().getSelected();
            if (pSlotId == slots.size() - 36 + 27 + selectedHotBarSlot && selectedStack.getItem() instanceof BackpackItem)
                return;
        }

        if (layout.upgrades().contains(pSlotId)) {
            toggleUpgrade(pSlotId, ctrlKeyDown); // Only need to run this on the SERVER
            if (ctrlKeyDown) return; // If we are on the SERVER and ctrl hotKey is down, return
            if (player.level().isClientSide) {
                if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LCONTROL))
                    return;
            }
        }

        // Handle upgrade specific click events
        for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
            UpgradeContext ctx = UpgradeContext.forMenuWithSlot(
                    this, player, container, getBackpackType(), blockPos, pSlotId, pButton
            );
            if (upgrade.clicked(ctx))
                return;
        }

        if (pClickType == ClickType.PICKUP && craftingResultSlot != null
                && pSlotId == craftingResultSlot.index) {
            craftResultToCarried(pPlayer);
            return;
        }

        // Handle SWAP click type (F key for offhand, number keys for hotbar)
        if (pClickType == ClickType.SWAP && layout.items().contains(pSlotId)) {
            Slot slot = slots.get(pSlotId);
            ItemStack slotItem = slot.getItem();

            int targetSlot = pButton == 40 ? Inventory.SLOT_OFFHAND : pButton;
            ItemStack targetItem = pPlayer.getInventory().getItem(targetSlot);

            if (!slotItem.isEmpty() && !targetItem.isEmpty() && ItemStack.isSameItemSameComponents(slotItem, targetItem))
                return;

            if (!slotItem.isEmpty()) {
                int maxStackSize = slotItem.getMaxStackSize();

                if (slotItem.getCount() > maxStackSize) {
                    if (!slotItem.getItem().equals(targetItem.getItem()) && !targetItem.isEmpty()) return;

                    ItemStack toSwap = slotItem.split(maxStackSize);

                    if (!targetItem.isEmpty()) {
                        slot.safeInsert(targetItem.copy(), targetItem.getCount());
                        targetItem.setCount(0); // Clear the target
                    }

                    pPlayer.getInventory().setItem(targetSlot, toSwap);

                    slot.setChanged();
                    return;
                }
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
                            tryRemove.ifPresent(stack -> {
                                setCarried(stack);
                                slot.onTake(player, stack);
                            });
                        } else if (slot.mayPlace(carried)) {
                            if (ItemStack.isSameItemSameComponents(slotItem, carried)) {
                                setCarried(slot.safeInsert(carried, 1));
                            } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                                setCarried(slotItem);
                                slot.setByPlayer(carried);
                            }
                        } else if (ItemStack.isSameItemSameComponents(slotItem, carried)) {
                            Optional<ItemStack> tryRemove = slot.tryRemove(slotItem.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            tryRemove.ifPresent(stack -> {
                                carried.grow(stack.getCount());
                                slot.onTake(player, stack);
                            });
                        }
                    }
                }

                slot.setChanged();
            }
        } else if (pClickType == ClickType.PICKUP && pButton == 0) {
            if (pSlotId < 0) {
                super.clicked(pSlotId, pButton, pClickType, pPlayer);
            } else {
                Slot slot = this.slots.get(pSlotId);
                ItemStack slotItem = slot.getItem();
                ItemStack carried = this.getCarried();

                if (slotItem.isEmpty()) {
                    if (!carried.isEmpty() && slot.mayPlace(carried)) {
                        setCarried(slot.safeInsert(carried));
                    }
                } else if (slot.mayPickup(player)) {
                    if (carried.isEmpty()) {
                        // Picking up from slot with empty hand - cap at max stack size
                        int maxCarrySize = slotItem.getMaxStackSize();
                        int amountToTake = Math.min(slotItem.getCount(), maxCarrySize);

                        ItemStack toCarry = slotItem.split(amountToTake);
                        setCarried(toCarry);
                        slot.onTake(player, toCarry);
                    } else if (slot.mayPlace(carried)) {
                        // Both slot and carried have items
                        if (ItemStack.isSameItemSameComponents(slotItem, carried)) {
                            // Same item - merge
                            setCarried(slot.safeInsert(carried, carried.getCount()));
                        } else {
                            // Different items - swap if valid
                            int maxCarrySize = slotItem.getMaxStackSize();

                            if (slotItem.getCount() > maxCarrySize) {
                                // Slot has more than can be carried - try to split
                                ItemStack toCarry = slotItem.split(maxCarrySize);

                                // Check if the remaining slotItem can accept the carried stack
                                if (slotItem.isEmpty() || (ItemStack.isSameItemSameComponents(slotItem, carried) &&
                                        slotItem.getCount() + carried.getCount() <= slot.getMaxStackSize(carried))) {
                                    // Valid swap
                                    if (!slotItem.isEmpty()) {
                                        slotItem.grow(carried.getCount());
                                    } else {
                                        slot.setByPlayer(carried);
                                    }
                                    setCarried(toCarry);
                                } else {
                                    // Invalid swap - restore the slot
                                    slotItem.grow(toCarry.getCount());
                                }
                            } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                                // Normal swap - slotItem fits in carried
                                setCarried(slotItem);
                                slot.setByPlayer(carried);
                            }
                        }
                    }
                }
            }
        } else {
            super.clicked(pSlotId, pButton, pClickType, pPlayer);
        }
        container.setPlayerInteraction(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack slotItem = slot.getItem();

        // Delegate to any upgrade that wants to intercept shift-click for this slot/item.
        for (UpgradeType upgradeType : UpgradeType.values()) {
            IUpgrade upgrade = UpgradeRegistry.get(upgradeType);
            if (upgrade == null) continue;

            UpgradeContext ctx = UpgradeContext.forMenuWithSlot(this, player, container, type, blockPos, slot.index, -1);
            Optional<ItemStack> result = upgrade.onQuickMove(ctx);
            if (result.isPresent()) {
                return result.get();
            }
        }

        // If item is an upgrade item, put it into upgrade slot (Only from player inventory)
        if (slotItem.is(ModTags.Items.BACKPACK_UPGRADE)) {
            if (player instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new SetCarriedPacket(ItemStack.EMPTY));

            if (!layout.upgrades().contains(index)) {
                // Are there any free upgrade slots
                for (int i : layout.upgrades().range()) {
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
        if (isToolItem(slotItem) && layout.getAllSections().stream().noneMatch(section -> section.contains(index))) {
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
                    int slotIndex = layout.tools().getStartIndex() + entry.getValue();
                    if (slots.get(slotIndex).getItem().isEmpty()) {
                        slots.get(slotIndex).safeInsert(slotItem);
                        return ItemStack.EMPTY;  // Exit once the tool is placed
                    }
                }
            }

            // If no specific tool slot was available, search in the general range
            for (int i = layout.tools().getStartIndex() + toolSlotMap.size(); i < layout.tools().getEndIndex(); i++) {
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

                if (layout.items().contains(index) || layout.tools().contains(index) ||
                        layout.upgrades().contains(index) || layout.jukeboxDiscs().contains(index)) {
                    // Move itemStack2 from container to player inventory, stacking items right->left
                    if (!moveItemStack(itemStack2, layout.getTotalSlots(), layout.getTotalSlots() + 36, true)) {
                        return ItemStack.EMPTY;
                    }
                    // Move itemStack2 from player inventory to container, stacking items left->right
                } else {
                    // Move backpack inventory <-> hotbar
                    if (itemStack2.getItem() instanceof BackpackItem) {
                        int playerInvStart = layout.getTotalSlots();
                        int hotbarStart = playerInvStart + 27;
                        int hotbarEnd = playerInvStart + 36;
                        if (index < hotbarStart) {
                            // Inventory -> hotbar
                            if (moveItemStack(itemStack2, hotbarStart, hotbarEnd, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            // Hotbar -> inventory
                            if (moveItemStack(itemStack2, playerInvStart, hotbarStart, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                        return ItemStack.EMPTY;
                    }
                    if (moveItemStack(itemStack2, layout.items().getStartIndex(), layout.items().getEndIndex(), false)) {
                        return ItemStack.EMPTY;
                    }
                    // Next, try transferring to tool slot range, starting at offset 5
                    if (moveItemStack(itemStack2, layout.tools().getStartIndex() + 5, layout.tools().getEndIndex(), false)) {
                        return ItemStack.EMPTY;
                    }
                    // Finally, try transferring to the main tool slots as a last resort
                    if (moveItemStack(itemStack2, layout.tools().getStartIndex(), layout.tools().getEndIndex(), false)) {
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

    private boolean moveItemStack(ItemStack newStack, int startIndex, int endIndex, boolean reverseDirection) {
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
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(newStack, itemstack)) {
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

    @Override
    public boolean stillValid(Player player) {
        switch (type) {
            case ITEM -> {
                ItemStack selectedStack = player.getInventory().getSelected();
                return selectedStack.getItem() instanceof BackpackItem;
            }
            case WORN -> {
                return BackpackHelper.isWearingBackpack(player);
            }
            case BLOCK -> {
                if (container instanceof BlockEntity be) {
                    return !be.isRemoved()
                            && Container.stillValidBlockEntity(be, player, 0);
                }
            }
            case CONTRAPTION -> {
                Entity entity = player.level().getEntity(contraptionId);
                if (!(entity instanceof AbstractContraptionEntity cce)) return false;
                return !cce.isRemoved() && player.distanceToSqr(cce) < 8 * 8;
            }
        }
        return false;
    }

    public boolean hasActiveUpgrade(UpgradeType upgrade) {
        return UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), upgrade);
    }

    public boolean hasUpgrade(UpgradeType upgrade) {
        return UpgradeHelper.hasUpgrade(container.getItemHandler(), upgrade);
    }

    private boolean isToolItem(ItemStack itemStack) {
        // Items that, when shift-clicked, get transferred into tool storage area
        return itemStack.is(Tags.Items.TOOLS)
                || itemStack.is(Items.WARPED_FUNGUS_ON_A_STICK)
                || itemStack.is(Items.CARROT_ON_A_STICK)
                || itemStack.is(Items.ELYTRA)
                || itemStack.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag)
                || itemStack.is(AllItems.CARDBOARD_SWORD)
                || itemStack.is(AllItems.POTATO_CANNON);
    }

    private void toggleUpgrade(int slotId, boolean ctrlKeyDown) {
        Slot slot = this.slots.get(slotId);
        ItemStack itemStack = slot.getItem();

        if (!(itemStack.getItem() instanceof UpgradeItem)) return;
        if (itemStack.getItem().equals(ModItems.BACKPACK_CRAFTING_UPGRADE.get())) return;
        if (itemStack.getItem().equals(ModItems.BACKPACK_WORKSHOP_UPGRADE.get())) return;

        UpgradeType toggledType = UpgradeType.fromItem(itemStack.getItem());
        boolean wasActive = toggledType != null
                && UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), toggledType);

        slot.set(ctrlKeyDown
                ? UpgradeHelper.toggleUpgrade(itemStack)
                : UpgradeHelper.ensureActivated(itemStack)
        );

        if (toggledType == null) return;

        boolean isNowActive = UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), toggledType);
        IUpgrade upgrade = UpgradeRegistry.get(toggledType);
        if (upgrade == null) return;

        UpgradeContext ctx = UpgradeContext.forMenu(this, player, container.getItemHandler(), getBackpackType(), blockPos);

        if (wasActive && !isNowActive) {
            upgrade.onRemoved(ctx);
        } else if (!wasActive && isNowActive) {
            upgrade.onInstalled(ctx);
        }
    }

    public void updateBackpackDataFromContainer() {
        upgradeSync.updateFromSuppliers();
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide) {
            upgradeSync.setLocalIntValue(UpgradeDataSync.Field.WORKSHOP_PROCESSING,
                    WorkshopUpgrade.getSyncValue(container));
        }
        super.broadcastChanges();
    }

    public boolean isUpgradeSettingEnabled(UpgradeDataSync.Field setting) {
        return upgradeSync.getBoolean(setting);
    }

    public int getUpgradeSyncValue(UpgradeDataSync.Field setting) {
        return upgradeSync.getInteger(setting);
    }

    public void initSlots() {
        this.addItemSlots();
        this.addToolSlots();
        this.addUpgradeSlots();

        // Add Upgrade-Specific Slots
        UpgradeContext context = UpgradeContext.forMenu(this, player, container.getItemHandler(), type, blockPos);
        for (UpgradeType upgradeType : UpgradeType.values()) {
            IUpgrade upgrade = UpgradeRegistry.get(upgradeType);
            if (upgrade == null) continue;

            List<Slot> created = upgrade.createSlots(context);
            if (!created.isEmpty()) {
                created.forEach(this::addSlot);
                upgradeSlots.put(upgradeType, List.copyOf(created));
            }
        }

        // Add player inventory and hotbar slots
        this.addPlayerSlots();
    }

    private void addItemSlots() {
        int slot = layout.items().getStartIndex();
        for (int ignored : layout.items().range()) {
            addSlot(new BackpackSlot(container, slot, slot * Util.SLOT_SIZE, 0));
            slot++;
        }
    }

    private void addToolSlots() {
        int slot = layout.tools().getStartIndex();
        for (int ignored : layout.tools().range()) {
            addSlot(new ToolSlot(container, slot, slot * Util.SLOT_SIZE, 0));
            slot++;
        }
    }

    private void addUpgradeSlots() {
        int slot = layout.upgrades().getStartIndex();
        for (int ignored : layout.upgrades().range()) {
            addSlot(new UpgradeSlot(container, slot, slot * Util.SLOT_SIZE, 0, player, this::onUpgradeSlotChanged, this::onUpgradeItemTaken));
            slot++;
        }
    }

    private void addPlayerSlots() {
        Inventory inventory = player.getInventory();
        int xOffset = 61;
        int yOffset = 4 + (Util.SLOT_SIZE * 3);

        // Player Inventory Slots
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, y * 9 + x + 9, xOffset + Util.SLOT_SIZE * x, y * Util.SLOT_SIZE));
            }
        }

        // Hotbar Slots
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, xOffset + i * Util.SLOT_SIZE, yOffset));
        }
    }

    private void onUpgradeItemTaken(ItemStack takenStack) {
        UpgradeType removedType = UpgradeType.fromItem(takenStack.getItem());
        if (removedType != null) {
            if (removedType.hasPanel() && container.isPanelExpanded(removedType)) {
                container.clearPanelExpanded(removedType);
                if (player.level().isClientSide) {
                    upgradeSync.setLocalIntValue(UpgradeDataSync.Field.EXPANDED_PANELS,
                            UpgradeType.toPanelSyncValue(container.getExpandedPanel()));
                }
            }
        }

        onUpgradeSlotChanged();

        if (removedType != null) {
            IUpgrade upgrade = UpgradeRegistry.get(removedType);
            if (upgrade != null) {
                UpgradeContext context = UpgradeContext.forMenu(this, player, container.getItemHandler(), type, blockPos);
                upgrade.onRemoved(context);
            }
        }
    }

    public void setUpgradeSlotListener(Runnable listener) {
        this.upgradeSlotListener = listener;
    }

    private void onUpgradeSlotChanged() {
        if (upgradeSlotListener != null)
            upgradeSlotListener.run();
    }

    public boolean moveStackToPlayerInventory(ItemStack stack) {
        return moveItemStack(stack, layout.getTotalSlots(), layout.getTotalSlots() + 36, true);
    }

    public boolean moveStackToStorageThenPlayer(ItemStack stack) {
        boolean moved = moveItemStack(stack, layout.items().getStartIndex(), layout.items().getEndIndex(), false);
        if (!stack.isEmpty()) {
            moved |= moveStackToPlayerInventory(stack);
        }
        return moved;
    }

    // ======= CRAFTING UPGRADE =======
    public void registerCraftingComponents(BackpackCraftingContainer craftingContainer,
                                           ResultContainer resultContainer, Slot resultSlot) {
        this.craftingContainer = craftingContainer;
        this.craftingResultContainer = resultContainer;
        this.craftingResultSlot = resultSlot;
    }

    @Nullable
    public BackpackCraftingContainer getCraftingContainer() {
        return craftingContainer;
    }

    @Nullable
    public Slot getCraftingResultSlot() {
        return craftingResultSlot;
    }

    public List<Slot> getCraftingMatrixSlots() {
        List<Slot> matrix = new ArrayList<>(BackpackCraftingContainer.WIDTH * BackpackCraftingContainer.HEIGHT);
        for (Slot slot : this.slots) {
            if (slot instanceof CraftingGridSlot) matrix.add(slot);
        }
        matrix.sort(Comparator.comparingInt(Slot::getContainerSlot));
        return matrix;
    }

    public boolean isCraftingPanelReady() {
        return hasActiveUpgrade(UpgradeType.CRAFTING) && isPanelExpanded(UpgradeType.CRAFTING);
    }

    private void craftResultToCarried(Player player) {
        Slot slot = craftingResultSlot;
        if (slot == null || !slot.mayPickup(player)) return;

        ItemStack result = slot.getItem();
        if (result.isEmpty()) return;

        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(carried, result)) return;
            if (carried.getCount() + result.getCount() > carried.getMaxStackSize()) return;
        }

        ItemStack crafted = result.copy();
        if (slot instanceof CraftingResultSlot crs) {
            crs.accountQuickCraft(crafted, crafted.getCount());
        }
        slot.onTake(player, crafted); // consumes ingredients and recomputes the result

        if (carried.isEmpty()) {
            setCarried(crafted);
        } else {
            carried.grow(crafted.getCount());
            setCarried(carried);
        }
    }

    public void onCraftingGridChanged() {
        container.setDataChanged();
        recomputeCraftingResult();
    }

    public void recomputeCraftingResult() {
        if (craftingContainer == null || craftingResultContainer == null || craftingResultSlot == null) return;
        Level level = player.level();
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (level.getServer() == null) return;

        CraftingInput input = craftingContainer.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer()
                .getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            if (craftingResultContainer.setRecipeUsed(level, serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }

        int resultSlotIndex = craftingResultSlot.index;
        craftingResultContainer.setItem(0, result);
        setRemoteSlot(resultSlotIndex, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), resultSlotIndex, result));
    }

    public SortOrder getSortOrder() {
        return container.getSortOrder();
    }

    public void setSortOrder(SortOrder order) {
        container.setSortOrder(order);
    }

    public void setCtrlKeyDown(boolean value) {
        ctrlKeyDown = value;
    }

    public BackpackType getBackpackType() {
        return type;
    }

    private int getSlotsSize() {
        return slots.size();
    }

    protected Slot getPlayerSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + slotIndex);
    }

    protected Slot getHotbarSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + 27 + slotIndex);
    }

    protected int getItemSlotCount() {
        return layout.items().getCount();
    }

    protected int getToolSlotCount() {
        return layout.tools().getCount();
    }

    protected int getTotalSlotCount() {
        return layout.getTotalSlots();
    }

    protected List<Slot> getUpgradeSlots(UpgradeType upgradeType) {
        return upgradeSlots.getOrDefault(upgradeType, List.of());
    }

    public boolean isPanelExpanded(UpgradeType upgradeType) {
        return container.isPanelExpanded(upgradeType);
    }

    public void togglePanelExpanded(UpgradeType upgradeType) {
        container.togglePanelExpanded(upgradeType);
        if (player.level().isClientSide) {
            upgradeSync.setLocalIntValue(UpgradeDataSync.Field.EXPANDED_PANELS,
                    UpgradeType.toPanelSyncValue(container.getExpandedPanel()));
            if (type == BackpackType.BLOCK || type == BackpackType.CONTRAPTION) {
                PacketDistributor.sendToServer(new SetActivePanelPacket(upgradeType));
            }
        }
    }

    public void setUpgradeSetting(UpgradeDataSync.Field setting, boolean value) {
        if (container.getUpgradeSetting(setting) == value) return;
        container.setUpgradeSetting(setting, value);

        if (player.level().isClientSide) {
            upgradeSync.setLocalBoolValue(setting, value);
        } else {
            updateBackpackDataFromContainer();
        }
    }

    public void sortBackpackItems(int startIndex, SortOrder sortOrder) {
        int stackMultiplier = container.getStackMultiplier();
        ServerPlayer sp = (ServerPlayer) player;

        BackpackSlotLayout.SlotSection section = layout.getSectionForSlot(startIndex);

        // Create a map to track all items (with or without Components)
        Map<Util.ItemWithComponent, Integer> itemCompMap = new HashMap<>();

        // Add all items in the container from startIndex to endIndex into the map
        for (int i : section.range()) {
            ItemStack stack = getSlot(i).getItem();
            if (!stack.isEmpty()) {
                DataComponentPatch patch = stack.getComponentsPatch();
                Util.ItemWithComponent key = new Util.ItemWithComponent(stack.getItem(), patch);
                itemCompMap.merge(key, stack.getCount(), Integer::sum);
            }
        }

        // Create a list of entries and sort them
        List<Map.Entry<Util.ItemWithComponent, Integer>> sortedItems = new ArrayList<>(itemCompMap.entrySet());

        switch (sortOrder) {
            case SortOrder.MOD:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithComponent, Integer> entry) -> BuiltInRegistries.ITEM.getKey(entry.getKey().item()).toString())
                        .thenComparing(entry -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));
                break;
            case SortOrder.NAME:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithComponent, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
                        .thenComparing(entry -> entry.getKey().getCustomName()) // Then by custom name
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            default:
                // Default to COUNT
                sortedItems.sort(
                        Map.Entry.<Util.ItemWithComponent, Integer>comparingByValue().reversed()
                                .thenComparing(entry -> entry.getKey().toString())
                );
        }

        NonNullList<ItemStack> compactedList = NonNullList.withSize(section.getCount(), ItemStack.EMPTY);

        // Special handling for tool slots
        if (section.getName().equals("Tools")) {
            List<Class<? extends TieredItem>> toolOrder =
                    List.of(SwordItem.class, PickaxeItem.class, AxeItem.class, ShovelItem.class, HoeItem.class);

            for (int t = 0; t < toolOrder.size(); t++) {
                Class<? extends TieredItem> toolClass = toolOrder.get(t);

                Map.Entry<Util.ItemWithComponent, Integer> best = sortedItems.stream()
                        .filter(e -> toolClass.isInstance(e.getKey().item()))
                        .max(Comparator.comparingInt(e -> getVanillaTierIndex(new ItemStack(e.getKey().item()))))
                        .orElse(null);

                if (best != null) {
                    Util.ItemWithComponent key = best.getKey();
                    DataComponentPatch patch = key.patch();

                    ItemStack stack = new ItemStack(key.item(), 1);
                    if (!patch.isEmpty()) stack.applyComponents(patch);
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
        int idx = (section.getName().equals("Tools") ? 5 : 0);
        for (Map.Entry<Util.ItemWithComponent, Integer> entry : sortedItems) {
            Util.ItemWithComponent key = entry.getKey();
            Item item = key.item();
            DataComponentPatch patch = key.patch();
            int totalCount = entry.getValue();

            ItemStack tempStack = new ItemStack(item, 1);
            int maxStackSize = (section.getName().equals("Items"))
                    ? stackMultiplier * tempStack.getMaxStackSize()
                    : tempStack.getMaxStackSize();

            while (totalCount > 0 && idx < compactedList.size()) {
                int stackSize = Math.min(totalCount, maxStackSize);
                ItemStack stack = new ItemStack(item, stackSize);
                if (!patch.isEmpty()) stack.applyComponents(patch);
                compactedList.set(idx++, stack);
                totalCount -= stackSize;
            }

            // fallback: backfill empty reserved slots
            if (totalCount > 0 && startIndex == layout.tools().getStartIndex()) {
                for (int t = 0; t < 5 && totalCount > 0; t++) {
                    if (compactedList.get(t).isEmpty()) {
                        int stackSize = Math.min(totalCount, maxStackSize);
                        ItemStack stack = new ItemStack(item, stackSize);
                        if (!patch.isEmpty()) stack.applyComponents(patch);
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

            if (startIndex > layout.getTotalSlots()) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(
                        player.containerMenu.containerId, getStateId(), i + startIndex, stack));
            }
        }
    }

    private static int getVanillaTierIndex(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof TieredItem tiered) {
            Tier tier = tiered.getTier();

            return switch (tier) {
                case Tiers.WOOD -> 0;
                case Tiers.STONE -> 1;
                case Tiers.IRON -> 2;
                case Tiers.GOLD -> 3;
                case Tiers.DIAMOND -> 4;
                case Tiers.NETHERITE -> 5;
                default -> -1;
            };
        }
        return -1;
    }

    public void toggleUpgradeSetting(UpgradeDataSync.Field setting) {
        if (!player.level().isClientSide) return;

        boolean current = isUpgradeSettingEnabled(setting);
        setUpgradeSetting(setting, !current);
        container.saveSettings();
    }

    public void setStateId(int stateId) {
        this.stateId = stateId;
    }

    public int getContraptionId() {
        return contraptionId;
    }

    public void setContraptionId(int id) {
        this.contraptionId = id;
    }
}
