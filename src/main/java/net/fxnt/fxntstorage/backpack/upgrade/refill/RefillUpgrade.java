package net.fxnt.fxntstorage.backpack.upgrade.refill;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class RefillUpgrade extends AbstractUpgrade {

    public RefillUpgrade() {
        super(UpgradeType.REFILL);
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.level().getGameTime() % 15 != 0)
            return;

        refillHand(context, false);
        refillHand(context, true);
    }

    private void refillHand(UpgradeContext context, boolean isOffHand) {
        ItemStack handItem = isOffHand ? context.player().getOffhandItem() : context.player().getMainHandItem();
        if (handItem.isEmpty() || handItem.getCount() >= handItem.getMaxStackSize()) return;

        if (!(handItem.getItem() instanceof BlockItem)) return; // Continue only if a placeable block
        if (isBlacklisted(context.player(), handItem)) return; // Check if item is blacklisted

        int requiredItems = handItem.getMaxStackSize() - handItem.getCount();
        int ignorePlayerSlot = isOffHand ? 40 : context.player().getInventory().selected;

        // Check Player inventory first
        int remaining = refillFromPlayerInventory(context.player(), handItem, requiredItems, ignorePlayerSlot);
        if (remaining > 0) {
            refillFromBackpack(context.container(), handItem, remaining);
        }
    }

    private boolean isBlacklisted(Player player, ItemStack stack) {
        // Check tags
        if (stack.is(ModTags.Items.REFILL_BLACKLIST)) return true;

        // Check config list
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String fullId = itemId.toString();

        for (String pattern : ClientSettings.getList(player.getUUID(), "RefillBlackList")) {
            if (pattern.endsWith(":*")) {
                // Wildcard match, blacklist namespace
                String namespace = pattern.substring(0, pattern.length() - 2);
                if (itemId.getNamespace().equals(namespace)) return true;
            } else {
                // Exact match
                if (fullId.equals(pattern)) return true;
            }
        }
        return false;
    }

    private int refillFromPlayerInventory(Player player, ItemStack handItem, int requiredItems, int ignoreSlot) {
        Container inventory = player.getInventory();

        int remaining = requiredItems;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i == ignoreSlot) continue;

            ItemStack inventoryItem = inventory.getItem(i);
            if (inventoryItem.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(handItem, inventoryItem)) continue;

            int amountToTransfer = Math.min(remaining, inventoryItem.getCount());
            inventoryItem.shrink(amountToTransfer);
            handItem.grow(amountToTransfer);

            remaining -= amountToTransfer;
        }

        if (requiredItems != remaining) {
            if (player.containerMenu instanceof BackpackMenu menu) {
                menu.container.setDataChanged();
            } else {
                player.containerMenu.slotsChanged(inventory);
            }
        }

        return remaining;
    }

    private void refillFromBackpack(IBackpackContainer container, ItemStack handItem, int requiredItems) {
        IItemHandlerModifiable itemHandler = container.getItemHandler();
        if (itemHandler == null) return;

        int remaining = requiredItems;

        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        for (int i : layout.items().range()) {
            ItemStack containerItem = itemHandler.getStackInSlot(i);
            if (containerItem.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(handItem, containerItem)) continue;

            int amountToTransfer = Math.min(remaining, containerItem.getCount());
            ItemStack extracted = itemHandler.extractItem(i, amountToTransfer, false);
            if (extracted.isEmpty()) continue;

            handItem.grow(extracted.getCount());
            remaining -= extracted.getCount();
        }

        if (requiredItems != remaining) {
            container.setDataChanged();
        }
    }
}
