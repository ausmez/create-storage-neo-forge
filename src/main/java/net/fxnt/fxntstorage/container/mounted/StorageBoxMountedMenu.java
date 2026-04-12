package net.fxnt.fxntstorage.container.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.container.ISortableStorageBox;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.network.packet.SetMountedStorageDirtyPacket;
import net.fxnt.fxntstorage.network.packet.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class StorageBoxMountedMenu extends AbstractContainerMenu implements ISortableStorageBox {
    private static final String TAG_SORT_ORDER = "SortOrder";
    private static final String TAG_FILTER = "Filter";

    private final Container container;
    private final CompoundTag nbt;
    private final Player player;
    private final int contraptionId;
    private final BlockPos localPos;
    private final int slotCount;

    public StorageBoxMountedMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(buf.readInt()), buf.readInt(), buf.readBlockPos(), buf.readNbt());
    }

    public Container getInventory() {
        return this.container;
    }

    public StorageBoxMountedMenu(int containerId, Inventory playerInventory, Container container, int contraptionId, BlockPos localPos, CompoundTag nbt) {
        super(ModMenuTypes.STORAGE_BOX_MOUNTED_MENU.get(), containerId);
        this.nbt = nbt;
        this.container = container;
        this.player = playerInventory.player;
        this.contraptionId = contraptionId;
        this.localPos = localPos;

        this.slotCount = container.getContainerSize();

        // Add Container Slots
        int index = 0;
        for (int i = 0; i < this.slotCount; i++) {
            this.addSlot(new Slot(container, index, index * Util.SLOT_SIZE, 0) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return filterTest(stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    setStorageDirty();
                }
            });
            index++;
        }

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
    public boolean stillValid(@NotNull Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    public @NotNull SortOrder getSortOrder() {
        return SortOrder.valueOf(nbt.getString(TAG_SORT_ORDER));
    }

    public void setSortOrder(@NotNull SortOrder order) {
        nbt.putString(TAG_SORT_ORDER, order.name());
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new SetSortOrderPacket(getSortOrder()));
        } else {
            updateContraptionNbt(tag -> tag.putString(TAG_SORT_ORDER, order.name()));
        }
    }

    public int getSlotsSize() {
        return slots.size();
    }

    public int getContainerSize() {
        return this.slotCount;
    }

    public @NotNull Slot getPlayerSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + slotIndex);
    }

    public @NotNull Slot getHotbarSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + 27 + slotIndex);
    }

    public boolean filterTest(ItemStack stack) {
        ItemStack filterItem = ItemStack.parseOptional(player.registryAccess(), nbt.getCompound(TAG_FILTER));
        return FilterItemStack.of(filterItem).test(player.level(), stack);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int pIndex) {
        Slot srcSlot = slots.get(pIndex);
        if (!srcSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack srcStack = srcSlot.getItem();
        if (!filterTest(srcStack)) return ItemStack.EMPTY;

        ItemStack copyOfSrcStack = srcStack.copy();

        if (pIndex < container.getContainerSize()) {
            if (!this.moveItemStackTo(srcStack, container.getContainerSize(), container.getContainerSize() + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(srcStack, 0, container.getContainerSize(), false)) {
            return ItemStack.EMPTY;
        }
        if (srcStack.isEmpty()) {
            srcSlot.set(ItemStack.EMPTY);
        } else {
            srcSlot.setChanged();
        }
        return copyOfSrcStack;
    }

    public void sortStorageItems(int startIndex, int endIndex, SortOrder sortOrder) {
        Util.sortStorageItems(this, (ServerPlayer) player, startIndex, endIndex, sortOrder, this.slotCount);
    }

    private void updateContraptionNbt(Consumer<CompoundTag> editor) {
        Entity entity = this.player.level().getEntity(contraptionId);
        if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) return;

        var contraption = contraptionEntity.getContraption();
        var info = contraption.getBlocks().get(localPos);
        if (info == null) return;

        CompoundTag tag = info.nbt();
        editor.accept(tag);

        contraption.getBlocks().put(localPos, new StructureTemplate.StructureBlockInfo(
                info.pos(), info.state(), tag
        ));
        MountedItemStorage storage = contraption.getStorage().getMountedItems().storages.get(localPos);
        if (storage != null && tag != null) {
            ((StorageBoxMountedStorage) storage).setSortOrder(SortOrder.valueOf(tag.getString(TAG_SORT_ORDER)));
        }
    }

    private void setStorageDirty() {
        PacketDistributor.sendToServer(new SetMountedStorageDirtyPacket(contraptionId, localPos));
    }
}
