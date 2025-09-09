package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.PickBlockUpgradePacket;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BackpackNetworkHelper {

    public static void sortBackpack(int pSlotId, SortOrder pSortOrder) {
        int slotStart;
        int slotEnd;

        if (pSlotId < Util.ITEM_SLOT_END_RANGE) { // BackpackSlots
            slotStart = Util.ITEM_SLOT_START_RANGE;
            slotEnd = Util.ITEM_SLOT_END_RANGE;
        } else if (pSlotId < Util.TOOL_SLOT_END_RANGE) {
            slotStart = Util.TOOL_SLOT_START_RANGE;
            slotEnd = Util.TOOL_SLOT_END_RANGE;
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE) {
            return; // Don't sort upgrade slots
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE + 27) {
            slotStart = Util.UPGRADE_SLOT_END_RANGE;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 27;
        } else {
            slotStart = Util.UPGRADE_SLOT_END_RANGE + 27;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 36;
        }

        ModNetwork.sendToServer(new SortInventoryPacket(Util.INV_TYPE_BACKPACK, slotStart, slotEnd, pSortOrder));
    }

    public static void doPickBlock(ItemStack stack) {
        ModNetwork.sendToServer(new PickBlockUpgradePacket(stack));
    }

    public static void writeItemStack(@NotNull ItemStack stack, FriendlyByteBuf buf) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            Item item = stack.getItem();
            buf.writeVarInt(Item.getId(item));
            buf.writeVarInt(stack.getCount()); // Needed for stacks > 127
            CompoundTag compoundTag = null;
            if (stack.hasTag()) {
                compoundTag = stack.getOrCreateTag();
            }
            buf.writeNbt(compoundTag);
        }
    }

    public static ItemStack readItemStack(@NotNull FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            int itemId = buf.readVarInt();
            int itemCount = buf.readVarInt();
            ItemStack itemstack = new ItemStack(Item.byId(itemId), itemCount);
            itemstack.setTag(buf.readNbt());
            return itemstack;
        }
    }

}
