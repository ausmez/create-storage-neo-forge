package net.fxnt.fxntstorage.reserve_storage;

import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.network.packet.SetMountedStorageDirtyPacket;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.GHOST_SLOTS;
import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.STORAGE_SLOTS;

public class ReserveStorageBoxMenu extends AbstractContainerMenu {
    public static final int STORAGE_SLOT_COUNT = STORAGE_SLOTS;
    public static final int TOTAL_CONTAINER_SLOTS = STORAGE_SLOTS + GHOST_SLOTS;
    private static final int PLAYER_INV_SLOTS = 36;

    private SortOrder sortOrder = SortOrder.COUNT;

    @Nullable
    public final ReserveStorageBoxEntity blockEntity;
    public final Container container;

    public final boolean isMounted;
    public final int contraptionId; // -1 when not mounted
    @Nullable
    public final BlockPos localPos; // null when not mounted

    public final Player player;

    public ReserveStorageBoxMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.RESERVE_STORAGE_BOX_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.isMounted = buf.readBoolean();

        if (isMounted) {
            int size = buf.readInt();
            SimpleContainer sc = new SimpleContainer(size) {
                @Override
                public int getMaxStackSize() {
                    return ReserveStorageBoxGhostSlot.MAX_COUNT;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return ReserveStorageBoxGhostSlot.MAX_COUNT;
                }
            };
            for (int i = STORAGE_SLOTS; i < TOTAL_CONTAINER_SLOTS; i++) {
                sc.setItem(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
            this.container = sc;
            this.contraptionId = buf.readInt();
            this.localPos = buf.readBlockPos();
            this.blockEntity = null;
        } else {
            BlockPos pos = buf.readBlockPos();
            this.blockEntity = (ReserveStorageBoxEntity) playerInventory.player.level().getBlockEntity(pos);
            this.container = this.blockEntity;
            this.contraptionId = -1;
            this.localPos = null;
        }

        initSlots(playerInventory);
    }

    public ReserveStorageBoxMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.RESERVE_STORAGE_BOX_MENU.get(), containerId);
        this.blockEntity = (ReserveStorageBoxEntity) blockEntity;
        this.container = this.blockEntity;
        this.player = playerInventory.player;
        this.isMounted = false;
        this.contraptionId = -1;
        this.localPos = null;
        checkContainerSize(container, ReserveStorageBoxEntity.getSlotCount());
        initSlots(playerInventory);
    }

    public ReserveStorageBoxMenu(int containerId, Inventory playerInventory,
                                 Container container, int contraptionId, BlockPos localPos) {
        super(ModMenuTypes.RESERVE_STORAGE_BOX_MENU.get(), containerId);
        this.blockEntity = null;
        this.container = container;
        this.player = playerInventory.player;
        this.isMounted = true;
        this.contraptionId = contraptionId;
        this.localPos = localPos;
        container.startOpen(playerInventory.player);
        initSlots(playerInventory);
    }

    private void initSlots(Inventory playerInventory) {
        if (isMounted) {
            initMountedSlots();
        } else {
            initBlockEntitySlots();
        }
        addPlayerSlots(playerInventory);
    }

    private void initBlockEntitySlots() {
        IItemHandler handler = blockEntity.getItemHandler();
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            addSlot(new SlotItemHandler(handler, i, i * Util.SLOT_SIZE, 0));
        }
        for (int i = STORAGE_SLOTS; i < TOTAL_CONTAINER_SLOTS; i++) {
            addSlot(new ReserveStorageBoxGhostSlot(handler, i, i * Util.SLOT_SIZE, 0));
        }
    }

    private void initMountedSlots() {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            addSlot(new Slot(container, i, i * Util.SLOT_SIZE, 0) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    markStorageDirty();
                }
            });
        }
        for (int i = STORAGE_SLOTS; i < TOTAL_CONTAINER_SLOTS; i++) {
            addSlot(new ReserveStorageBoxGhostSlot(container, i, i * Util.SLOT_SIZE, 0));
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(playerInventory, y * 9 + x + 9, x * Util.SLOT_SIZE, y * Util.SLOT_SIZE));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, i * Util.SLOT_SIZE, 0));
        }
    }

    public boolean isGhostDuplicate(int targetSlot, ItemStack item) {
        if (item.isEmpty()) return false;
        for (int i = STORAGE_SLOTS; i < TOTAL_CONTAINER_SLOTS; i++) {
            if (i == targetSlot) continue;
            if (ItemStack.isSameItem(container.getItem(i), item)) return true;
        }
        return false;
    }

    public void setGhostItem(int slot, ItemStack item) {
        container.setItem(slot, item);
    }

    public SortOrder getSortOrder() {
        return blockEntity != null ? blockEntity.getSortOrder() : sortOrder;
    }

    public void setSortOrder(SortOrder order) {
        if (blockEntity != null) {
            blockEntity.setSortOrder(order); // updates client field and notifies the server
        } else {
            sortOrder = order;
            if (player.level().isClientSide) {
                PacketDistributor.sendToServer(new SetSortOrderPacket(order));
            }
        }
    }

    public void applySortOrder(SortOrder order) {
        if (blockEntity != null) {
            blockEntity.setSortOrder(order);
        } else {
            sortOrder = order;
        }
    }

    public void sortStorageItems(int startIndex, int endIndex, SortOrder order) {
        Util.sortStorageItems(this, (ServerPlayer) player, startIndex, endIndex, order, STORAGE_SLOTS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot srcSlot = slots.get(index);
        if (!srcSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack srcStack = srcSlot.getItem();
        ItemStack copy = srcStack.copy();

        if (index < TOTAL_CONTAINER_SLOTS) {
            // Container -> player inventory
            if (!moveItemStackTo(srcStack, TOTAL_CONTAINER_SLOTS, TOTAL_CONTAINER_SLOTS + PLAYER_INV_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player -> storage slots only
            if (!moveItemStackTo(srcStack, 0, STORAGE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (srcStack.isEmpty()) {
            srcSlot.set(ItemStack.EMPTY);
        } else {
            srcSlot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (isMounted) {
            container.stopOpen(player);
        }
    }

    private void markStorageDirty() {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new SetMountedStorageDirtyPacket(contraptionId, localPos));
        }
    }
}
