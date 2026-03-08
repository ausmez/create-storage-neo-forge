package net.fxnt.fxntstorage.backpack.client.menu.slot;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.IUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeRegistry;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.item.upgrades.UpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class UpgradeSlot extends SlotItemHandler {
    private final IBackpackContainer backpack;
    private final Player player;
    private final Runnable onChanged;
    @Nullable
    private final Consumer<ItemStack> onUpgradeTaken;

    public UpgradeSlot(IBackpackContainer backpack, int slot, int x, int y,
                       Player player, Runnable onChanged, @Nullable Consumer<ItemStack> onUpgradeTaken) {
        super(backpack.getItemHandler(), slot, x, y);
        this.backpack = backpack;
        this.player = player;
        this.onChanged = onChanged;
        this.onUpgradeTaken = onUpgradeTaken;
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);

        BackpackMenu.BackpackType type = (backpack instanceof BackpackContainer)
                ? BackpackMenu.BackpackType.WORN
                : BackpackMenu.BackpackType.BLOCK;

        if (player.containerMenu instanceof BackpackMenu menu) {
            UpgradeContext context = UpgradeContext.forMenu(menu, player, menu.container.getItemHandler(), type, null);
            for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
                upgrade.onRemoved(context);
            }
        }

        if (onUpgradeTaken != null) {
            onUpgradeTaken.accept(stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        if (pStack.is(ModTags.Items.BACKPACK_UPGRADE)) {
            UpgradeItem item = (UpgradeItem) pStack.getItem();
            return isUniqueUpgrade(backpack.getItemHandler(), item);
        }
        return false;
    }

    @Override
    public void setByPlayer(@NotNull ItemStack stack) {
        ItemStack oldStack = getItem().copy();

        BackpackMenu.BackpackType type = (backpack instanceof BackpackContainer)
                ? BackpackMenu.BackpackType.WORN
                : BackpackMenu.BackpackType.BLOCK;

        UpgradeContext context = UpgradeContext.forUpgradeSlot(player, backpack, type);
        if (!oldStack.isEmpty()) {
            IUpgrade oldUpgrade = UpgradeRegistry.get(UpgradeType.fromItem(oldStack.getItem()));
            oldUpgrade.onRemoved(context);
        }

        super.setByPlayer(stack);

        IUpgrade upgrade = UpgradeRegistry.get(UpgradeType.fromItem(stack.getItem()));
        if (upgrade != null)
            upgrade.onInstalled(context);
    }

    @Override
    public void setChanged() {
        backpack.setDataChanged();
        if (onChanged != null)
            onChanged.run();
    }

    private boolean isUniqueUpgrade(IItemHandler itemHandler, Item upgradeItem) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        for (int i : layout.upgrades().range()) {
            if (itemHandler.getStackInSlot(i).getItem() == upgradeItem) {
                return false;
            }
        }
        return true;
    }
}
