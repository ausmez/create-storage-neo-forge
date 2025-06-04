package net.fxnt.fxntstorage.simple_storage.mounted;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.menu.StorageInteractionWrapper;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.CreateCodecs;
import com.simibubi.create.foundation.utility.CreateLang;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.SyncMountedStoragePacket;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.*;

public class SimpleStorageBoxMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> {
    public static final Codec<SimpleStorageBoxMountedStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER.xmap(
            SimpleStorageBoxMountedStorage::new, storage -> storage.wrapped
    );

    private final Set<Item> storageFilters = new HashSet<>();
    private static final Map<Contraption, Set<Item>> contraptionFilters = new HashMap<>();
    public boolean initialized = false;
    private boolean dirty = false;

    private ItemStack filterItem = null;

    protected SimpleStorageBoxMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
        super(type, handler);
    }

    protected SimpleStorageBoxMountedStorage(ItemStackHandler handler) {
        this(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED.get(), handler);
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        ItemStack itemInHand = player.getMainHandItem();

        if (!itemInHand.isEmpty()) {
            if (itemInHand.getItem().equals(this.filterItem.getItem()) || this.filterItem.isEmpty()) {
                if (!itemInHand.is(AllTags.AllItemTags.WRENCH.tag)) {
                    ItemStack remain = this.wrapped.insertItem(1, itemInHand, false);
                    this.moveItems();
                    player.setItemInHand(InteractionHand.MAIN_HAND, remain);
                }
            } else if (itemInHand.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {
                if (itemInHand.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())) {
                    if (!this.hasVoidUpgrade()) {
                        this.wrapped.setStackInSlot(VOID_UPGRADE_SLOT, itemInHand.copyWithCount(1));
                        if (!player.isCreative()) {
                            itemInHand.shrink(1);
                            player.getInventory().setChanged();
                        }
                    } else {
                        ItemStack voidStack = this.wrapped.getStackInSlot(VOID_UPGRADE_SLOT);
                        int slot = player.getInventory().getSlotWithRemainingSpace(voidStack);
                        if (slot > -1) {
                            player.getInventory().getItem(slot).grow(1);
                            player.getInventory().setChanged();
                        } else {
                            slot = player.getInventory().getFreeSlot();
                            if (slot > -1) {
                                player.getInventory().setItem(slot, voidStack);
                                player.getInventory().setChanged();
                            } else {
                                player.drop(voidStack, false);
                            }
                        }
                        this.wrapped.setStackInSlot(VOID_UPGRADE_SLOT, ItemStack.EMPTY);
                    }
                } else if (itemInHand.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                    boolean canAddUpgrade = false;
                    for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
                        if (this.wrapped.getStackInSlot(i).isEmpty()) {
                            this.wrapped.setStackInSlot(i, itemInHand.copyWithCount(1));
                            canAddUpgrade = true;
                            break;
                        }
                    }
                    if (!player.isCreative() && canAddUpgrade) {
                        itemInHand.shrink(1);
                        player.getInventory().setChanged();
                    } else if (!canAddUpgrade) {
                        player.displayClientMessage(Component.translatable("fxntstorage.storage_box_capacity_upgrade_max"), true);
                    }
                }
            }
            markDirty();
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos localPos = info.pos();
        Vec3 localPosVec = Vec3.atCenterOf(localPos);
        Predicate<Player> stillValid = p -> {
            Vec3 currentPos = contraption.entity.toGlobalVector(localPosVec, 0);
            return this.isMenuValid(player, contraption, currentPos);
        };
        Component blockName = Component.translatable("container.fxntstorage.simple_storage_box_title");
        Component menuName = CreateLang.translateDirect("contraptions.moving_container", blockName);
        Consumer<Player> onClose = p -> {
            Vec3 newPos = contraption.entity.toGlobalVector(localPosVec, 0);
            this.playClosingSound(level, newPos);
        };

        NetworkHooks.openScreen(
                player,
                this.createMenu(
                        menuName, this.wrapped, stillValid, onClose, contraption.entity.getId(), localPos, info.nbt()
                ), buf -> {
                    buf.writeInt(contraption.entity.getId());
                    buf.writeBlockPos(localPos);
                    buf.writeNbt(info.nbt());
                }
        );
        return true; // Do we just assume it opened?
    }

    private boolean hasVoidUpgrade() {
        return this.wrapped.getStackInSlot(VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.asItem());
    }

    private int getMaxItemCapacity() {
        int upgradeCount = 0;

        for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
            if (this.wrapped.getStackInSlot(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                ++upgradeCount;
            }
        }

        int maxCapacity = BASE_CAPACITY << upgradeCount; // Multiply by 2^upgradeCount
        int stackSize = filterItem.isEmpty() ? ITEM_STACK_SIZE : filterItem.getMaxStackSize();

        return maxCapacity * stackSize;
    }

    protected @Nullable MenuProvider createMenu(Component name, IItemHandlerModifiable handler, Predicate<Player> stillValid, Consumer<Player> onClose, int contraptionId, BlockPos localPos, CompoundTag nbt) {
        Container wrapper = new StorageInteractionWrapper(handler, stillValid, onClose);
        return new SimpleMenuProvider(
                (id, inv, player) -> new SimpleStorageBoxMountedMenu(id, inv, wrapper, contraptionId, localPos, nbt),
                name
        );
    }

    public static @NotNull SimpleStorageBoxMountedStorage fromStorage(SimpleStorageBoxEntity simpleStorageBox) {
        return new SimpleStorageBoxMountedStorage(copyToItemStackHandler(simpleStorageBox.getItemHandler()));
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof SimpleStorageBoxEntity simpleStorageBox) {
            simpleStorageBox.applyInventoryToBlock(this.wrapped);
        }

        if (level.isClientSide) return;

        if (level.getBlockEntity(pos) == null && wrapped.getSlots() > 0) {
            removeFiltersForContraptionFromLevel(level);
        }
    }

    private void removeFiltersForContraptionFromLevel(Level level) {
        contraptionFilters.keySet().removeIf(entity -> entity.entity.level() == level && entity.entity.isRemoved());
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot != 1 && slot != 2)
            return stack;
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        if (!isItemValid(slot, stack))
            return stack;

        ItemStack existing = this.wrapped.getStackInSlot(slot);

        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack;
        } else {
            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
                if (existing.isEmpty()) {
                    this.wrapped.setStackInSlot(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                } else {
                    existing.grow(reachedLimit ? limit : stack.getCount());
                }
            }
            if (filterItem.isEmpty()) filterItem = stack.copyWithCount(1);
            this.moveItems();

            this.dirty = true;

            return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
        }
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.extractItem(slot, amount, simulate);
        markDirty();
        return stack;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (!storageFilters.isEmpty() && storageFilters.contains(stack.getItem()) && filterItem.isEmpty())
            return false;

        if (filterTest(stack)) {
            boolean voidUpgrade = hasVoidUpgrade();
            if (slot > 1 && !voidUpgrade)
                return false;
            if (voidUpgrade)
                return true;
            return !(getPercent() >= 100);
        }

        return false;
    }

    private float getPercent() {
        return ((float) (wrapped.getStackInSlot(0).getCount() + wrapped.getStackInSlot(1).getCount()) / getMaxItemCapacity()) * 100;
    }

    private void moveItems() {
        ItemStack slot0 = wrapped.getStackInSlot(0);
        ItemStack slot1 = wrapped.getStackInSlot(1);

        if (getPercent() == 100 && !hasVoidUpgrade()) return;

        // If full & using void upgrade then items go into slot 2 (delete them all!)
        if (!wrapped.getStackInSlot(2).isEmpty()) {
            wrapped.setStackInSlot(2, ItemStack.EMPTY);
        }

        // Incoming items are placed into slot 1
        // Move items from slot 1 to slot 0 (slot 0 is bulk storage)
        if (slot1.isEmpty()) return;

        int slot1Amount = slot1.getCount();

        // If no items in slot 0, then add
        if (slot0.isEmpty()) {
            wrapped.setStackInSlot(0, slot1.copy());
            wrapped.setStackInSlot(1, ItemStack.EMPTY);
        } else {
            // Always move items from slot 1 to 0 if space available
            int slot0FreeSpace = getMaxItemCapacity() - filterItem.getMaxStackSize() - slot0.getCount();
            int amountToMove = Math.min(slot1Amount, slot0FreeSpace);
            slot0.grow(amountToMove);
            slot1.shrink(amountToMove);
        }
    }

    public boolean filterTest(ItemStack stack) {
        return filterItem.isEmpty() || ItemStack.isSameItemSameTags(stack, filterItem);
    }

    private EnumProperties.StorageUsed calculateFillLevel() {
        EnumProperties.StorageUsed fillLevel = EnumProperties.StorageUsed.EMPTY;
        int stored = wrapped.getStackInSlot(0).getCount() + wrapped.getStackInSlot(1).getCount();

        if (stored >= getMaxItemCapacity()) fillLevel = EnumProperties.StorageUsed.FULL;
        else if (stored > 0) fillLevel = EnumProperties.StorageUsed.HAS_ITEMS;

        return fillLevel;
    }

    public void updateClientStorageData(MovementContext context) {
        int amount = wrapped.getStackInSlot(0).getCount() + wrapped.getStackInSlot(1).getCount();
        int maxCapacity = getMaxItemCapacity();
        boolean voidUpgrade = !wrapped.getStackInSlot(VOID_UPGRADE_SLOT).isEmpty();
//        if (filterItem.isEmpty() && amount > 0) {
            CompoundTag filterTag = new CompoundTag();
            filterTag.putString("id", ForgeRegistries.ITEMS.getKey(this.wrapped.getStackInSlot(0).getItem()).toString());
            filterTag.putByte("Count", (byte) 1);

            filterItem = ItemStack.of(filterTag);
            context.blockEntityData.put("FilterItem", filterTag);
//        }

        EnumProperties.StorageUsed fillLevel = calculateFillLevel();

        context.blockEntityData.putInt("StoredAmount", amount);
        context.blockEntityData.putBoolean("VoidUpgrade", voidUpgrade);
        context.blockEntityData.putInt("MaxItemCapacity", maxCapacity);
        context.blockEntityData.putFloat("PercentageUsed", getPercent());
        if (!context.state.getValue(SimpleStorageBox.STORAGE_USED).equals(fillLevel)) {
            BlockState updatedState = context.state.setValue(SimpleStorageBox.STORAGE_USED, fillLevel);
            StructureTemplate.StructureBlockInfo updatedInfo = new StructureTemplate.StructureBlockInfo(context.localPos, updatedState, context.blockEntityData);
            context.contraption.getBlocks().put(context.localPos, updatedInfo);
        }

        ModNetwork.sendToAllTracking(context.contraption.entity, new SyncMountedStoragePacket(context.contraption.entity.getId(), context.localPos, fillLevel, context.blockEntityData));
        markClean();
    }

    public void initBlockEntityData(MovementContext context) {
        if (initialized) return;

        filterItem = ItemStack.of(context.blockEntityData.getCompound("FilterItem")).copyWithCount(1);

        Contraption contraption = context.contraption.entity.getContraption();
        storageFilters.clear();

        contraptionFilters.computeIfAbsent(contraption, c -> {
            Set<Item> filterSet = new HashSet<>();
            for (MountedItemStorage storage : c.getStorage().getMountedItems().storages.values()) {
                if (storage instanceof SimpleStorageBoxMountedStorage mounted) {
                    if (!mounted.wrapped.getStackInSlot(0).isEmpty()) {
                        filterSet.add(mounted.wrapped.getStackInSlot(0).getItem());
                    }
                }
            }
            return filterSet;
        });

        storageFilters.addAll(contraptionFilters.get(contraption));

        ModNetwork.sendToAllTracking(context.contraption.entity, new SyncMountedStoragePacket(
                context.contraption.entity.getId(),
                context.localPos,
                calculateFillLevel(),
                context.blockEntityData
        ));
        initialized = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

}
