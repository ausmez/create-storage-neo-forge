package net.fxnt.fxntstorage.backpack.inventory;

import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public interface IBackpackContainer {

    ItemStackHandler getItemHandler();

    int getStackMultiplier();

    void setPlayerInteraction(boolean isPlayer);

    void setDataChanged();

    boolean stillValid(Player player);

    SortOrder getSortOrder();

    void setSortOrder(SortOrder order);

    boolean isPanelExpanded(UpgradeType type);

    void togglePanelExpanded(UpgradeType type);

    void clearPanelExpanded(UpgradeType type);

    @Nullable
    UpgradeType getExpandedPanel();

    void setExpandedPanel(@Nullable UpgradeType type);

    boolean getUpgradeSetting(UpgradeDataSync.Field upgrade);

    void setUpgradeSetting(UpgradeDataSync.Field upgrade, boolean value);

    void saveSettings();
}
