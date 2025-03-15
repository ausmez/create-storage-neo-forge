package net.fxnt.fxntstorage.backpacks.main;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.containers.StorageBoxItem;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackpackTooltip implements TooltipComponent {
    protected List<ItemStack> storage = new ArrayList<>();
    protected List<ItemStack> upgrades = new ArrayList<>();
    protected List<Component> tooltipText = new ArrayList<>();

    private final ItemStackHandler inventory = new ItemStackHandler(BackpackBlock.totalSlotCount);

    private final Item item;

    public BackpackTooltip(ItemStack stack) {
        this.item = stack.getItem();
        this.loadTagData(BlockItem.getBlockEntityData(stack));
    }

    private void loadTagData(CompoundTag tag) {
        this.storage = this.loadInventory(tag);
        this.upgrades = this.loadUpgrades(tag);

        if (item instanceof BackpackItem && upgrades.isEmpty() && storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory_or_upgrades").withStyle(ChatFormatting.YELLOW));
        } else if (storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory").withStyle(ChatFormatting.YELLOW));
        }
    }

    private List<ItemStack> loadUpgrades(CompoundTag tag) {
        ArrayList<ItemStack> list = new ArrayList<>();

        if (tag == null || !tag.contains("Upgrades")) return Collections.emptyList();

        ListTag upgradeTags = tag.getList("Upgrades", Tag.TAG_STRING);
        for (Tag upgrade : upgradeTags) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FXNTStorage.MOD_ID, upgrade.getAsString()));
            if (item != null) list.add(item.getDefaultInstance());
        }

        // Leave the list in the order the upgrades are saved/stored
        return list;
    }

    private List<ItemStack> loadInventory(CompoundTag tag) {
        ArrayList<ItemStack> list = new ArrayList<>();

        if (tag == null || !tag.contains("Items")) return Collections.emptyList();

        this.inventory.deserializeNBT(tag);

        ListTag listTag;
        listTag = (item instanceof StorageBoxItem)
                ? tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND)
                : tag.getList("Items", Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag item = listTag.getCompound(i);
            boolean actualCountTagIsPresent = item.contains("ActualCount", Tag.TAG_INT);

            CompoundTag newTag = new CompoundTag();
            newTag.putString("id", item.getString("id"));
            newTag.putByte("Count", (actualCountTagIsPresent) ? (byte) 1 : item.getByte("Count"));
            if (item.contains("tag")) newTag.put("tag", item.getCompound("tag"));
            ItemStack stack = ItemStack.of(newTag);

            int slot = item.getByte("Slot") & 255;
            // Do not count upgrade slot items. They are dealt with separately
            if (slot < Util.UPGRADE_SLOT_START_RANGE) {

                if (actualCountTagIsPresent) {
                    int actualCount = item.getInt("ActualCount");
                    stack.setCount(Math.max(actualCount, 0));
                }

                boolean merged = false;
                for (ItemStack existingStack : list) {
                    if (ItemStack.isSameItemSameTags(existingStack, stack)) {
                        existingStack.grow(stack.getCount());
                        merged = true;
                        break;
                    }
                }

                if (!merged && !stack.isEmpty()) list.add(stack);
            }
        }

        // Sort the list by count in descending order, then by item name in ascending order
        list.sort((stack1, stack2) -> {
            int countCompare = Integer.compare(stack2.getCount(), stack1.getCount());
            if (countCompare != 0) return countCompare;
            return stack1.getItem().toString().compareTo(stack2.getItem().toString());
        });

        return list;
    }

}
