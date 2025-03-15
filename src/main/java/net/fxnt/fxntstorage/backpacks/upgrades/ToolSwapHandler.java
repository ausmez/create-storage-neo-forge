package net.fxnt.fxntstorage.backpacks.upgrades;

import net.fxnt.fxntstorage.backpacks.main.IBackpackContainer;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ToolSwapHandler {
    private final Player player;
    private final IBackpackContainer container;
    private final int startIndex;
    private final int endIndex;
    private static BlockState lastBlockState;
    private static ItemStack lastTool;
    private static LivingEntity lastEntity;
    private static ItemStack lastWeapon;

    public ToolSwapHandler(Player player, IBackpackContainer container, int startIndex, int endIndex) {
        this.player = player;
        this.container = container;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public record ToolInfo(ItemStack itemStack, int slot, double speed, int durability, boolean silkTouch) {
    }

    public record WeaponInfo(ItemStack itemStack, int slot, int durability, double damage) {
    }

    public void doToolSwap(BlockPos blockPos, LivingEntity entity) {
        if (blockPos != null && entity == null) {
            ItemStack currentItem = player.getMainHandItem();
            BlockState blockState = player.level().getBlockState(blockPos);

            if (blockState != lastBlockState || currentItem != lastTool) {
                // Avoid swapping if the best tool is already selected
                ToolInfo bestTool = findBestToolForBreakingBlock(blockState);
//                FXNTStorage.LOGGER.debug("bestTool={}", bestTool);
                if (bestTool != null && !bestTool.itemStack.equals(currentItem)) {
                    swapBlockTool(bestTool);
                }
            }
            lastBlockState = blockState;
            lastTool = currentItem;
        }

        if (entity != null && blockPos == null) {
            ItemStack currentItem = player.getMainHandItem();
            if (entity != lastEntity || currentItem != lastWeapon) {
                WeaponInfo bestWeapon = findBestWeaponForAttackingEntity(entity);
                if (bestWeapon != null && !bestWeapon.itemStack.equals(currentItem)) {
                    swapEntityTool(bestWeapon);
                }
            }
            lastEntity = entity;
            lastWeapon = currentItem;
        }
    }

    public void swapBlockTool(ToolInfo bestTool) {
        if (!bestTool.itemStack.isEmpty()) {
            int selectedSlot = player.getInventory().selected;
            if (selectedSlot != bestTool.slot) {
                ItemStack selectedStack = player.getInventory().getSelected().copy();

                if (!selectedStack.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, bestTool.itemStack.copy());
                    container.getItemHandler().setStackInSlot(bestTool.slot, selectedStack.copy());
                    container.setDataChanged();
                }
            }
        }
    }

    private ToolInfo findBestToolForBreakingBlock(BlockState blockState) {
        NonNullList<ToolInfo> suitableItems = NonNullList.create();

        boolean requiresSilkTouch = requiresSilkTouch(blockState);
        boolean prefersSilkTouch = prefersSilkTouch(blockState);
        boolean needsSilkTouch = requiresSilkTouch || prefersSilkTouch;

        // Is player's current tool good enough?
        ItemStack currentTool = player.getMainHandItem();
        boolean canUseAnyTool = canUseAnyToolForDrops(blockState);
        boolean currentToolIsCorrect = currentTool.isCorrectToolForDrops(blockState) || canUseAnyTool;
        boolean currentToolHasSilkTouch = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SILK_TOUCH, currentTool) > 0;
        boolean currentToolMeetsSilkTouchRequirement = currentToolIsCorrect && (!needsSilkTouch || currentToolHasSilkTouch);

        if (currentToolMeetsSilkTouchRequirement && !canUseAnyTool) {
            return null; // Current tool is already sufficient
        }

        // If the block can be broken with any tool, add the current tool to the suitable items list
        if (canUseAnyTool) {
            suitableItems.add(new ToolInfo(currentTool, player.getInventory().selected,
                    currentTool.getDestroySpeed(blockState), currentTool.getDamageValue(), currentToolHasSilkTouch));
        }

        // Add suitable tools from the player's inventory
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack itemStack = container.getItemHandler().getStackInSlot(i);
            boolean hasSilkTouch = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) > 0;
            boolean isItemTool = itemStack.is(Tags.Items.TOOLS) || itemStack.is(Tags.Items.SHEARS);

            // If the block can be broken with any tool, add all tools to determine the fastest
            if (canUseAnyTool && isItemTool) {
                double speed = itemStack.getDestroySpeed(blockState);
                int durability = itemStack.getDamageValue();

                suitableItems.add(new ToolInfo(itemStack, i, speed, durability, hasSilkTouch));
            }
            // Otherwise, check if the tool is the correct one for the block
            else if (itemStack.isCorrectToolForDrops(blockState)) {
                double speed = itemStack.getDestroySpeed(blockState);
                int durability = itemStack.getDamageValue();

                suitableItems.add(new ToolInfo(itemStack, i, speed, durability, hasSilkTouch));
            }
        }

        if (suitableItems.isEmpty()) {
            return new ToolInfo(ItemStack.EMPTY, -1, 0, 0, false);
        }

        sortTools(suitableItems, needsSilkTouch);
        for (ToolInfo tool : suitableItems) {
            if (suitableItems.size() == 1) return tool;
            if (tool.silkTouch && prefersSilkTouch) return tool;
            if (tool.silkTouch && requiresSilkTouch) return tool;
            if (!tool.silkTouch) return tool;
        }
        return null;
    }

    public void swapEntityTool(WeaponInfo bestWeapon) {
        if (!bestWeapon.itemStack.isEmpty()) {
            int selectedSlot = player.getInventory().selected;
            if (selectedSlot != bestWeapon.slot) {
                ItemStack selectedStack = player.getInventory().getSelected().copy();

                if (!selectedStack.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, bestWeapon.itemStack.copy());
                    container.getItemHandler().setStackInSlot(bestWeapon.slot, selectedStack.copy());
                    container.setDataChanged();
                }
            }
        }
    }

    public WeaponInfo findBestWeaponForAttackingEntity(LivingEntity entity) {
        NonNullList<WeaponInfo> suitableItems = NonNullList.create();

        // Is player's current tool good enough?
        ItemStack currentItem = player.getInventory().getSelected();
        double currentItemDamage = getAttackDamage(currentItem, entity);

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack itemStack = container.getItemHandler().getStackInSlot(i);
            if (itemStack.canPerformAction(ToolActions.SWORD_SWEEP) || itemStack.is(ItemTags.SWORDS) || itemStack.is(ItemTags.AXES)) {
                double itemDamage = getAttackDamage(itemStack, entity);
                int durability = itemStack.getDamageValue();
                if (itemDamage > currentItemDamage) {
                    suitableItems.add(new WeaponInfo(itemStack, i, durability, itemDamage));
                }
            }
        }
        if (suitableItems.isEmpty()) return new WeaponInfo(ItemStack.EMPTY, -1, 0, 0);

        sortWeapons(suitableItems);
        return suitableItems.get(0);
    }

    public static double getAttackDamage(ItemStack weapon, LivingEntity target) {
        double baseDamage = getBaseAttackDamage(weapon);
        double enchantmentDamage = calculateEnchantmentDamage(weapon, target);
        return baseDamage + enchantmentDamage;
    }

    public static double getBaseAttackDamage(ItemStack itemStack) {
        return itemStack.getItem().getAttributeModifiers(EquipmentSlot.MAINHAND, itemStack).get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    public static double calculateEnchantmentDamage(ItemStack weapon, LivingEntity target) {
        int sharpnessLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        int smiteLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SMITE, weapon);
        int baneLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, weapon);

        double additionalDamage = 0.0;

        // Sharpness adds 1 extra damage for the first level, and 0.5 for each additional level.
        if (sharpnessLevel > 0) {
            additionalDamage += 0.5 * sharpnessLevel + 0.5;
        }

        // Check if the target is undead for Smite.
        if (smiteLevel > 0 && target.getMobType() == MobType.UNDEAD) {
            additionalDamage += smiteLevel * 2.5;  // Each level adds 2.5 extra damage.
        }

        // Check if the target is an arthropod for Bane of Arthropods.
        if (baneLevel > 0 && target.getMobType() == MobType.ARTHROPOD) {
            additionalDamage += baneLevel * 2.5;  // Each level adds 2.5 extra damage.
        }

        return additionalDamage;
    }

    public static void sortTools(@NotNull NonNullList<ToolInfo> tools, boolean sortSilkTouchFirst) {
        tools.sort((o1, o2) -> {
            // If sorting by Silk Touch preference
            if (sortSilkTouchFirst) {
                // Compare by Silk Touch first (true comes before false)
                int silkTouchComparison = Boolean.compare(o2.silkTouch, o1.silkTouch);
                if (silkTouchComparison != 0) {
                    return silkTouchComparison;
                }
            }

            // If Silk Touch is either not a factor or after sorting by Silk Touch, compare by speed - higher speed first
            int speedComparison = Double.compare(o2.speed, o1.speed);
            if (speedComparison != 0) {
                return speedComparison;
            }

            // If speed is the same, compare by slot - lower slot number first
            return Integer.compare(o1.slot, o2.slot);
        });
    }

    public static void sortWeapons(@NotNull NonNullList<WeaponInfo> tools) {
        tools.sort((o1, o2) -> {
            // Compare by damage - highest damage dealt first
            int damageComparison = Double.compare(o2.damage, o1.damage);
            if (damageComparison != 0) {
                return damageComparison;
            }
            // Compare by durability - prefer damaged weapon first
            return Double.compare(o2.durability, o1.durability);
        });
    }

    public boolean requiresSilkTouch(BlockState blockState) {
        // Blocks that MUST have a tool with Silk Touch in order to drop
        Block block = blockState.getBlock();
        return block == Blocks.BEE_NEST || blockState.is(BlockTags.CORALS) || block == Blocks.AMETHYST_CLUSTER
                || block == Blocks.CHISELED_BOOKSHELF || blockState.is(Tags.Blocks.GLASS) || blockState.is(Tags.Blocks.GLASS_PANES)
                || blockState.is(BlockTags.ICE) || block == Blocks.TURTLE_EGG;
    }

    public boolean prefersSilkTouch(BlockState state) {
        // List of blocks that the player has specified to use Silk Touch in order to drop the block itself
        CompoundTag pData = player.getPersistentData();

        if (pData.contains("fxntPreferSilkTouch") && pData.getBoolean("fxntPreferSilkTouch")) {
            if (pData.contains("fxntPrefersSilkTouchList")) {
                ListTag blockListTag = pData.getList("fxntPrefersSilkTouchList", Tag.TAG_STRING);
                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                List<String> blockList = new ArrayList<>();
                for (int i = 0; i < blockListTag.size(); i++) {
                    blockList.add(blockListTag.getString(i));
                }

                if (blockId != null && !blockList.isEmpty())
                    return blockList.contains(blockId.toString());
            }
            return false;
        }
        return false;
    }

    public boolean canUseAnyToolForDrops(BlockState state) {
        return state.is(ModTags.Blocks.BREAKABLE_WITH_ANY_TOOL);
    }
}
