package net.fxnt.fxntstorage.backpack.tooltip;

import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BackpackTooltip implements TooltipComponent {
    protected List<ItemStack> storage = new ArrayList<>();
    protected List<ItemStack> upgrades = new ArrayList<>();
    protected List<Component> tooltipText = new ArrayList<>();

    public final Item item;

    public BackpackTooltip(ItemStack stack) {
        this.item = stack.getItem();
        loadComponentData(stack);
    }

    private void loadComponentData(ItemStack stack) {
        if (stack.has(DataComponents.CONTAINER)) {
            ArrayList<ItemStack> list = new ArrayList<>();
            List<ItemStack> contents = stack.get(DataComponents.CONTAINER).stream().toList();

            for (ItemStack item : contents) {
                if (item.is(ModTags.Items.BACKPACK_UPGRADE) || item.is(ModTags.Items.STORAGE_BOX_UPGRADE)) continue;
                boolean merged = false;
                for (ItemStack existingStack : list) {
                    if (ItemStack.isSameItemSameComponents(existingStack, item)) {
                        existingStack.grow(item.getCount());
                        merged = true;
                        break;
                    }
                }

                if (!merged && !item.isEmpty()) list.add(item);
            }

            // Sort the list by count in descending order, then by item name in ascending order
            list.sort((stack1, stack2) -> {
                int countCompare = Integer.compare(stack2.getCount(), stack1.getCount());
                if (countCompare != 0) return countCompare;
                return stack1.getItem().toString().compareTo(stack2.getItem().toString());
            });

            this.storage = list;

            if (item instanceof SimpleStorageBoxItem) {
                for (int i = 4; i < contents.size(); ++i) {
                    this.upgrades.add(contents.get(i));
                }
            }

        }

        if (stack.has(ModDataComponents.BACKPACK_UPGRADES)) {
            List<String> upgradeItems = stack.get(ModDataComponents.BACKPACK_UPGRADES).stream().toList();
            for (String upgrade : upgradeItems) {
                // FIXME: These should really be ItemStacks
                this.upgrades.add(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, upgrade)).getDefaultInstance());
            }
        }

        if (item instanceof BackpackItem && upgrades.isEmpty() && storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory_or_upgrades").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
        } else if (storage.isEmpty()) {
            tooltipText.add(Component.translatable("tooltip.fxntstorage.no_inventory").withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
        }
    }

}
