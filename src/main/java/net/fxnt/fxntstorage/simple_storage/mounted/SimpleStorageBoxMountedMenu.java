package net.fxnt.fxntstorage.simple_storage.mounted;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.network.packet.SetMountedStorageDirtyPacket;
import net.fxnt.fxntstorage.simple_storage.ISimpleStorageBoxMenu;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.function.Consumer;

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

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getStoredAmount();
            }

            @Override
            public void set(int i) {
                setStoredAmount(i);
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getMaxItemCapacity();
            }

            @Override
            public void set(int i) {
                setMaxItemCapacity(i);
            }
        });

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return getVoidUpgrade() ? 1 : 0;
            }

            @Override
            public void set(int i) {
                setVoidUpgrade(i);
            }
        });

        // Add Void slot
        this.addSlot(new Slot(container, VOID_UPGRADE_SLOT, 8, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (this.hasItem()) return false;
                return stack.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Add Capacity Slots
        for (int i = 0; i < MAX_CAPACITY_UPGRADES; ++i) {
            int slot = i + CAPACITY_UPGRADE_SLOT_START;
            int y = 58;
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
        int yOffset = 94;
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
        int playerStartSlot = 1 + MAX_CAPACITY_UPGRADES;
        if (slotId >= 0 && slotId < playerStartSlot) {
            ItemStack itemStack = this.slots.get(slotId).getItem();
            if (itemStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                // Calculate new capacity
                int upgrades = this.getCapacityUpgrades();
                if (upgrades > 0) {
                    int storedAmount = this.getStoredAmount();
                    int stackSize = ITEM_STACK_SIZE;
                    if (!this.container.getItem(0).isEmpty()) {
                        stackSize = this.container.getItem(0).getMaxStackSize();
                    }
                    int capacityCheck = BASE_CAPACITY;
                    for (int i = 0; i < upgrades - 1; i++) {
                        capacityCheck *= 2;
                    }
                    capacityCheck = capacityCheck * stackSize;
                    if (capacityCheck < storedAmount) {
                        return;
                    }
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    private int getCapacityUpgrades() {
        int count = 0;
        for (int i = 1; i < MAX_CAPACITY_UPGRADES + 1; ++i) {
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
        int playerStartSlot = 1 + MAX_CAPACITY_UPGRADES;

        // If click player slot, if upgrade then move to upgrade slot, otherwise, don't allow inserting items
        if (index < playerStartSlot) {
            // Clicked on upgrade slot
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
                // Move to void slot
                if (!this.slots.getFirst().hasItem()) {
                    this.slots.getFirst().set(slotStack.copyWithCount(1));
                    slotStack.shrink(1);
                    this.container.setChanged();
                    player.getInventory().setChanged();
                    return slotStack;
                }
            } else if (slotStack.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                // Move to upgrade slot
                for (int i = 1; i <= MAX_CAPACITY_UPGRADES; i++) {
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

    public void setFilterItem(ItemStack stack) {
        this.nbt.put("FilterItem", stack.copyWithCount(1).save(player.registryAccess()));
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

    public void setStoredAmount(int value) {
        nbt.putInt("StoredAmount", value);
        updateContraptionNbt(tag -> tag.putInt("StoredAmount", value));
    }

    public int getMaxItemCapacity() {
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

    public void setMaxItemCapacity(int value) {
        nbt.putInt("MaxItemCapacity", value);
        updateContraptionNbt(tag -> tag.putInt("MaxItemCapacity", value));
    }

    public boolean getVoidUpgrade() {
        return !container.getItem(VOID_UPGRADE_SLOT).isEmpty();
    }

    public void setVoidUpgrade(int value) {
        nbt.putBoolean("VoidUpgrade", value != 0);
        updateContraptionNbt(tag -> tag.putBoolean("VoidUpgrade", value != 0));
    }

    public Container getContainer() {
        return this.container;
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
//        contraption.resetClientContraption();
    }

    private void setStorageDirty() {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(new SetMountedStorageDirtyPacket(contraptionId, localPos));
        }
    }
}
