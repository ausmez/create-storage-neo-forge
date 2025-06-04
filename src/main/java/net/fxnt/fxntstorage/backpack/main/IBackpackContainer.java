package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public interface IBackpackContainer {

    ItemStackHandler getItemHandler();

    int getStackMultiplier();

    void setPlayerInteraction(boolean isPlayer);

    void setDataChanged();

    void setTag(CompoundTag tag);

    SortOrder getSortOrder();

    void setSortOrder(SortOrder order);

}
