package net.fxnt.fxntstorage.container.mounted;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.menu.StorageInteractionWrapper;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.utility.CreateCodecs;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SyncMountedStoragePacket;
import net.fxnt.fxntstorage.registry.ContraptionStorageFilters;
import net.fxnt.fxntstorage.util.SortOrder;
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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class StorageBoxMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> implements ContraptionStorageFilters.FilteredMountedStorage {
    public static final Codec<StorageBoxMountedStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER.xmap(
            StorageBoxMountedStorage::new, storage -> storage.wrapped
    );

    public boolean initialized = false;
    private boolean dirty = false;

    private FilterItemStack filterItem = FilterItemStack.empty();
    private @Nullable Contraption currentContraption = null;
    private boolean voidUpgrade;
    private SortOrder sortOrder;

    protected StorageBoxMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
        super(type, handler);
    }

    protected StorageBoxMountedStorage(ItemStackHandler handler) {
        this(ModMountedStorageTypes.STORAGE_BOX_MOUNTED.get(), handler);
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        if (player.isSpectator()) return false;

        ItemStack itemInHand = player.getMainHandItem();

        // Right-Click with Create Wrench in hand will toggle void mode
        if (itemInHand.is(AllTags.AllItemTags.WRENCH.tag) && info.nbt() != null) {
            boolean nbtValue = info.nbt().getBoolean("VoidUpgrade");
            voidUpgrade = !nbtValue;

            CompoundTag newNbt = info.nbt().copy();
            newNbt.putBoolean("VoidUpgrade", !nbtValue);

            BlockState updatedState = info.state().setValue(StorageBox.VOID_UPGRADE, !nbtValue);

            StructureTemplate.StructureBlockInfo updatedInfo = new StructureTemplate.StructureBlockInfo(info.pos(), updatedState, newNbt);
            contraption.getBlocks().put(info.pos(), updatedInfo);

            markDirty();
            return true;
        }

        if (!itemInHand.isEmpty()) {
            // Current item in player hand will be inserted into container
            if (filterItem.isEmpty() || filterTest(itemInHand)) {
                ItemStack remainder = ItemHandlerHelper.insertItem(this.wrapped, itemInHand, false);

                if (remainder.getCount() > 0 && voidUpgrade) {
                    remainder = ItemStack.EMPTY;
                }

                if (remainder.getCount() <= itemInHand.getCount()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, remainder);
                } else {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                markDirty();
                return true;
            } else {
                return false;
            }
        }

        ServerLevel level = player.serverLevel();
        BlockPos localPos = info.pos();
        Vec3 localPosVec = Vec3.atCenterOf(localPos);
        Predicate<Player> stillValid = p -> {
            Vec3 currentPos = contraption.entity.toGlobalVector(localPosVec, 0);
            return this.isMenuValid(player, contraption, currentPos);
        };
        Component menuName = this.getMenuName(info, contraption);
        Consumer<Player> onClose = p -> {
            Vec3 newPos = contraption.entity.toGlobalVector(localPosVec, 0);
            this.playClosingSound(level, newPos);
        };

        NetworkHooks.openScreen(
                player,
                this.createMenu(
                        menuName, this.wrapped, stillValid, onClose, contraption.entity.getId(), localPos, info.nbt()
                ), buf -> {
                    buf.writeInt(this.wrapped.getSlots());
                    buf.writeInt(contraption.entity.getId());
                    buf.writeBlockPos(localPos);
                    buf.writeNbt(info.nbt());
                }
        );
        return true;
    }

    protected @Nullable MenuProvider createMenu(Component name, IItemHandlerModifiable handler, Predicate<Player> stillValid, Consumer<Player> onClose, int contraptionId, BlockPos localPos, CompoundTag nbt) {
        Container wrapper = new StorageInteractionWrapper(handler, stillValid, onClose);
        return new SimpleMenuProvider(
                (id, inv, player) -> new StorageBoxMountedMenu(id, inv, wrapper, contraptionId, localPos, nbt),
                name
        );
    }

    public static StorageBoxMountedStorage fromStorage(StorageBoxEntity storageBox) {
        return new StorageBoxMountedStorage(copyToItemStackHandler(storageBox.getItemHandler()));
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof StorageBoxEntity storageBoxEntity) {
            storageBoxEntity.applyInventoryToBlock(this.wrapped);
        }

        currentContraption = null;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!this.isItemValid(slot, stack))
            return stack;

        ItemStack amount = super.insertItem(slot, stack, simulate);
        if (slot == wrapped.getSlots() - 1 && !amount.isEmpty() && voidUpgrade) {
            return ItemStack.EMPTY;
        }
        markDirty();
        return amount;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.extractItem(slot, amount, simulate);
        if (!stack.isEmpty() && !simulate)
            markDirty();
        return stack;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (currentContraption != null) {
            ContraptionStorageFilters registry = ContraptionStorageFilters.getOrCreate(currentContraption);
            if (registry.matches(currentContraption.entity.level(), stack) && filterItem.isEmpty()) {
                return false;
            }
        }

        return filterTest(stack);
    }

    private boolean filterTest(ItemStack stack) {
        return filterItem.test(null, stack);
    }

    private float calculatePercentageUsed() {
        int totalSpace = 0;
        int usedSpace = 0;

        for (int i = 0; i < wrapped.getSlots(); i++) {
            var stack = wrapped.getStackInSlot(i);
            int maxStackSize = stack.isEmpty() ? 64 : stack.getMaxStackSize();

            totalSpace += maxStackSize;
            usedSpace += stack.getCount();
        }

        if (totalSpace == 0) return 0;

        return ((float) usedSpace / totalSpace) * 100;
    }

    private EnumProperties.StorageUsed calculateFillLevel() {
        int totalSlots = wrapped.getSlots();
        boolean allSlotsFull = true;

        int filledSlots = 0;
        for (int i = 0; i < wrapped.getSlots(); i++) {
            ItemStack slot = wrapped.getStackInSlot(i);
            if (!slot.isEmpty()) {
                filledSlots++;
                if (slot.getCount() < slot.getMaxStackSize()) {
                    allSlotsFull = false;
                }
            } else {
                allSlotsFull = false;
            }
        }
        int emptySlots = totalSlots - filledSlots;

        EnumProperties.StorageUsed newStorageUsed = EnumProperties.StorageUsed.EMPTY;

        if (allSlotsFull) {
            newStorageUsed = EnumProperties.StorageUsed.FULL;
        } else if (emptySlots == 0) {
            newStorageUsed = EnumProperties.StorageUsed.SLOTS_FILLED;
        } else if (filledSlots > 0) {
            newStorageUsed = EnumProperties.StorageUsed.HAS_ITEMS;
        }

        return newStorageUsed;
    }

    public void updateClientStorageData(MovementContext context) {
        int amount = IntStream.range(0, wrapped.getSlots()).map(i -> wrapped.getStackInSlot(i).getCount()).sum();
        float percentUsed = calculatePercentageUsed();
        EnumProperties.StorageUsed fillLevel = calculateFillLevel();

        context.blockEntityData.putInt("StoredAmount", amount);
        context.blockEntityData.putFloat("PercentageUsed", percentUsed);
        context.blockEntityData.putString("SortOrder", sortOrder.name());
        context.blockEntityData.putBoolean("VoidUpgrade", voidUpgrade);
        context.state.setValue(StorageBox.STORAGE_USED, fillLevel);
        context.state.setValue(StorageBox.VOID_UPGRADE, voidUpgrade);

        ModNetwork.sendToAllTracking(context.contraption.entity, new SyncMountedStoragePacket(context.contraption.entity.getId(), context.localPos, fillLevel, context.blockEntityData));
        markClean();
    }

    public void initBlockEntityData(MovementContext context) {
        if (initialized) return;

        filterItem = FilterItemStack.of(context.blockEntityData.getCompound("Filter"));
        voidUpgrade = context.blockEntityData.getBoolean("VoidUpgrade");
        sortOrder = SortOrder.valueOf(context.blockEntityData.getString("SortOrder"));

        this.currentContraption = context.contraption.entity.getContraption();

        if (this.currentContraption != null && !filterItem.isEmpty()) {
            ContraptionStorageFilters registry = ContraptionStorageFilters.getOrCreate(currentContraption);
            registry.register(this, filterItem);
        }

        ModNetwork.sendToAllTracking(context.contraption.entity, new SyncMountedStoragePacket(
                context.contraption.entity.getId(),
                context.localPos,
                calculateFillLevel(),
                context.blockEntityData
        ));
        initialized = true;
    }

    public void setSortOrder(SortOrder order) {
        sortOrder = order;
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
