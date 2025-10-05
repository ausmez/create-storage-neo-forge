package net.fxnt.fxntstorage.backpack;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.tooltip.BackpackTooltip;
import net.fxnt.fxntstorage.backpack.util.BackpackHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.compat.CuriosCompat;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class BackpackItem extends BlockItem {
    private final Block block;

    public BackpackItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties.stacksTo(1).fireResistant());
        this.block = pBlock;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, @Nullable Player pPlayer, ItemStack pStack, BlockState pState) {
        return super.updateCustomBlockEntityTag(pPos, pLevel, pPlayer, pStack, pState);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            // Curios capability
            final LazyOptional<?> curio = FXNTStorage.curiosLoaded
                    ? LazyOptional.of(() -> new CuriosCompat(stack))
                    : LazyOptional.empty();

            // Item Handler capability
            final ItemStackHandler itemHandler = new ItemStackHandler() {
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
            final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

            {
                // Load existing NBT from the ItemStack on init
                CompoundTag beTag = stack.getOrCreateTag().getCompound("BlockEntityTag");
                if (beTag.contains("Items", Tag.TAG_COMPOUND)) {
                    CompoundTag itemsTag = beTag.getCompound("Items");

                    int size = (itemsTag.contains("Size")) ? itemsTag.getInt("Size") : itemsTag.size();
                    itemHandler.setSize(size);
                    itemHandler.deserializeNBT(itemsTag);
                }
            }

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                if (FXNTStorage.curiosLoaded && cap == CuriosCapability.ITEM)
                    return curio.cast();

                if (cap == ForgeCapabilities.ITEM_HANDLER)
                    return itemHandlerCap.cast();

                return LazyOptional.empty();
            }
        };
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        if (armorType != EquipmentSlot.CHEST) return false;
        if (stack.getItem() instanceof BackpackItem
                && ((LivingEntity) entity).getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem) return true; // Allow backpack swap
        if (FXNTStorage.curiosLoaded) {
            return !BackpackHelper.isWearingBackpack((Player) entity);
        }
        return super.canEquip(stack, armorType, entity);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BackpackHandler.openBackpackFromInventory(serverPlayer, Util.BACKPACK_IN_HAND);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        final Component CTRL_TO_VIEW_CONTENTS = Component.translatable("tooltip.fxntstorage.holdForContents", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY);
        final Component SHIFT_TO_VIEW_SUMMARY = Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY);

        String translationKey = "tooltip.fxntstorage.backpack";
        int placeholder = ((BackpackBlock) block).stackMultiplier;
        List<Component> summaryLines = new ArrayList<>();

        summaryLines.add(Component.empty());
        summaryLines.addAll(TooltipHelper.cutTextComponent(Component.translatable(translationKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
        summaryLines.add(Component.empty());
        for (int i = 1; i < 10; i++) {
            if (!I18n.exists(translationKey + ".condition" + i)) break;
            summaryLines.addAll(TooltipHelper.cutTextComponent(Component.translatable(translationKey + ".condition" + i), FontHelper.Palette.ALL_GRAY));
            summaryLines.addAll(TooltipHelper.cutTextComponent(Component.translatable(translationKey + ".behaviour" + i, placeholder),
                    FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
        }

        if (Screen.hasShiftDown() == Screen.hasControlDown()) {
            tooltipComponents.add(SHIFT_TO_VIEW_SUMMARY);
            tooltipComponents.add(CTRL_TO_VIEW_CONTENTS);
        }

        if (Screen.hasShiftDown() && !Screen.hasControlDown()) {
            tooltipComponents.add(SHIFT_TO_VIEW_SUMMARY);
            tooltipComponents.addAll(summaryLines);
        }

        if (!Screen.hasShiftDown() && Screen.hasControlDown()) {
            tooltipComponents.add(CTRL_TO_VIEW_CONTENTS);
        }

    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            return Optional.of(new BackpackTooltip(pStack));
        }
        return Optional.empty();
    }

}
