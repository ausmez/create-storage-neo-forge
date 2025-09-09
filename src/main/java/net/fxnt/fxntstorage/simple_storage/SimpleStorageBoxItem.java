package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.backpack.tooltip.BackpackTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.SLOT_COUNT;

public class SimpleStorageBoxItem extends BlockItem {

    public SimpleStorageBoxItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
        pTooltip.add(Component.translatable("tooltip.fxntstorage.holdForContents", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            return Optional.of(new BackpackTooltip(pStack));
        }
        return Optional.empty();
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final ItemStackHandler handler = new ItemStackHandler(SLOT_COUNT) {
                @Override
                public CompoundTag serializeNBT() {
                    ListTag nbtTagList = new ListTag();
                    for (int i = 0; i < stacks.size(); i++) {
                        if (!stacks.get(i).isEmpty()) {
                            CompoundTag itemTag = new CompoundTag();
                            itemTag.putInt("Slot", i);
                            itemTag.putInt("ActualCount", stacks.get(i).getCount());
                            stacks.get(i).save(itemTag);
                            nbtTagList.add(itemTag);
                        }
                    }

                    CompoundTag nbt = new CompoundTag();
                    nbt.put("Items", nbtTagList);
                    nbt.putInt("Size", stacks.size());

                    stack.getOrCreateTag()
                            .getCompound("BlockEntityTag")
                            .put("Items", nbt);

                    return nbt;
                }

                @Override
                public void deserializeNBT(CompoundTag nbt) {
                    setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
                    ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
                    for (int i = 0; i < tagList.size(); i++) {
                        CompoundTag itemTags = tagList.getCompound(i);
                        int slot = itemTags.getInt("Slot");
                        ItemStack slotStack = ItemStack.of(itemTags);
                        if (itemTags.contains("ActualCount", Tag.TAG_INT)) {
                            slotStack.setCount(itemTags.getInt("ActualCount"));
                        }
                        stacks.set(slot, slotStack);
                    }
                    onLoad();
                }
            };

            private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> handler);

            {
                // Load existing NBT from the ItemStack on init
                CompoundTag beTag = stack.getOrCreateTag().getCompound("BlockEntityTag");
                if (beTag.contains("Items", Tag.TAG_COMPOUND)) {
                    handler.deserializeNBT(beTag.getCompound("Items"));
                }
            }

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ITEM_HANDLER ? itemHandler.cast() : LazyOptional.empty();
            }
        };
    }

}
