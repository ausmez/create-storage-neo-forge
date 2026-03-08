package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxDisposeAllPacket;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToolboxDisposeAllPacket.class)
public abstract class ToolboxDisposeAllPacketMixin {

    @Mixin(ToolboxBlockEntity.class)
    public interface ToolboxBlockEntityMixin {
        @Accessor("inventory")
        ToolboxInventory fxnt$getInventory();
    }

    @Inject(
            method = "lambda$handle$0",
            at = @At("TAIL")
    )

    private void fxnt$afterDisposeAll(CompoundTag compound, ServerPlayer player, MutableBoolean sendData, ToolboxBlockEntity toolbox, ToolboxInventory inventory, CallbackInfo ci) {
        ToolboxInventory toolboxInv = ((ToolboxBlockEntityMixin) toolbox).fxnt$getInventory();

        // Check equipped backpack for toolbox items
        if (ClientSettings.getBoolean(player.getUUID(), "CheckBackpackForToolboxItems")
                && BackpackHelper.isWearingBackpack(player)) {

            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            IBackpackContainer backpackContainer = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            for (int i : layout.items().range()) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(toolboxInv, stack, false);
                if (remainder.getCount() != stack.getCount()) {
                    itemHandler.setStackInSlot(i, remainder);
                    backpackContainer.setDataChanged();
                }
            }
        }
    }
}
