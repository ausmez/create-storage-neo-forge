package net.fxnt.fxntstorage.controller;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public enum StorageInterfaceUnpacking implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos blockPos, BlockState blockState, Direction direction, List<ItemStack> items, @Nullable PackageOrderWithCrafts packageOrderWithCrafts, boolean simulate) {
        BlockEntity targetBE = level.getBlockEntity(blockPos);
        if (targetBE == null) {
            return false;
        } else {
            IItemHandler targetInv = targetBE.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve().orElse(null);
            StorageInterfaceEntity storageInterfaceEntity = ((StorageInterfaceEntity) targetBE);

            if (targetInv == null) {
                return false;
            } else if (!simulate) {
                for (ItemStack itemStack : items) {
                    storageInterfaceEntity.controller.storageNetwork.insertItems(itemStack);
                }

                return true;
            } else {
                List<ItemStorage> emptyBoxes = new ArrayList<>();
                Map<Item, List<ItemStorage>> storageBoxes = new HashMap<>();

                for (StorageNetwork.StorageNetworkItem item : storageInterfaceEntity.controller.storageNetwork.boxes) {
                    Item filterItem = item.simpleStorageBoxEntity.filterItem.getItem();
                    ItemStorage storage = new ItemStorage(
                            item.simpleStorageBoxEntity.getMaxItemCapacity(),
                            item.simpleStorageBoxEntity.getStoredAmount(),
                            item.simpleStorageBoxEntity.hasVoidUpgrade()
                    );

                    if (filterItem == Items.AIR) {
                        emptyBoxes.add(storage);
                    } else {
                        storageBoxes.computeIfAbsent(filterItem, k -> new ArrayList<>()).add(storage);
                    }
                }

                Map<Util.ItemWithNBT, Integer> itemCounts = new HashMap<>();
                for (ItemStack stack : items) {
                    Util.ItemWithNBT key = new Util.ItemWithNBT(stack.getItem(), stack.getTag());
                    itemCounts.merge(key, stack.getCount(), Integer::sum);
                }

                Map<ItemStack, Boolean> condensed = new HashMap<>();
                for (Map.Entry<Util.ItemWithNBT, Integer> entry : itemCounts.entrySet()) {
                    Util.ItemWithNBT key = entry.getKey();
                    Item item = key.item();
                    CompoundTag tag = key.tag();

                    int totalCount = entry.getValue();

                    while (totalCount > 0) {
                        int stackSize = totalCount;
                        ItemStack stack = new ItemStack(item, stackSize);
                        if (tag != null) {
                            stack.setTag(tag.copy());
                        }
                        condensed.put(stack, false);
                        totalCount -= stackSize;
                    }
                }

                for (Map.Entry<ItemStack, Boolean> entry : condensed.entrySet()) {
                    ItemStack itemStack = entry.getKey();
                    boolean storageFound = false;

                    List<ItemStorage> matchingBoxes = storageBoxes.get(itemStack.getItem());
                    if (matchingBoxes != null) {
                        for (ItemStorage box : matchingBoxes) {
                            if (box.voidUpgrade() || box.storedAmount() + itemStack.getCount() <= box.maxCapacity()) {
                                storageFound = true;
                                break;
                            }
                        }
                    }

                    if (ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_FILL_EMPTY.get()) {
                        if (!storageFound && !emptyBoxes.isEmpty()) {
                            ItemStorage box = emptyBoxes.remove(0);
                            if (box.voidUpgrade() || itemStack.getCount() <= box.maxCapacity()) {
                                storageFound = true;
                            }
                        }
                    }

                    entry.setValue(storageFound);
                }

                return condensed.values().stream().allMatch(Boolean::booleanValue);
            }
        }
    }

    private record ItemStorage(int maxCapacity, int storedAmount, boolean voidUpgrade) {}

}
