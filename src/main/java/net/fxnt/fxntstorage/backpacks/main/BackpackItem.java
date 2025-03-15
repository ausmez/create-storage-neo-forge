package net.fxnt.fxntstorage.backpacks.main;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.util.BackpackHandler;
import net.fxnt.fxntstorage.compat.CuriosCompat;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class BackpackItem extends BlockItem {
    private final Block block;

    public BackpackItem(Block pBlock, @NotNull Properties pProperties) {
        super(pBlock, pProperties.stacksTo(1).fireResistant());
        this.block = pBlock;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(@NotNull BlockPos pPos, @NotNull Level pLevel, @Nullable Player pPlayer, @NotNull ItemStack pStack, @NotNull BlockState pState) {
        return super.updateCustomBlockEntityTag(pPos, pLevel, pPlayer, pStack, pState);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (FXNTStorage.curiosLoaded) {
            return new ICapabilityProvider() {
                final LazyOptional<ICurio> curio = LazyOptional.of(() -> new CuriosCompat(stack));

                @Override
                public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return CuriosCapability.ITEM.orEmpty(cap, curio);
                }
            };
        }
        return null;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        if (armorType != EquipmentSlot.CHEST) return false;
        if (FXNTStorage.curiosLoaded) {
            AtomicReference<Boolean> ret = new AtomicReference<>(false);
            CuriosApi.getCuriosInventory((LivingEntity) entity).ifPresent(curiosItemHandler -> {
                curiosItemHandler.getStacksHandler("back").ifPresent(stacksHandler -> {
                    ret.set(stacksHandler.getStacks().getStackInSlot(0).getItem() instanceof BackpackItem);
                });
            });
            return !ret.get();
        }
        return super.canEquip(stack, armorType, entity);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        final Component CTRL_TO_VIEW_CONTENTS = Component.translatable("tooltip.fxntstorage.holdForContents", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY);
        final Component SHIFT_TO_VIEW_SUMMARY = Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY);

        String translationKey = "tooltip.fxntstorage.back_pack";
        int placeholder = ((BackpackBlock) block).maxStackSize;
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
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        AtomicReference<TooltipComponent> ret = new AtomicReference<>(null);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if ((Screen.hasControlDown() && !Screen.hasShiftDown()) || mc.player != null && !mc.player.containerMenu.getCarried().isEmpty()) {
                ret.set(new BackpackTooltip(pStack));
            }
        });
        return Optional.ofNullable(ret.get());
    }

}
