package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.util.SortOrder;
import net.neoforged.neoforge.items.ItemStackHandler;

public interface IBackpackContainer {

    ItemStackHandler getItemHandler();

    int getStackMultiplier();

    void setPlayerInteraction(boolean isPlayer);

    void setDataChanged();

    SortOrder getSortOrder();

    void setSortOrder(SortOrder order);

}
