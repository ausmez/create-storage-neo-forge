package net.fxnt.fxntstorage.backpack.client.tooltip;

import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.container.StorageBoxItem;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackpackTooltip implements TooltipComponent {
    protected List<ItemStack> storage = new ArrayList<>();
    protected List<ItemStack> upgrades = new ArrayList<>();
    protected List<Component> tooltipText = new ArrayList<>();

    private final Item item;

    public BackpackTooltip(ItemStack stack) {
        this.item = stack.getItem();
        this.loadTagData(BlockItem.getBlockEntityData(stack));
    }

    private void loadTagData(CompoundTag tag) {
        this.storage = this.loadInventory(tag);
        this.upgrades = this.loadUpgrades(tag);

        if ((item instanceof BackpackItem || item instanceof SimpleStorageBoxItem) && upgrades.isEmpty() && storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory_or_upgrades").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
        }
        if (item instanceof StorageBoxItem && storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
        }
    }

    private List<ItemStack> loadUpgrades(CompoundTag tag) {
        if (tag == null) return Collections.emptyList();

        List<ItemStack> list = new ArrayList<>();

        if (item instanceof SimpleStorageBoxItem) {
            if (!tag.contains("Items")) return Collections.emptyList();

            ListTag listTag = tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); ++i) {
                ItemStack stack = ItemStack.of(listTag.getCompound(i));
                if (stack.is(ModTags.Items.STORAGE_BOX_UPGRADE)) {
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

        } else {
            if (!tag.contains("Upgrades")) return Collections.emptyList();

            ListTag upgradeTags = tag.getList("Upgrades", Tag.TAG_STRING);
            for (Tag upgrade : upgradeTags) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, upgrade.getAsString()));
                if (item != null) list.add(item.getDefaultInstance());
            }
        }

        // Leave the list in the order the upgrades are saved/stored
        return list;
    }

    private List<ItemStack> loadInventory(CompoundTag tag) {
        ArrayList<ItemStack> list = new ArrayList<>();
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        if (tag == null || !tag.contains("Items")) return Collections.emptyList();

        ListTag listTag = tag.getCompound("Items").getList("Items", Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag listItem = listTag.getCompound(i);
            boolean actualCountTagIsPresent = listItem.contains("ActualCount", Tag.TAG_INT);

            CompoundTag newTag = new CompoundTag();
            newTag.putString("id", listItem.getString("id"));
            newTag.putByte("Count", (actualCountTagIsPresent) ? (byte) 1 : listItem.getByte("Count"));
            if (listItem.contains("tag")) newTag.put("tag", listItem.getCompound("tag"));
            ItemStack stack = ItemStack.of(newTag);

            int slot = listItem.getByte("Slot") & 255;
            // Do not count upgrade slot items. They are dealt with separately
            if (item instanceof BackpackItem && layout.items().contains(slot) ||
                    item instanceof BackpackItem && layout.tools().contains(slot) ||
                    item instanceof SimpleStorageBoxItem && slot == 0 ||
                    item instanceof StorageBoxItem) {

                if (actualCountTagIsPresent) {
                    int actualCount = listItem.getInt("ActualCount");
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
