package net.fxnt.fxntstorage.reserve_storage.mounted;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.menu.StorageInteractionWrapper;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.codec.CreateCodecs;
import com.simibubi.create.foundation.utility.CreateLang;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.network.packet.SyncMountedStoragePacket;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBox;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxMenu;
import net.fxnt.fxntstorage.util.ContraptionInteractionContext;
import net.fxnt.fxntstorage.util.DeployerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ReserveStorageBoxMountedStorage extends SimpleMountedStorage {
    private static final int STORAGE_SLOTS = 27;
    private static final int GHOST_SLOT_OFFSET = 27;
    private static final int GHOST_SLOTS = 9;

    private boolean dirty = false;

    public static final MapCodec<ReserveStorageBoxMountedStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER.xmap(
            ReserveStorageBoxMountedStorage::new, storage -> storage.wrapped
    ).fieldOf("value");

    protected ReserveStorageBoxMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
        super(type, handler);
    }

    protected ReserveStorageBoxMountedStorage(ItemStackHandler handler) {
        this(ModMountedStorageTypes.RESERVE_STORAGE_BOX_MOUNTED.get(), handler);
    }

    @Override
    public int getSlots() {
        return STORAGE_SLOTS;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot >= STORAGE_SLOTS) return stack;
        if (!simulate) markDirty();
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack slotStack = wrapped.getStackInSlot(slot);
        if (slotStack.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        int minimum = getConfiguredMinimum(slotStack);
        // Deployers bypass the minimum so they can keep deploying
        if (minimum <= 0 || DeployerContext.DEPLOYER_ACTIVE.get()) {
            if (!simulate) markDirty();
            return super.extractItem(slot, amount, simulate);
        }

        int total = getTotalStorageCount(slotStack);
        int extractable = Math.max(0, total - minimum);
        if (extractable == 0) {
            if (!simulate) markDirty();
            return ItemStack.EMPTY;
        }

        if (!simulate) markDirty();
        return super.extractItem(slot, Math.min(amount, extractable), simulate);
    }

    private int getConfiguredMinimum(ItemStack item) {
        for (int i = 0; i < GHOST_SLOTS; i++) {
            ItemStack ghost = wrapped.getStackInSlot(GHOST_SLOT_OFFSET + i);
            if (!ghost.isEmpty() && ItemStack.isSameItemSameComponents(ghost, item)) {
                return ghost.getCount();
            }
        }
        return 0;
    }

    private int getTotalStorageCount(ItemStack item) {
        int total = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = wrapped.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean isGhostDuplicate(int targetSlot, ItemStack item) {
        if (item.isEmpty()) return false;
        for (int i = GHOST_SLOT_OFFSET; i < GHOST_SLOT_OFFSET + GHOST_SLOTS; i++) {
            if (i == targetSlot) continue;
            if (ItemStack.isSameItem(wrapped.getStackInSlot(i), item)) return true;
        }
        return false;
    }

    public void setGhostSlot(int absoluteSlot, ItemStack stack) {
        wrapped.setStackInSlot(absoluteSlot, stack);
        markDirty();
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        if (player.isSpectator()) return false;

        ItemStack itemInHand = player.getMainHandItem();
        Direction side = ContraptionInteractionContext.INTERACTION_DIRECTION.get();
        if (side == null) return false;
        if (!side.equals(info.state().getValue(ReserveStorageBox.FACING))) return false;

        if (!itemInHand.isEmpty()) {
            ItemStack remainder = ItemHandlerHelper.insertItem(this, itemInHand, false);
            if (remainder.getCount() <= itemInHand.getCount()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, remainder);
            } else {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            markDirty();
            return true;
        }

        if (!player.isShiftKeyDown()) return false;

        ServerLevel level = player.serverLevel();
        BlockPos localPos = info.pos();
        Vec3 localPosVec = Vec3.atCenterOf(localPos);

        Predicate<Player> stillValid = p -> {
            Vec3 currentPos = contraption.entity.toGlobalVector(localPosVec, 0);
            return this.isMenuValid(player, contraption, currentPos);
        };

        CompoundTag nbt = info.nbt();
        Component customName = (nbt != null && nbt.contains("CustomName", Tag.TAG_STRING))
                ? BlockEntity.parseCustomNameSafe(nbt.getString("CustomName"), player.registryAccess())
                : null;
        Component blockName = customName != null ? customName : info.state().getBlock().getName();
        Component menuName = CreateLang.translateDirect("contraptions.moving_container", blockName);

        Consumer<Player> onClose = p -> {
            Vec3 newPos = contraption.entity.toGlobalVector(localPosVec, 0);
            this.playClosingSound(level, newPos);
        };

        OptionalInt id = player.openMenu(
                createMenu(menuName, this.wrapped, stillValid, onClose, contraption.entity.getId(), localPos, nbt),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeInt(this.wrapped.getSlots());
                    for (int i = GHOST_SLOT_OFFSET; i < GHOST_SLOT_OFFSET + GHOST_SLOTS; i++) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.wrapped.getStackInSlot(i));
                    }
                    buf.writeInt(contraption.entity.getId());
                    buf.writeBlockPos(localPos);
                }
        );
        return id.isPresent();
    }

    protected @Nullable MenuProvider createMenu(Component name, IItemHandlerModifiable handler,
                                                Predicate<Player> stillValid, Consumer<Player> onClose,
                                                int contraptionId, BlockPos localPos, CompoundTag nbt) {
        Container wrapper = new StorageInteractionWrapper(handler, stillValid, onClose);
        return new SimpleMenuProvider(
                (id, inv, player) -> new ReserveStorageBoxMenu(id, inv, wrapper, contraptionId, localPos),
                name
        );
    }

    public static ReserveStorageBoxMountedStorage fromStorage(ReserveStorageBoxEntity reserveStorageBox) {
        return new ReserveStorageBoxMountedStorage(copyToItemStackHandler(reserveStorageBox.getItemHandler()));
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof ReserveStorageBoxEntity bbe) {
            bbe.applyInventoryToBlock(this.wrapped);
        }

        EnumProperties.StorageUsed fillLevel = EnumProperties.calculateFillLevel(wrapped, wrapped.getSlots() - 9);
        BlockState currentState = level.getBlockState(pos);
        if (currentState.hasProperty(ReserveStorageBox.STORAGE_USED) && currentState.getValue(ReserveStorageBox.STORAGE_USED) != fillLevel) {
            level.setBlock(pos, currentState.setValue(ReserveStorageBox.STORAGE_USED, fillLevel), Block.UPDATE_CLIENTS);
        }
    }

    public ReserveCalcResult calculateReserve() {
        int storedAmount = 0;
        int capacity = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = wrapped.getStackInSlot(i);
            storedAmount += stack.getCount();
            capacity += stack.isEmpty() ? 64 : stack.getMaxStackSize();
        }
        float percentageUsed = capacity == 0 ? 0f : (storedAmount * 100f) / capacity;

        char[] status = new char[GHOST_SLOTS];
        int totalRequired = 0;
        int totalFulfilled = 0;

        for (int i = 0; i < GHOST_SLOTS; i++) {
            ItemStack ghost = wrapped.getStackInSlot(GHOST_SLOT_OFFSET + i);
            if (ghost.isEmpty()) {
                status[i] = 'E'; // not configured
                continue;
            }
            int required = ghost.getCount();
            int stored = 0;
            for (int j = 0; j < STORAGE_SLOTS; j++) {
                ItemStack slot = wrapped.getStackInSlot(j);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, ghost)) {
                    stored += slot.getCount();
                }
            }
            status[i] = stored >= required ? 'F' // fully met
                    : stored > 0 ? 'P' // partially met
                      : 'U'; // nothing stored
            totalRequired += required;
            totalFulfilled += Math.min(stored, required);
        }

        float percent = totalRequired == 0 ? -1f : (float) totalFulfilled / totalRequired * 100f;
        return new ReserveCalcResult(percent, new String(status), storedAmount, percentageUsed);
    }

    public void updateClientStorageData(MovementContext context) {
        ReserveCalcResult result = calculateReserve();

        float percent = result.percent();
        String slotStatus = result.slotStatus();
        int storedAmount = result.storedAmount();
        float percentageUsed = result.percentageUsed();
        EnumProperties.StorageUsed fillLevel = EnumProperties.calculateFillLevel(wrapped, wrapped.getSlots() - 9);

        context.blockEntityData.putFloat("ReservePercent", percent);
        context.blockEntityData.putString("ReserveSlotStatus", slotStatus);
        context.blockEntityData.putInt("StoredAmount", storedAmount);
        context.blockEntityData.putFloat("PercentageUsed", percentageUsed);
        context.state = context.state.setValue(ReserveStorageBox.STORAGE_USED, fillLevel);
        PacketDistributor.sendToPlayersTrackingEntity(
                context.contraption.entity,
                new SyncMountedStoragePacket(context.contraption.entity.getId(), context.localPos, fillLevel, context.blockEntityData)
        );
        markClean();
    }

    public record ReserveCalcResult(float percent, String slotStatus, int storedAmount, float percentageUsed) {
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
