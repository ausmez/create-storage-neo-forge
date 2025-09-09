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
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.SyncMountedStoragePacket;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    private ItemStack filterItem = ItemStack.EMPTY;

    protected SimpleStorageBoxMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
        super(type, handler);
    }

    protected SimpleStorageBoxMountedStorage(ItemStackHandler handler) {
        this(ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED.get(), handler);
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        if (player.isSpectator()) return false;

        ItemStack itemInHand = player.getMainHandItem();

        if (!itemInHand.isEmpty()) {
            if (itemInHand.getItem().equals(filterItem.getItem()) || filterItem.isEmpty()) {
                if (!itemInHand.is(AllTags.AllItemTags.WRENCH.tag)) {
                    ItemStack remain = wrapped.insertItem(1, itemInHand, false);
                    player.setItemInHand(InteractionHand.MAIN_HAND, remain);
                }
            } else if (itemInHand.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {
                if (itemInHand.is(ModItems.STORAGE_BOX_VOID_UPGRADE.get())) {
                    if (!this.hasVoidUpgrade()) {
                        wrapped.setStackInSlot(VOID_UPGRADE_SLOT, itemInHand.copyWithCount(1));
                        if (!player.isCreative()) {
                            itemInHand.shrink(1);
                            player.getInventory().setChanged();
                        }
                    } else {
                        ItemStack voidStack = wrapped.getStackInSlot(VOID_UPGRADE_SLOT);
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
                        wrapped.setStackInSlot(VOID_UPGRADE_SLOT, ItemStack.EMPTY);
                    }
                } else if (itemInHand.is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                    boolean canAddUpgrade = false;
                    for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
                        if (wrapped.getStackInSlot(i).isEmpty()) {
                            wrapped.setStackInSlot(i, itemInHand.copyWithCount(1));
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
        return wrapped.getStackInSlot(VOID_UPGRADE_SLOT).is(ModItems.STORAGE_BOX_VOID_UPGRADE.asItem());
    }

    private int getMaxItemCapacity() {
        int upgradeCount = 0;

        for (int i = CAPACITY_UPGRADE_SLOT_START; i < CAPACITY_UPGRADE_SLOT_START + MAX_CAPACITY_UPGRADES; i++) {
            if (wrapped.getStackInSlot(i).is(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get())) {
                upgradeCount++;
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

    public static SimpleStorageBoxMountedStorage fromStorage(SimpleStorageBoxEntity simpleStorageBox) {
        return new SimpleStorageBoxMountedStorage(copyToItemStackHandler(simpleStorageBox.getItemHandler()));
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof SimpleStorageBoxEntity simpleStorageBox) {
            simpleStorageBox.applyInventoryToBlock(wrapped, filterItem);
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
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot > 0)
            return stack;
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        if (!isItemValid(slot, stack))
            return stack;

        ItemStack existing = wrapped.getStackInSlot(slot);

        int limit = getMaxItemCapacity();

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return (hasVoidUpgrade()) ? ItemStack.EMPTY : stack;
        } else {
            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
                if (existing.isEmpty()) {
                    wrapped.setStackInSlot(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                } else {
                    existing.grow(reachedLimit ? limit : stack.getCount());
                }
            }
            if (filterItem.isEmpty()) filterItem = stack.copyWithCount(1);

            markDirty();

            if (reachedLimit)
                return (hasVoidUpgrade()) ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - limit);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.extractItem(slot, amount, simulate);
        if (!stack.isEmpty() && !simulate)
            markDirty();
        return stack;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!storageFilters.isEmpty() && storageFilters.contains(stack.getItem()) && filterItem.isEmpty())
            return false;

        if (filterTest(stack)) {
            boolean voidUpgrade = hasVoidUpgrade();
            if (slot > 0) return false;
            if (voidUpgrade) return true;
            return wrapped.getStackInSlot(0).getCount() < getMaxItemCapacity();
        }
        return false;
    }

    private float getPercent() {
        return ((float) wrapped.getStackInSlot(0).getCount() / getMaxItemCapacity()) * 100;
    }

    public boolean filterTest(ItemStack stack) {
        return filterItem.isEmpty() || ItemStack.isSameItemSameTags(stack, filterItem);
    }

    private EnumProperties.StorageUsed calculateFillLevel() {
        EnumProperties.StorageUsed fillLevel = EnumProperties.StorageUsed.EMPTY;
        int stored = wrapped.getStackInSlot(0).getCount();

        if (stored >= getMaxItemCapacity()) fillLevel = EnumProperties.StorageUsed.FULL;
        else if (stored > 0) fillLevel = EnumProperties.StorageUsed.HAS_ITEMS;

        return fillLevel;
    }

    public void updateClientStorageData(MovementContext context) {
        int amount = wrapped.getStackInSlot(0).getCount();
        int maxCapacity = getMaxItemCapacity();
        boolean voidUpgrade = !wrapped.getStackInSlot(VOID_UPGRADE_SLOT).isEmpty();

        if (!wrapped.getStackInSlot(0).isEmpty() && filterItem.isEmpty()) {
            filterItem = wrapped.getStackInSlot(0).copyWithCount(1);
        }

        context.blockEntityData.put("FilterItem", filterItem.save(new CompoundTag()));

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

        // --- Slot layout migration check (1.1.2)
        CompoundTag itemsTag = context.blockEntityData.getCompound("Items");
        int oldSize = itemsTag.getInt("Size");

        if (oldSize != SLOT_COUNT) {
            FXNTStorage.LOGGER.debug("Migrating slot layout from previous version of Simple Storage Box mounted on contraption {} at {}", context.contraption.entity.getId(), context.localPos);
            if (oldSize == 0) {
                // Slot layout does not contain a Size tag
                ListTag existingItems = context.blockEntityData.getList("Items", Tag.TAG_COMPOUND);
                oldSize = context.blockEntityData.getInt("slotCount");
                CompoundTag newTag = new CompoundTag();

                newTag.putInt("Size", oldSize);
                newTag.put("Items", existingItems);
                itemsTag = newTag;
            }

            CompoundTag migratedNbt = migrateSlotItems(itemsTag, oldSize); // Slot layout migration
            context.blockEntityData.put("Items", migratedNbt);

            ItemStackHandler newWrapped = createItemHandler(SLOT_COUNT);
            newWrapped.deserializeNBT(migratedNbt);
            for (int i = 0; i < wrapped.getSlots(); i++) {
                if (i < newWrapped.getSlots()) {
                    wrapped.setStackInSlot(i, newWrapped.getStackInSlot(i));
                } else {
                    wrapped.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }

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

    private CompoundTag migrateSlotItems(CompoundTag itemsTag, int oldSize) {
        ItemStackHandler oldHandler = createItemHandler(oldSize);
        ItemStackHandler newHandler = createItemHandler(SLOT_COUNT);
        oldHandler.deserializeNBT(itemsTag);

        // --- Old Slot0 + Slot1 -> New Slot0
        ItemStack slot0 = oldHandler.getStackInSlot(0);
        ItemStack slot1 = oldHandler.getStackInSlot(1);

        if (!slot0.isEmpty() || !slot1.isEmpty()) {
            ItemStack merged = slot0.copy();

            int totalCount = slot0.getCount() + slot1.getCount();
            merged.setCount(Math.min(totalCount, getMaxItemCapacity()));

            newHandler.setStackInSlot(0, merged);
        }

        // --- Old Slot3 -> New Slot1
        newHandler.setStackInSlot(1, oldHandler.getStackInSlot(3).copy());

        // --- Old Slot4-12 -> New Slot2-10
        for (int oldSlot = 4; oldSlot <= 12; oldSlot++) {
            int newSlot = (oldSlot - 4) + 2;
            if (newSlot < newHandler.getSlots()) {
                newHandler.setStackInSlot(newSlot, oldHandler.getStackInSlot(oldSlot).copy());
            }
        }

        return newHandler.serializeNBT();
    }

    private @NotNull ItemStackHandler createItemHandler(int slotCount) {
        return new ItemStackHandler(slotCount) {
            @Override
            public CompoundTag serializeNBT() {
                ListTag nbtTagList = new ListTag();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putInt("Slot", i);
                        itemTag.putInt("ActualCount", stacks.get(i).getCount());
                        stacks.get(i).save(itemTag);
                        nbtTagList.add(itemTag);
                    }
                }
                CompoundTag nbt = new CompoundTag();
                nbt.put("Items", nbtTagList);
                nbt.putInt("Size", stacks.size());
                return nbt;
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
                ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < tagList.size(); i++) {
                    CompoundTag itemTags = tagList.getCompound(i);
                    int slot = itemTags.getInt("Slot");
                    ItemStack slotStack = ItemStack.of(itemTags);
                    if (itemTags.contains("ActualCount", Tag.TAG_INT)) {
                        slotStack.setCount(itemTags.getInt("ActualCount"));
                    }
                    stacks.set(slot, slotStack);
                }
                onLoad();
            }
        };
    }

}
