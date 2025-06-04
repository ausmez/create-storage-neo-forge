package net.fxnt.fxntstorage.backpack.util;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackpackNetworkHelper {
    public static final ResourceLocation BACKPACK_KEY_OPEN_CLOSE = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_key_open_close");
    public static final ResourceLocation BACKPACK_MENU_CTRL_DOWN = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_menu_ctrl_down");
    public static final ResourceLocation BACKPACK_MENU_CTRL_UP = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_menu_ctrl_up");
    public static final ResourceLocation JETPACK_FLY = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_fly");
    public static final ResourceLocation PLAYER_INPUT = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "player_input");
    public static final ResourceLocation SORT_BACKPACK_INVENTORY = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sort_backpack_inventory");
    public static final ResourceLocation SYNC_CLIENT_SETTINGS = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_client_settings");
    public static final ResourceLocation TOGGLE_HOVER = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "toggle_jetpack_hover");
    public static final ResourceLocation UPGRADE_PICK_BLOCK = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "upgrade_pick_block");
    public static final ResourceLocation SET_SORT_ORDER = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_sort_order");

    private static final FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());

    public static void sendCtrlKeyDown() {
        ModNetwork.sendToServer(new ServerboundPacket(BACKPACK_MENU_CTRL_DOWN, data));
    }

    public static void sendCtrlKeyUp() {
        ModNetwork.sendToServer(new ServerboundPacket(BACKPACK_MENU_CTRL_UP, data));
    }

    public static void sortBackpack(int pSlotId, SortOrder pSortOrder) {
        if (pSlotId < Util.ITEM_SLOT_END_RANGE) {
            data.writeInt(Util.ITEM_SLOT_START_RANGE).writeInt(Util.ITEM_SLOT_END_RANGE); // BackpackSlots
            data.writeEnum(pSortOrder);
        } else if (pSlotId < Util.TOOL_SLOT_END_RANGE) {
            // TODO: Sort tools into tool slots followed by standard items?
            data.writeInt(Util.TOOL_SLOT_START_RANGE + 5).writeInt(Util.TOOL_SLOT_END_RANGE); // ToolSlots
            data.writeEnum(pSortOrder);
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE) {
            return; // We don't sort upgrade slots
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE + 27) {
            data.writeInt(Util.UPGRADE_SLOT_END_RANGE).writeInt(Util.UPGRADE_SLOT_END_RANGE + 27); // PlayerSlots
            data.writeEnum(pSortOrder);
        } else {
            data.writeInt(Util.UPGRADE_SLOT_END_RANGE + 27).writeInt(Util.UPGRADE_SLOT_END_RANGE + 36); // Hot bar
            data.writeEnum(pSortOrder);
        }

        ModNetwork.sendToServer(new ServerboundPacket(SORT_BACKPACK_INVENTORY, data));
    }

    public static void sendClientSettings() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        List<? extends String> prefersSilkTouchList = ConfigManager.ClientConfig.TOOLSWAP_PREFERS_SILK_TOUCH_LIST.get();
        boolean prefersSilkTouch = ConfigManager.ClientConfig.TOOLSWAP_PREFER_SILK_TOUCH.get();
        boolean ignoreFanProcessing = ConfigManager.ClientConfig.MAGNET_IGNORE_FAN_PROCESSING.get();
        boolean displayFeederMessage = ConfigManager.ClientConfig.DISPLAY_FEEDER_MESSAGE.get();

        buf.writeInt(prefersSilkTouchList.size());
        for (String blockId : prefersSilkTouchList) {
            buf.writeUtf(blockId);
        }
        buf.writeBoolean(prefersSilkTouch);
        buf.writeBoolean(ignoreFanProcessing);
        buf.writeBoolean(displayFeederMessage);

        ModNetwork.sendToServer(new ServerboundPacket(BackpackNetworkHelper.SYNC_CLIENT_SETTINGS, buf));
    }

    public static void doPickBlock(ItemStack stack) {
        data.writeItem(stack);
        ModNetwork.sendToServer(new ServerboundPacket(UPGRADE_PICK_BLOCK, data));
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
