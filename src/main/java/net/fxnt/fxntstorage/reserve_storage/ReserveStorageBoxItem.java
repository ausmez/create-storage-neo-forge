package net.fxnt.fxntstorage.reserve_storage;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.backpack.client.tooltip.BackpackTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReserveStorageBoxItem extends BlockItem {
    public ReserveStorageBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);

        final Component ctrlToViewContents = Component.translatable("tooltip.fxntstorage.holdForContents", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY);
        final Component shiftToViewSummary = Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY);

        if (Screen.hasShiftDown() == Screen.hasControlDown()) {
            pTooltipComponents.addLast(shiftToViewSummary);
            pTooltipComponents.addLast(ctrlToViewContents);
        }

        if (Screen.hasShiftDown() && !Screen.hasControlDown()) {
            pTooltipComponents.add(shiftToViewSummary);
            addUpgradeDetails(pTooltipComponents);
        }

        if (!Screen.hasShiftDown() && Screen.hasControlDown()) {
            pTooltipComponents.add(ctrlToViewContents);
        }
    }

    private void addUpgradeDetails(List<Component> tooltipComponents) {
        List<Component> text = new ArrayList<>();
        String translateKey = getDescriptionId() + ".tooltip";

        if (Screen.hasShiftDown()) {
            // Add summary component
            text.add(Component.empty());
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
            text.add(Component.empty());

            // Add up to 9 conditions/behaviors
            for (int i = 1; i < 10; i++) {
                if (!I18n.exists(translateKey + ".condition" + i)) break;
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".condition" + i), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable(translateKey + ".behaviour" + i),
                        FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
            }
        }
        tooltipComponents.addAll(text);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            return Optional.of(new BackpackTooltip(stack));
        }
        return Optional.empty();
    }
}
