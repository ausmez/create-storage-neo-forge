package net.fxnt.fxntstorage.container.mounted;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.SetMountedStorageDirtyPacket;
import net.fxnt.fxntstorage.network.SetSortOrderPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class StorageBoxMountedMenu extends AbstractContainerMenu {
    private final Container container;
    private final CompoundTag nbt;
    private final Player player;
    private final int contraptionId;
    private final BlockPos localPos;
    private final int slotCount;

    public StorageBoxMountedMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
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

        ContainerData data = new SimpleContainerData(container.getContainerSize());
        this.slotCount = data.getCount();

        // Add Container Slots
        int index = 0;
        for (int i = 0; i < this.slotCount; i++) {
            this.addSlot(new Slot(container, index, index * Util.SLOT_SIZE, 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
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

    public SortOrder getSortOrder() {
        return SortOrder.valueOf(nbt.getString("SortOrder"));
    }

    public void setSortOrder(SortOrder order) {
        nbt.putString("SortOrder", order.name());
        if (player.level().isClientSide) {
            ModNetwork.sendToServer(new SetSortOrderPacket(getSortOrder()));
        } else {
            updateContraptionNbt(tag -> tag.putString("SortOrder", order.name()));
        }
    }

    public int getSlotsSize() {
        return slots.size();
    }

    public int getContainerSize() {
        return this.slotCount;
    }

    public Slot getPlayerSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + slotIndex);
    }

    public Slot getHotbarSlot(int slotIndex) {
        return slots.get(getSlotsSize() - 36 + 27 + slotIndex);
    }

    public boolean filterTest(ItemStack stack) {
        ItemStack filterItem = ItemStack.of(nbt.getCompound("Filter"));
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

        switch (sortOrder) {
            case MOD:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(entry.getKey().item())).toString())  // Sort by registry name (ascending)
                        .thenComparing(Map.Entry::getValue, Comparator.reverseOrder()));  // Then sort by count (descending)
                break;
            case NAME:
                sortedItems.sort(Comparator
                        .comparing((Map.Entry<Util.ItemWithNBT, Integer> entry) -> entry.getKey().item().getName(new ItemStack(entry.getKey().item())).getString())  // Sort by item name (ascending)
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

            int maxStackSize = new ItemStack(item, 1).getMaxStackSize();

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

            if (startIndex >= this.slotCount) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(player.containerMenu.containerId, getStateId(), i + startIndex, stack));
            }
        }
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
        ((StorageBoxMountedStorage) storage).setSortOrder(SortOrder.valueOf(tag.getString("SortOrder")));
//        contraption.deferInvalidate = true;
    }

    private void setStorageDirty() {
        ModNetwork.sendToServer(new SetMountedStorageDirtyPacket(contraptionId, localPos));
    }

}
