package net.fxnt.fxntstorage.backpack.upgrade.toolswap;

import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.WeakHashMap;

public class ToolSwapHandler {
    private final Player player;
    private final IBackpackContainer container;
    private final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

    public ToolSwapHandler(Player player, IBackpackContainer container) {
        this.player = player;
        this.container = container;
    }

    private record ToolInfo(ItemStack itemStack, int slot, double speed, int durability, boolean silkTouch) {
    }

    private record WeaponInfo(ItemStack itemStack, int slot, int durability, double damage) {
    }

    private record SwapState(BlockState lastBlockState, ItemStack lastTool, LivingEntity lastEntity,
                             ItemStack lastWeapon) {
        static final SwapState EMPTY = new SwapState(null, null, null, null);
    }

    private static final WeakHashMap<Player, SwapState> playerState = new WeakHashMap<>();

    public void doToolSwap(@Nullable BlockPos blockPos, @Nullable LivingEntity entity) {
        SwapState state = playerState.getOrDefault(player, SwapState.EMPTY);
        if (blockPos != null && entity == null) {
            ItemStack currentItem = player.getMainHandItem();
            BlockState blockState = player.level().getBlockState(blockPos);

            if (blockState != state.lastBlockState || currentItem != state.lastTool) {
                // Avoid swapping if the best tool is already selected
                ToolInfo bestTool = findBestToolForBreakingBlock(blockState);
                if (bestTool != null && !bestTool.itemStack.equals(currentItem)) {
                    swapBlockTool(bestTool);
                }
            }
            playerState.put(player, new SwapState(blockState, currentItem, state.lastEntity, state.lastWeapon));
        }

        if (blockPos == null && entity != null) {
            ItemStack currentItem = player.getMainHandItem();
            if (entity != state.lastEntity || currentItem != state.lastWeapon) {
                WeaponInfo bestWeapon = findBestWeaponForAttackingEntity(entity);
                if (bestWeapon != null && !bestWeapon.itemStack.equals(currentItem)) {
                    swapEntityTool(bestWeapon);
                }
            }
            playerState.put(player, new SwapState(state.lastBlockState, state.lastTool, entity, currentItem));
        }
    }

    private void swapBlockTool(ToolInfo bestTool) {
        if (bestTool.itemStack().isEmpty()) return;

        ItemStack current = player.getMainHandItem();
        if (ItemStack.isSameItemSameComponents(current, bestTool.itemStack)) return;

        player.setItemInHand(InteractionHand.MAIN_HAND, bestTool.itemStack.copy());
        container.getItemHandler().setStackInSlot(bestTool.slot(), current.copy());
        container.setDataChanged();
    }

    private @Nullable ToolInfo findBestToolForBreakingBlock(BlockState blockState) {
        NonNullList<ToolInfo> suitableItems = NonNullList.create();

        boolean requiresSilkTouch = requiresSilkTouch(blockState);
        boolean prefersSilkTouch = prefersSilkTouch(blockState);
        boolean needsSilkTouch = requiresSilkTouch || prefersSilkTouch;

        // Is player's current tool good enough?
        ItemStack currentTool = player.getMainHandItem();
        boolean canUseAnyTool = canUseAnyToolForDrops(blockState);
        boolean currentToolIsCorrect = currentTool.isCorrectToolForDrops(blockState) || canUseAnyTool;
        boolean currentToolHasSilkTouch = currentTool.getEnchantmentLevel(player.level().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH)) > 0;
        boolean currentToolMeetsSilkTouchRequirement = currentToolIsCorrect && (!needsSilkTouch || currentToolHasSilkTouch);

        if (currentToolMeetsSilkTouchRequirement && canUseAnyTool) {
            return null; // Current tool is already sufficient
        }

        // If the block can be broken with any tool, add the current tool to the suitable items list
        if (canUseAnyTool || currentToolIsCorrect) {
            suitableItems.add(new ToolInfo(currentTool, player.getInventory().selected,
                    currentTool.getDestroySpeed(blockState), currentTool.getDamageValue(), currentToolHasSilkTouch));
        }

        // Add suitable tools from the player's inventory
        for (int i : layout.tools().range()) {
            ItemStack itemStack = container.getItemHandler().getStackInSlot(i);
            boolean hasSilkTouch = itemStack.getEnchantmentLevel(player.level().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH)) > 0;
            boolean isItemTool = itemStack.is(Tags.Items.TOOLS) || itemStack.is(Tags.Items.TOOLS_SHEAR);

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

    private void swapEntityTool(WeaponInfo bestWeapon) {
        if (bestWeapon.itemStack.isEmpty()) return;

        ItemStack current = player.getMainHandItem();
        if (ItemStack.isSameItemSameComponents(current, bestWeapon.itemStack())) return;

        player.setItemInHand(InteractionHand.MAIN_HAND, bestWeapon.itemStack.copy());
        container.getItemHandler().setStackInSlot(bestWeapon.slot, current.copy());
        container.setDataChanged();

    }

    private WeaponInfo findBestWeaponForAttackingEntity(LivingEntity entity) {
        NonNullList<WeaponInfo> suitableItems = NonNullList.create();
        boolean preferSwords = container.getUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD);

        ItemStack currentItem = player.getInventory().getSelected();
        double currentItemDamage = getAttackDamage(currentItem, entity);
        // Add current weapon to list
        suitableItems.add(new WeaponInfo(currentItem, player.getInventory().selected, currentItem.getDamageValue(), currentItemDamage));

        for (int i : layout.tools().range()) {
            ItemStack itemStack = container.getItemHandler().getStackInSlot(i);

            boolean isSword = itemStack.is(ItemTags.SWORDS) || itemStack.canPerformAction(ItemAbilities.SWORD_SWEEP);
            boolean isAxe = itemStack.is(ItemTags.AXES);

            if (!isSword && !isAxe) continue;

            double itemDamage = getAttackDamage(itemStack, entity);
            int durability = itemStack.getDamageValue();

            if (!preferSwords && itemDamage <= currentItemDamage)
                continue;

            suitableItems.add(new WeaponInfo(itemStack, i, durability, itemDamage));
        }

        if (suitableItems.isEmpty())
            return new WeaponInfo(ItemStack.EMPTY, -1, 0, 0);

        sortWeapons(suitableItems, preferSwords);
        return suitableItems.getFirst();
    }

    private static double getAttackDamage(ItemStack weapon, LivingEntity target) {
        double baseDamage = getBaseAttackDamage(weapon);
        double enchantmentDamage = calculateEnchantmentDamage(weapon, target);
        return baseDamage + enchantmentDamage;
    }

    @SuppressWarnings("deprecation")
    private static double getBaseAttackDamage(ItemStack itemStack) {
        // Base (1) + attack damage modifier
        return 1 + itemStack.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.attribute().is(Attributes.ATTACK_DAMAGE))
                .mapToDouble(e -> e.modifier().amount())
                .findFirst()
                .orElse(0);
    }

    private static double calculateEnchantmentDamage(ItemStack weapon, LivingEntity target) {
        int sharpnessLevel = weapon.getEnchantmentLevel(target.level().registryAccess().holderOrThrow(Enchantments.SHARPNESS));
        int smiteLevel = weapon.getEnchantmentLevel(target.level().registryAccess().holderOrThrow(Enchantments.SMITE));
        int baneLevel = weapon.getEnchantmentLevel(target.level().registryAccess().holderOrThrow(Enchantments.BANE_OF_ARTHROPODS));

        double additionalDamage = 0.0;

        // Sharpness adds 1 extra damage for the first level, and 0.5 for each additional level.
        if (sharpnessLevel > 0) {
            additionalDamage += 0.5 * sharpnessLevel + 0.5;
        }

        // Check if the target is undead for Smite.
        if (smiteLevel > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            additionalDamage += smiteLevel * 2.5;  // Each level adds 2.5 extra damage.
        }

        // Check if the target is an arthropod for Bane of Arthropods.
        if (baneLevel > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            additionalDamage += baneLevel * 2.5;  // Each level adds 2.5 extra damage.
        }

        return additionalDamage;
    }

    private static void sortTools(@NotNull NonNullList<ToolInfo> tools, boolean sortSilkTouchFirst) {
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

    private static void sortWeapons(@NotNull NonNullList<WeaponInfo> tools, boolean preferSwords) {
        tools.sort((o1, o2) -> {
            boolean o1IsSword = o1.itemStack().is(ItemTags.SWORDS);
            boolean o2IsSword = o2.itemStack().is(ItemTags.SWORDS);
            boolean o1IsAxe = o1.itemStack().is(ItemTags.AXES);
            boolean o2IsAxe = o2.itemStack().is(ItemTags.AXES);

            if (preferSwords && o1IsSword != o2IsSword) {
                return o1IsSword ? -1 : 1;
            }

            if (!preferSwords && o1IsAxe != o2IsAxe) {
                return o1IsAxe ? -1 : 1;
            }

            // Compare by damage - highest damage dealt first
            int damageComparison = Double.compare(o2.damage, o1.damage);
            if (damageComparison != 0) {
                return damageComparison;
            }
            // Compare by durability - prefer damaged weapon first
            return Integer.compare(o2.durability, o1.durability);
        });
    }

    private boolean requiresSilkTouch(BlockState blockState) {
        // Blocks that MUST have a tool with Silk Touch in order to drop
        Block block = blockState.getBlock();
        return block == Blocks.BEE_NEST || blockState.is(BlockTags.CORALS) || block == Blocks.AMETHYST_CLUSTER
                || block == Blocks.CHISELED_BOOKSHELF || blockState.is(Tags.Blocks.GLASS_BLOCKS) || blockState.is(Tags.Blocks.GLASS_PANES)
                || blockState.is(BlockTags.ICE) || block == Blocks.TURTLE_EGG;
    }

    private boolean prefersSilkTouch(BlockState state) {
        // List of blocks that the player has specified to use Silk Touch in order to drop the block itself
        if (container.getUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH)) {
            List<String> blockList = ClientSettings.getList(player.getUUID(), "PrefersSilkTouchList");
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

            if (!blockList.isEmpty())
                return blockList.contains(blockId.toString());
        }
        return false;
    }

    private boolean canUseAnyToolForDrops(BlockState state) {
        return state.is(ModTags.Blocks.BREAKABLE_WITH_ANY_TOOL);
    }
}
