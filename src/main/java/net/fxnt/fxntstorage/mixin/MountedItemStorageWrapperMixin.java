package net.fxnt.fxntstorage.mixin;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import net.fxnt.fxntstorage.registry.ContraptionStorageFilters.FilteredMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MountedItemStorageWrapper.class)
public abstract class MountedItemStorageWrapperMixin extends CombinedInvWrapper {

    @Unique
    private List<FilteredMountedStorage> fxnt$filtered = List.of();

    private MountedItemStorageWrapperMixin(IItemHandlerModifiable... itemHandler) {
        super(itemHandler);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fxnt$collectFilteredStorages(ImmutableMap<BlockPos, MountedItemStorage> storages, CallbackInfo ci) {
        List<FilteredMountedStorage> filtered = new ArrayList<>();
        for (MountedItemStorage storage : storages.values()) {
            if (storage instanceof FilteredMountedStorage f) filtered.add(f);
        }
        fxnt$filtered = filtered;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!fxnt$filtered.isEmpty() && !stack.isEmpty()) {
            int index = getIndexForSlot(slot);
            if (index >= 0 && !(itemHandler[index] instanceof FilteredMountedStorage)) {
                for (FilteredMountedStorage storage : fxnt$filtered) {
                    if (storage.prefersItem(stack)) return stack;
                }
            }
        }
        return super.insertItem(slot, stack, simulate);
    }
}
