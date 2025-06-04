package net.fxnt.fxntstorage.backpack;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.tooltip.BackpackTooltip;
import net.fxnt.fxntstorage.backpack.util.BackpackHandler;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class BackpackItem extends BlockItem {
    private final Block block;

    public BackpackItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1).fireResistant());
        this.block = block;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        if (armorType != EquipmentSlot.CHEST) return false;
        if (FXNTStorage.curiosLoaded) {
            AtomicReference<Boolean> ret = new AtomicReference<>(false);
            CuriosApi.getCuriosInventory(entity)
                    .flatMap(curiosItemHandler -> curiosItemHandler.getStacksHandler("back"))
                    .ifPresent(stacksHandler -> ret.set(stacksHandler.getStacks().getStackInSlot(0).getItem() instanceof BackpackItem));
            return !ret.get();
        }
        return super.canEquip(stack, armorType, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BackpackHandler.openBackpackFromInventory(serverPlayer, Util.BACKPACK_IN_HAND);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
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
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);

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
            pTooltipComponents.add(SHIFT_TO_VIEW_SUMMARY);
            pTooltipComponents.add(CTRL_TO_VIEW_CONTENTS);
        }

        if (Screen.hasShiftDown() && !Screen.hasControlDown()) {
            pTooltipComponents.add(SHIFT_TO_VIEW_SUMMARY);
            pTooltipComponents.addAll(summaryLines);
        }

        if (!Screen.hasShiftDown() && Screen.hasControlDown()) {
            pTooltipComponents.add(CTRL_TO_VIEW_CONTENTS);
        }

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            return Optional.of(new BackpackTooltip(pStack));
        }
        return Optional.empty();
    }

}
