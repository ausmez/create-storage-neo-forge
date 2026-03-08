package net.fxnt.fxntstorage.compat.constructionwand;

import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import thetadev.constructionwand.api.IContainerHandler;
import thetadev.constructionwand.basics.WandUtil;

import java.util.Optional;

public class HandlerStorageBox implements IContainerHandler {

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getCount() == 1
                && (Block.byItem(inventoryStack.getItem()) instanceof StorageBox
                || Block.byItem(inventoryStack.getItem()) instanceof SimpleStorageBox);
    }

    @Override
    public int countItems(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        Optional<IItemHandler> itemHandlerOptional = inventoryStack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (itemHandlerOptional.isEmpty()) {
            return 0;
        } else {
            int total = 0;
            IItemHandler itemHandler = itemHandlerOptional.get();

            for (int i = 0; i < itemHandler.getSlots(); ++i) {
                ItemStack containerStack = itemHandler.getStackInSlot(i);
                if (WandUtil.stackEquals(itemStack, containerStack)) {
                    total += Math.max(0, containerStack.getCount());
                }
            }

            return total;
        }
    }

    @Override
    public int useItems(Player player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        Optional<IItemHandler> itemHandlerOptional = inventoryStack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (itemHandlerOptional.isEmpty()) {
            return 0;
        } else {
            IItemHandler itemHandler = itemHandlerOptional.get();

            for (int i = 0; i < itemHandler.getSlots(); ++i) {
                ItemStack handlerStack = itemHandler.getStackInSlot(i);
                if (WandUtil.stackEquals(itemStack, handlerStack)) {
                    ItemStack extracted = itemHandler.extractItem(i, count, false);
                    count -= extracted.getCount();
                    if (count <= 0) {
                        break;
                    }
                }
            }

            saveInventoryToStack(inventoryStack);
            return count;
        }
    }

    public static void saveInventoryToStack(ItemStack stack) {
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(cap -> {
            if (cap instanceof ItemStackHandler handler) {
                CompoundTag beTag = stack.getOrCreateTagElement("BlockEntityTag");
                beTag.put("Items", handler.serializeNBT());

                if (Block.byItem(stack.getItem()) instanceof SimpleStorageBox) {
                    beTag.putInt("StoredAmount", handler.getStackInSlot(0).getCount());
                    BlockItem.setBlockEntityData(stack, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), beTag);
                } else if (Block.byItem(stack.getItem()) instanceof StorageBox) {
                    int storedAmount = 0;
                    int totalSpace = 0;
                    float percentageUsed = 0.0f;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack itemStack = handler.getStackInSlot(i);
                        int maxStackSize = Math.min(handler.getSlotLimit(i), itemStack.getMaxStackSize());

                        storedAmount += handler.getStackInSlot(i).getCount();
                        totalSpace += maxStackSize;
                    }
                    if (totalSpace > 0)
                        percentageUsed = ((float) storedAmount / totalSpace) * 100;

                    beTag.putInt("StoredAmount", storedAmount);
                    beTag.putFloat("PercentageUsed", percentageUsed);
                    BlockItem.setBlockEntityData(stack, ModBlockEntities.STORAGE_BOX_ENTITY.get(), beTag);
                }
            }
        });
    }
}
