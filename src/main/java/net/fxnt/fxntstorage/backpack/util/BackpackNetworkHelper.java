package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.PickBlockUpgradePacket;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.network.packet.SyncClientSettingsPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackpackNetworkHelper {

    public static void sortBackpack(int pSlotId, SortOrder pSortOrder) {
        int slotStart;
        int slotEnd;

        if (pSlotId < Util.ITEM_SLOT_END_RANGE) { // BackpackSlots
            slotStart = Util.ITEM_SLOT_START_RANGE;
            slotEnd = Util.ITEM_SLOT_END_RANGE;
        } else if (pSlotId < Util.TOOL_SLOT_END_RANGE) {
            // TODO: Sort tools into tool slots followed by standard items?
            slotStart = Util.TOOL_SLOT_START_RANGE + 5;
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

    public static void sendClientSettings(Player player) {
        List<? extends String> prefersSilkTouchList = ConfigManager.ClientConfig.TOOLSWAP_PREFERS_SILK_TOUCH_LIST.get();
        boolean preferSilkTouch = ConfigManager.ClientConfig.TOOLSWAP_PREFER_SILK_TOUCH.get();
        boolean ignoreFanProcessing = ConfigManager.ClientConfig.MAGNET_IGNORE_FAN_PROCESSING.get();
        boolean displayFeederMessage = ConfigManager.ClientConfig.DISPLAY_FEEDER_MESSAGE.get();
        boolean allowChorusFruit = ConfigManager.ClientConfig.ALLOW_CHORUS_FRUIT.get();
        int torchDeployerCooldown = ConfigManager.ClientConfig.TORCH_DEPLOYER_COOLDOWN.get();
        int torchDeployerLightLevel = ConfigManager.ClientConfig.TORCH_DEPLOYER_LIGHT_LEVEL.get();
        boolean jetpackHoverBobbing = ConfigManager.ClientConfig.JETPACK_HOVER_BOBBING.get();
        ConfigManager.ClientConfig.TorchDeployerLightSource torchDeployerLightSource = ConfigManager.ClientConfig.TORCH_DEPLOYER_LIGHT_SOURCE.get();

        CompoundTag playerData = player.getPersistentData();
        CompoundTag fxntSettingsTag = Util.getOrCreateSubTag(playerData, ConfigManager.FXNTSTORAGE_SETTINGS_TAG);
        fxntSettingsTag.putBoolean("JetpackHoverBobbing", jetpackHoverBobbing);

        ListTag listTag = new ListTag();
        for (String entry : prefersSilkTouchList) {
            listTag.add(StringTag.valueOf(entry));
        }

        CompoundTag settings = new CompoundTag();
        settings.put("prefersSilkTouchList", listTag);
        settings.putBoolean("preferSilkTouch", preferSilkTouch);
        settings.putBoolean("ignoreFanProcessing", ignoreFanProcessing);
        settings.putBoolean("displayFeederMessage", displayFeederMessage);
        settings.putBoolean("allowChorusFruit", allowChorusFruit);
        settings.putInt("torchDeployerCooldown", torchDeployerCooldown);
        settings.putInt("torchDeployerLightLevel", torchDeployerLightLevel);
        settings.putString("torchDeployerLightSource", torchDeployerLightSource.name());
        settings.putBoolean("jetpackHoverBobbing", jetpackHoverBobbing);

        ModNetwork.sendToServer(new SyncClientSettingsPacket(settings));
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
