package net.fxnt.fxntstorage.backpack.main;

import net.minecraft.core.component.DataComponentPatch;
import net.neoforged.neoforge.items.ItemStackHandler;

public interface IBackpackContainer {

    ItemStackHandler getItemHandler();

    int getStackMultiplier();

    void setPlayerInteraction(boolean isPlayer);

    void setDataChanged();

    void setContents(DataComponentPatch componentPatch);

}
