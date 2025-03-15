package net.fxnt.fxntstorage.item.upgrades;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.fxnt.fxntstorage.util.KeybindHandler.TOGGLE_JETPACK_HOVER_KEY;

public class UpgradeItem extends Item {
    public final String name;

    public UpgradeItem(Properties pProperties, String name) {
        super(pProperties);
        this.name = name;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public String getUpgradeName() {
        String name = this.getDescriptionId();
        String replaceTarget = "item." + FXNTStorage.MOD_ID + ".";
        return name.replace(replaceTarget, "");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        switch (name) {
            case Util.MAGNET_UPGRADE_DEACTIVATED:
            case Util.PICKBLOCK_UPGRADE_DEACTIVATED:
            case Util.ITEMPICKUP_UPGRADE_DEACTIVATED:
            case Util.FLIGHT_UPGRADE_DEACTIVATED:
            case Util.REFILL_UPGRADE_DEACTIVATED:
            case Util.FEEDER_UPGRADE_DEACTIVATED:
            case Util.TOOLSWAP_UPGRADE_DEACTIVATED:
            case Util.FALLDAMAGE_UPGRADE_DEACTIVATED:
                pTooltipComponents.add(Component.translatable("tooltip.fxntstorage.upgrade_deactivated").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            case Util.MAGNET_UPGRADE:
            case Util.PICKBLOCK_UPGRADE:
            case Util.ITEMPICKUP_UPGRADE:
            case Util.REFILL_UPGRADE:
            case Util.FEEDER_UPGRADE:
            case Util.TOOLSWAP_UPGRADE:
            case Util.FALLDAMAGE_UPGRADE:
            case Util.STORAGE_BOX_VOID_UPGRADE:
            case Util.STORAGE_BOX_CAPACITY_UPGRADE:
                addUpgradeDetails(pTooltipComponents);
                break;
            case Util.FLIGHT_UPGRADE:
                addFlightUpgradeDetails(pTooltipComponents);
                break;
        }
    }

    private void addUpgradeDetails(List<Component> tooltipComponents) {
        if (Objects.equals(name, Util.FLIGHT_UPGRADE_DEACTIVATED)) {
            addFlightUpgradeDetails(tooltipComponents);
            return;
        }

        List<Component> text = new ArrayList<>();
        String placeholder = "";
        String translateKey = ("tooltip." + FXNTStorage.MOD_ID + "." + name).replaceAll("_deactivated$", "");

        text.add(Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY));

        if (Screen.hasShiftDown()) {
            // Add summary component
            text.add(Component.empty());
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
            text.add(Component.empty());

            if (Objects.equals(name.replaceAll("_deactivated$", ""), Util.MAGNET_UPGRADE) ||
                    Objects.equals(name.replaceAll("_deactivated$", ""), Util.ITEMPICKUP_UPGRADE)) {
                placeholder = ConfigManager.CommonConfig.BACKPACK_MAGNET_RANGE.get().toString();
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".exclusion"), FontHelper.Palette.STANDARD_CREATE));
                text.add(Component.empty());
            }

            // Add up to 9 conditions/behaviours
            for (int i = 1; i < 10; i++) {
                if (!I18n.exists(translateKey + ".condition" + i)) break;
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".condition" + i), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable(translateKey + ".behaviour" + i, placeholder),
                        FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
            }

            // Add a final condition/behaviour for toggling the upgrade
            if (!name.equals(Util.STORAGE_BOX_CAPACITY_UPGRADE) && !name.equals(Util.STORAGE_BOX_VOID_UPGRADE)) {
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.condition"), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.behaviour"),
                        FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
                text.add(Component.empty());
            }

            // Finally add any subtext components
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".subtext"), FontHelper.Palette.GRAY_AND_GOLD.highlight(), FontHelper.Palette.GRAY_AND_GOLD.highlight()));
        }

        tooltipComponents.addAll(text);
    }

    private void addFlightUpgradeDetails(List<Component> tooltipComponents) {
        List<Component> text = new ArrayList<>();
        String translateKey = ("tooltip." + FXNTStorage.MOD_ID + "." + name).replaceAll("_deactivated$", "");

        text.add(Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY));
        text.add(Component.translatable("tooltip.fxntstorage.holdForControls", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY));

        if (Screen.hasShiftDown()) {
            // Add summary component
            text.add(Component.empty());
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
            text.add(Component.empty());

            // Add a condition/behaviour for toggling the upgrade
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.condition"), FontHelper.Palette.ALL_GRAY));
            text.addAll(TooltipHelper.cutTextComponent(
                    Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.behaviour"),
                    FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));

            if (!Screen.hasControlDown()) text.add(Component.empty());
        }

        if (Screen.hasControlDown()) {
            String placeholder = TOGGLE_JETPACK_HOVER_KEY.getKey().getDisplayName().getString();
            text.add(Component.empty());
            for (int i = 1; i < 10; i++) {
                if (!I18n.exists(translateKey + ".condition" + i)) break;
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".condition" + i), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable(translateKey + ".behaviour" + i, placeholder, placeholder),
                        FontHelper.Palette.PURPLE.primary(), FontHelper.Palette.PURPLE.highlight(), 1));
            }
        }

        tooltipComponents.addAll(text);
    }

}
