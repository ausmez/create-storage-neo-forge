package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.backpack.tooltip.BackpackTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

public class SimpleStorageBoxItem extends BlockItem {

    public SimpleStorageBoxItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(Component.translatable("tooltip.fxntstorage.holdForContents", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown()) {
            return Optional.of(new BackpackTooltip(stack));
        }
        return Optional.empty();
    }

}
