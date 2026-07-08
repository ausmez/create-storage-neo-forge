package net.fxnt.fxntstorage.simple_storage.mounted;

import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.network.packet.SetMountedStorageDirtyPacket;
import net.fxnt.fxntstorage.simple_storage.CompactingChain;
import net.fxnt.fxntstorage.simple_storage.CompactingRecipeHelper;
import net.fxnt.fxntstorage.simple_storage.ISimpleStorageBoxMenu;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.*;

public class SimpleStorageBoxMountedMenu extends AbstractContainerMenu implements ISimpleStorageBoxMenu {
    private final Container container;
    private final CompoundTag nbt;
    private final Player player;
    private final int contraptionId;
    private final BlockPos localPos;

    public SimpleStorageBoxMountedMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(SLOT_COUNT), buf.readInt(), buf.readBlockPos(), Objects.requireNonNull(buf.readNbt()));
    }

    public SimpleStorageBoxMountedMenu(int containerId, Inventory playerInventory, Container container, int contraptionId, BlockPos localPos, CompoundTag nbt) {
        super(ModMenuTypes.SIMPLE_STORAGE_BOX_MOUNTED_MENU.get(), containerId);
        this.nbt = nbt;
        this.container = container;
        this.player = playerInventory.player;
        this.contraptionId = contraptionId;
        this.localPos = localPos;
        container.startOpen(playerInventory.player);

        // Add Void slot (lower)
        this.addSlot(new Slot(container, VOID_UPGRADE_SLOT, 8, 20 + Util.SLOT_SIZE) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (this.hasItem()) return false;
                return stack.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                setStorageDirty();
            }
        });

        // Add Compacting slot (upper)
        this.addSlot(new Slot(container, COMPACTING_UPGRADE_SLOT, 8, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (this.hasItem()) return false;
                return stack.is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                setStorageDirty();
            }
        });

        // Add Capacity Slots
        for (int i = 0; i < MAX_CAPACITY_UPGRADES; ++i) {
            int slot = i + CAPACITY_UPGRADE_SLOT_START;
            int y = 60;
            int x = 8;
            this.addSlot(new Slot(container, slot, x + (Util.SLOT_SIZE * i), y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (this.hasItem()) return false;
                    return stack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setStorageDirty();
                }
            });
        }

        // Add Inventory Slots
        int xOffset = 8;
        int yOffset = 96;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInventory, y * 9 + x + 9, xOffset + Util.SLOT_SIZE * x, yOffset + y * Util.SLOT_SIZE));
            }
        }
        // Add Hot bar Slots
        yOffset = yOffset + (Util.SLOT_SIZE * 3) + 4;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, xOffset + i * Util.SLOT_SIZE, yOffset));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int playerStartSlot = 2 + MAX_CAPACITY_UPGRADES;
        if (slotId >= 0 && slotId < playerStartSlot) {
            ItemStack itemStack = this.slots.get(slotId).getItem();
            if (itemStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get()) && !canRemoveCapacityUpgrade()) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    // Internal T0 capacity for a given upgrade count (includes the compacting multiplier).
    private int capacityForUpgrades(int upgrades) {
        int stackSize = getFilterItem().isEmpty() ? ITEM_STACK_SIZE : getFilterItem().getMaxStackSize();
        return (BASE_CAPACITY << upgrades) * stackSize * compactingMultiplier();
    }

    // A capacity upgrade may only be removed if the contents still fit in the reduced capacity
    // (compared in internal T0 units, so the compacting multiplier cancels). Server-authoritative.
    private boolean canRemoveCapacityUpgrade() {
        int upgrades = getCapacityUpgrades();
        if (upgrades <= 0) return true;
        return getStoredAmount() <= capacityForUpgrades(upgrades - 1);
    }

    private int getCapacityUpgrades() {
        int count = 0;
        for (int i = 2; i < MAX_CAPACITY_UPGRADES + 2; ++i) {
            ItemStack stack = this.slots.get(i).getItem();
            if (!stack.isEmpty() && stack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                ++count;
            }
        }
        return count;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack slotStack = this.slots.get(index).getItem();
        int playerStartSlot = 2 + MAX_CAPACITY_UPGRADES;

        // If click player slot, if upgrade then move to upgrade slot, otherwise, don't allow inserting items
        if (index < playerStartSlot) {
            // Clicked on upgrade slot
            if (slotStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get()) && !canRemoveCapacityUpgrade()) {
                return ItemStack.EMPTY;
            }
            int playerSlot = player.getInventory().getSlotWithRemainingSpace(slotStack);
            if (playerSlot == -1) {
                playerSlot = player.getInventory().getFreeSlot();
            }
            if (playerSlot > -1) {
                ItemStack playerStack = player.getInventory().getItem(playerSlot);
                if (playerStack.isEmpty()) {
                    player.getInventory().setItem(playerSlot, slotStack.copyWithCount(1));
                } else {
                    playerStack.grow(1);
                }
                slotStack.shrink(1);
                this.container.setChanged();
                player.getInventory().setChanged();
                setStorageDirty();
                return ItemStack.EMPTY;
            }
        } else {
            // Clicked Player Slot
            if (slotStack.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())) {
                // Move to the void slot (menu index 0)
                if (!this.slots.getFirst().hasItem()) {
                    this.slots.getFirst().set(slotStack.copyWithCount(1));
                    slotStack.shrink(1);
                    this.container.setChanged();
                    player.getInventory().setChanged();
                    setStorageDirty();
                    return slotStack;
                }
            } else if (slotStack.is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE.get())) {
                // Move to the compacting slot (menu index 1)
                if (!this.slots.get(1).hasItem()) {
                    this.slots.get(1).set(slotStack.copyWithCount(1));
                    slotStack.shrink(1);
                    this.container.setChanged();
                    player.getInventory().setChanged();
                    setStorageDirty();
                    return slotStack;
                }
            } else if (slotStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                // Move to a capacity slot (menu indices 2..2+MAX_CAPACITY_UPGRADES)
                for (int i = 2; i < 2 + MAX_CAPACITY_UPGRADES; i++) {
                    if (!this.slots.get(i).hasItem()) {
                        this.slots.get(i).set(slotStack.copyWithCount(1));
                        slotStack.shrink(1);
                        this.container.setChanged();
                        player.getInventory().setChanged();
                        return slotStack;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos getLocalPos() {
        return this.localPos;
    }

    public void applySyncedData(CompoundTag source) {
        this.nbt.putInt("StoredAmount", source.getInt("StoredAmount"));
        this.nbt.putInt("MaxItemCapacity", source.getInt("MaxItemCapacity"));
        this.nbt.putBoolean("CompactingUpgrade", source.getBoolean("CompactingUpgrade"));
        if (source.contains("FilterItem", Tag.TAG_COMPOUND)) {
            this.nbt.put("FilterItem", source.getCompound("FilterItem"));
        }
    }

    public ItemStack getFilterItem() {
        CompoundTag filterTag = this.nbt.getCompound("FilterItem");
        if (filterTag.isEmpty() || !filterTag.contains("id", Tag.TAG_STRING)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(player.registryAccess(), filterTag).orElse(ItemStack.EMPTY);
    }

    public int getStoredAmount() {
        if (player.level().isClientSide) {
            return nbt.getInt("StoredAmount");
        }
        return container.getItem(0).getCount();
    }

    // Capacity of a plain (non-compacting) box, measured in filter-item units.
    private int getBaseItemCapacity() {
        int upgradeCount = 0;

        for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
            if (container.getItem(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                upgradeCount++;
            }
        }

        int maxCapacity = BASE_CAPACITY << upgradeCount; // Multiply by 2^upgradeCount
        int stackSize = getFilterItem().isEmpty() ? ITEM_STACK_SIZE : getFilterItem().getMaxStackSize();

        return maxCapacity * stackSize;
    }

    public int getMaxItemCapacity() {
        // Server computes from the live container; client reads the value synced into nbt
        // (the client-side container is empty), mirroring getStoredAmount().
        if (player.level().isClientSide) {
            return nbt.getInt("MaxItemCapacity");
        }
        return getBaseItemCapacity() * compactingMultiplier();
    }

    private boolean isCompacting() {
        if (player.level().isClientSide) {
            return nbt.getBoolean("CompactingUpgrade");
        }
        return container.getItem(COMPACTING_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_COMPACTING_UPGRADE.get());
    }

    // How many T0 items make up one highest-tier unit when compacting, else 1.
    private int compactingMultiplier() {
        if (!isCompacting()) return 1;
        ItemStack filter = getFilterItem();
        if (filter.isEmpty()) return 1;
        if (CompactingRecipeHelper.isEmpty()) {
            CompactingRecipeHelper.rebuild(player.level().getRecipeManager(), player.level().registryAccess());
        }
        CompactingChain chain = CompactingRecipeHelper.buildChain(filter.getItem());
        return chain != null ? chain.highestTierT0PerUnit() : 1;
    }

    private @Nullable CompactingChain compactingChain() {
        ItemStack filter = getFilterItem();
        if (!isCompacting() || filter.isEmpty()) return null;
        if (CompactingRecipeHelper.isEmpty()) {
            CompactingRecipeHelper.rebuild(player.level().getRecipeManager(), player.level().registryAccess());
        }
        return CompactingRecipeHelper.buildChain(filter.getItem());
    }

    @Override
    public int getDisplayedStoredAmount() {
        int mult = compactingMultiplier();
        return mult > 1 ? getStoredAmount() / mult : getStoredAmount();
    }

    @Override
    public int getDisplayedMaxCapacity() {
        int mult = compactingMultiplier();
        // Scale the (synced) internal T0 capacity back into highest-tier units.
        return mult > 1 ? getMaxItemCapacity() / mult : getMaxItemCapacity();
    }

    @Override
    public ItemStack getDisplayedItem() {
        CompactingChain chain = compactingChain();
        return chain != null ? chain.itemForSlot(0) : getFilterItem();
    }

    public boolean getVoidUpgrade() {
        return container.getItem(VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
    }

    public Container getContainer() {
        return this.container;
    }

    private void setStorageDirty() {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new SetMountedStorageDirtyPacket(contraptionId, localPos));
        }
    }
}
