package net.fxnt.fxntstorage.backpacks.main;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public interface IBackpackContainer {

    ItemStackHandler getItemHandler();

    int getStackMultiplier();

    void setPlayerInteraction(boolean isPlayer);

    void setDataChanged();

    void setTag(CompoundTag tag);

}
