package net.fxnt.fxntstorage.item.upgrades;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.util.KeybindHandler;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.fxnt.fxntstorage.util.KeybindHandler.TOGGLE_JETPACK_HOVER_KEY;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class UpgradeItem extends Item {
    public final String name;

    public UpgradeItem(Properties pProperties, String name) {
        super(pProperties);
        this.name = name;
    }

    public String getUpgradeName() {
        String name = this.getDescriptionId();
        String replaceTarget = "item." + FXNTStorage.MOD_ID + ".";
        return name.replace(replaceTarget, "");
    }

    public String getBaseUpgradeName() {
        String name = this.getDescriptionId();
        return name.replaceAll("^.*?_([^_]+)_.*$", "$1");
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        switch (name) {
            case Util.MAGNET_UPGRADE_DEACTIVATED:
            case Util.PICKBLOCK_UPGRADE_DEACTIVATED:
            case Util.ITEMPICKUP_UPGRADE_DEACTIVATED:
            case Util.FLIGHT_UPGRADE_DEACTIVATED:
            case Util.REFILL_UPGRADE_DEACTIVATED:
            case Util.FEEDER_UPGRADE_DEACTIVATED:
            case Util.TOOLSWAP_UPGRADE_DEACTIVATED:
            case Util.FALLDAMAGE_UPGRADE_DEACTIVATED:
            case Util.OREMINING_UPGRADE_DEACTIVATED:
            case Util.TORCHDEPLOYER_UPGRADE_DEACTIVATED:
            case Util.JUKEBOX_UPGRADE_DEACTIVATED:
            case Util.HEALTH_UPGRADE_DEACTIVATED:
                pTooltipComponents.add(Component.translatable("tooltip.fxntstorage.upgrade_deactivated").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            case Util.MAGNET_UPGRADE:
            case Util.PICKBLOCK_UPGRADE:
            case Util.ITEMPICKUP_UPGRADE:
            case Util.REFILL_UPGRADE:
            case Util.FEEDER_UPGRADE:
            case Util.TOOLSWAP_UPGRADE:
            case Util.FALLDAMAGE_UPGRADE:
            case Util.OREMINING_UPGRADE:
            case Util.TORCHDEPLOYER_UPGRADE:
            case Util.JUKEBOX_UPGRADE:
            case Util.HEALTH_UPGRADE:
            case Util.CRAFTING_UPGRADE:
            case Util.WORKSHOP_UPGRADE:
            case Util.STORAGE_BOX_VOID_UPGRADE:
            case Util.STORAGE_BOX_CAPACITY_UPGRADE:
            case Util.STORAGE_BOX_COMPACTING_UPGRADE:
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

        UpgradeType upgradeType = UpgradeType.fromBaseName(getBaseUpgradeName());
        if (upgradeType != null && upgradeType.isPlayerOnly() && BackpackScreen.isHoveredUpgradeSlotInContraption()) {
            text.add(Component.translatable("tooltip.fxntstorage.upgrade_inactive").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }

        text.add(Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY));

        if (Screen.hasShiftDown()) {
            // Add summary component
            text.add(Component.empty());
            if (name.equals(Util.CRAFTING_UPGRADE) || name.equals(Util.WORKSHOP_UPGRADE)) {
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary1"), FontHelper.Palette.STANDARD_CREATE));
                text.add(Component.empty());
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary2"), FontHelper.Palette.STANDARD_CREATE));
            } else {
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
            }
            text.add(Component.empty());

            if (Objects.equals(name.replaceAll("_deactivated$", ""), Util.MAGNET_UPGRADE) ||
                    Objects.equals(name.replaceAll("_deactivated$", ""), Util.ITEMPICKUP_UPGRADE)) {
                placeholder = ConfigManager.ServerConfig.MAGNET_PULL_RANGE.get().toString();
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".exclusion"), FontHelper.Palette.STANDARD_CREATE));
                text.add(Component.empty());
            }

            if (Objects.equals(name.replaceAll("_deactivated$", ""), Util.OREMINING_UPGRADE)) {
                placeholder = KeybindHandler.ORE_MINE_ANY_BLOCK.getKey().getDisplayName().getString();
            }

            if (Objects.equals(name.replaceAll("_deactivated$", ""), Util.JUKEBOX_UPGRADE)) {
                placeholder = ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.get().toString();
            }

            // Add up to 9 conditions/behaviors
            for (int i = 1; i < 10; i++) {
                if (!I18n.exists(translateKey + ".condition" + i)) break;
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".condition" + i), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable(translateKey + ".behaviour" + i, placeholder),
                        FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
            }

            // Add a final condition/behavior for toggling the upgrade
            if (!name.equals(Util.STORAGE_BOX_CAPACITY_UPGRADE) && !name.equals(Util.STORAGE_BOX_COMPACTING_UPGRADE)
                    && !name.equals(Util.STORAGE_BOX_VOID_UPGRADE) && !name.equals(Util.CRAFTING_UPGRADE)
                    && !name.equals(Util.WORKSHOP_UPGRADE)) {
                text.addAll(TooltipHelper.cutTextComponent(Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.condition"), FontHelper.Palette.ALL_GRAY));
                text.addAll(TooltipHelper.cutTextComponent(
                        Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.behaviour"),
                        FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));
                text.add(Component.empty());
            }

            if (name.equals(Util.STORAGE_BOX_COMPACTING_UPGRADE)) text.add(Component.empty());

            // Finally add any subtext components
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".subtext"), FontHelper.Palette.GRAY_AND_GOLD.highlight(), FontHelper.Palette.GRAY_AND_GOLD.highlight()));
        }

        tooltipComponents.addAll(text);
    }

    private void addFlightUpgradeDetails(List<Component> tooltipComponents) {
        List<Component> text = new ArrayList<>();
        String translateKey = ("tooltip." + FXNTStorage.MOD_ID + "." + name).replaceAll("_deactivated$", "");

        UpgradeType upgradeType = UpgradeType.fromBaseName(getBaseUpgradeName());
        if (upgradeType != null && upgradeType.isPlayerOnly() && BackpackScreen.isHoveredUpgradeSlotInContraption()) {
            text.add(Component.translatable("tooltip.fxntstorage.upgrade_inactive").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }

        text.add(Component.translatable("tooltip.fxntstorage.holdForDescription", (Screen.hasShiftDown()) ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY));
        text.add(Component.translatable("tooltip.fxntstorage.holdForControls", (Screen.hasControlDown()) ? "§fCtrl" : "§7Ctrl").withStyle(ChatFormatting.DARK_GRAY));

        if (Screen.hasShiftDown()) {
            // Add summary component
            text.add(Component.empty());
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".summary"), FontHelper.Palette.STANDARD_CREATE));
            text.add(Component.empty());

            // Add a condition/behavior for toggling the upgrade
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.condition"), FontHelper.Palette.ALL_GRAY));
            text.addAll(TooltipHelper.cutTextComponent(
                    Component.translatable("tooltip." + FXNTStorage.MOD_ID + ".upgrade_item_toggle.behaviour"),
                    FontHelper.Palette.STANDARD_CREATE.primary(), FontHelper.Palette.STANDARD_CREATE.highlight(), 1));

            text.add(Component.empty());
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(translateKey + ".subtext"), FontHelper.Palette.GRAY_AND_GOLD.highlight(), FontHelper.Palette.GRAY_AND_GOLD.highlight()));
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
