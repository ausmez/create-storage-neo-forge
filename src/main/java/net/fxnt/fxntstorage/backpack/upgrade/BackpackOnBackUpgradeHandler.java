package net.fxnt.fxntstorage.backpack.upgrade;

import com.mojang.datafixers.util.Pair;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BackpackOnBackUpgradeHandler {

    public Player player;
    private final BackpackHelper helper;
    private final int magnetUpgradeRange = ConfigManager.CommonConfig.BACKPACK_MAGNET_RANGE.get();
    private final ItemStack itemStack;

    public BackpackOnBackUpgradeHandler(Player player) {
        this.player = player;
        this.helper = new BackpackHelper();
        this.itemStack = BackpackHelper.getEquippedBackpackStack(player);
    }

    public boolean hasUpgrade(String upgradeName) {
        if (this.itemStack.isEmpty()) return false;
        CompoundTag tag = this.itemStack.getTagElement("BlockEntityTag");
        if (tag != null) {
            if (tag.contains("Upgrades")) {
                ListTag upgradesList = tag.getList("Upgrades", Tag.TAG_STRING);
                for (Tag upgrade : upgradesList) {
                    if (upgrade.getAsString().equals(upgradeName)) return true;
                }
            }
        }
        return false;
    }

    private IBackpackContainer getContainer() {
        if (player.containerMenu instanceof BackpackMenu backPackMenu && backPackMenu.backpackType == Util.BACKPACK_ON_BACK) {
            return backPackMenu.container;
        } else {
            return new BackpackContainer(this.itemStack, this.player);
        }
    }

    // SERVER SIDE
    public void applyMagnetUpgrade() {
        if (this.itemStack.isEmpty() || this.player.level().isClientSide || !hasUpgrade(Util.MAGNET_UPGRADE)) return;

        // Define the bounding box around the center position
        AABB boundingBox = new AABB(this.player.blockPosition()).inflate(magnetUpgradeRange);

        // Retrieve all item entities within the range
        List<ItemEntity> nearbyItems = this.player.level().getEntitiesOfClass(ItemEntity.class, boundingBox);

        if (!nearbyItems.isEmpty()) {
            for (ItemEntity itemEntity : nearbyItems) {
                if (itemEntity.getItem().getItem() instanceof BackpackItem) continue;

                CompoundTag pd = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);
                if (pd.contains("IgnoreFanProcessing") && pd.getBoolean("IgnoreFanProcessing")) {
                    CompoundTag nbt = itemEntity.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);
                    if (nbt.contains("CreateData")) {
                        CompoundTag createData = nbt.getCompound("CreateData");
                        if (createData.contains("Processing")) {
                            CompoundTag processing = createData.getCompound("Processing");
                            if (processing.contains("Time") && processing.getInt("Time") > 0) continue;
                        }
                    }
                }

                this.helper.itemEntityToBackPack(getContainer(), itemEntity, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE);
            }
        }
    }

    // SERVER SIDE
    public boolean applyItemPickupUpgrade(ItemEntity itemEntity, UUID target, int pickupDelay) {
        if (this.itemStack.isEmpty() || this.player.level().isClientSide || !hasUpgrade(Util.ITEMPICKUP_UPGRADE) || hasUpgrade(Util.MAGNET_UPGRADE))
            return false;
        ItemStack itemStack = itemEntity.getItem();
        Item item = itemStack.getItem();
        int i = itemStack.getCount();
        if (pickupDelay == 0 && (target == null || target.equals(player.getUUID())) &&
                this.helper.itemEntityToBackPack(getContainer(), itemEntity, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE)) {

            player.take(itemEntity, i);
            if (itemStack.isEmpty()) {
                itemEntity.discard();
                itemStack.setCount(i);
            }
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
            player.onItemPickup(itemEntity);
            return true;
        }
        return false;
    }

    // SERVER SIDE
    public void applyPickBlockUpgrade(ItemStack pickedStack) {
        if (this.itemStack.isEmpty() || this.player.level().isClientSide || !hasUpgrade(Util.PICKBLOCK_UPGRADE)) return;
        PickBlockHandler.pickBlockHandler(player, getContainer(), pickedStack);
    }

    // SERVER SIDE
    public void applyFeederUpgrade() {
        if (this.itemStack.isEmpty() || this.player.level().isClientSide || !hasUpgrade(Util.FEEDER_UPGRADE)) return;
        boolean doFeed = isDoFeed();

        if (doFeed) {
            // Look for food in backpack
            IBackpackContainer container = getContainer();
            IItemHandlerModifiable itemHandler = container.getItemHandler();

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack food = itemHandler.getStackInSlot(i);
                if (!food.isEdible() || hasNegativeEffects(food)) continue;

                String foodName = food.getItem().getName(food).getString();

                // Stash MainHandItem and place food item in Main Hand
                ItemStack mainHandItem = player.getMainHandItem();
                player.getInventory().items.set(player.getInventory().selected, food);

                ItemStack singleItem = food.copyWithCount(1);

                // Use the food item and check if it was consumed
                if (singleItem.use(player.level(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
                    player.getInventory().items.set(player.getInventory().selected, mainHandItem);
                    food.shrink(1);
                    itemHandler.setStackInSlot(i, food);

                    ItemStack remainder = ForgeEventFactory.onItemUseFinish(player, singleItem.copy(), 0, singleItem.getItem().finishUsingItem(singleItem, player.level(), player));
                    if (!remainder.isEmpty()) {
                        boolean itemPlaced = false;
                        int firstEmptyStack = -1;

                        for (int j = 0; j < itemHandler.getSlots(); j++) {
                            ItemStack stack = itemHandler.getStackInSlot(j);

                            if (stack.isEmpty() && firstEmptyStack < 0) {
                                firstEmptyStack = j;
                            }
                            if ((ItemStack.isSameItemSameTags(stack, remainder) && stack.getCount() < container.getStackMultiplier() * remainder.getMaxStackSize())) {
                                ItemStack insertResult = itemHandler.insertItem(j, remainder, false);
                                if (!insertResult.isEmpty()) {
                                    player.drop(remainder, true);
                                }
                                itemPlaced = true;
                                break;
                            }
                        }

                        if (!itemPlaced && firstEmptyStack > -1) {
                            itemHandler.insertItem(firstEmptyStack, remainder, false);
                        }
                    }

                    container.setDataChanged();

                    if (player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getBoolean("DisplayFeederMessage")) {
                        String foodNameFormatted = (Util.isVowel(foodName.charAt(0)) ? "an" : "a") + " §a" + foodName + "§r";
                        player.displayClientMessage(Component.translatable("item.fxntstorage.backpack_feeder_upgrade.message", foodNameFormatted), true);
                    }

                } else {
                    // For some reason, food item was not consumed, revert item
                    player.getInventory().items.set(player.getInventory().selected, mainHandItem);
                }

                return;
            }
        }
    }

    private boolean hasNegativeEffects(@NotNull ItemStack food) {
        FoodProperties foodProperties = food.getFoodProperties(this.player);
        if (foodProperties == null) return false;

        if (food.is(Items.CHORUS_FRUIT) && !ConfigManager.ClientConfig.ALLOW_CHORUS_FRUIT.get()) return true;

        CompoundTag tag = food.getTag();
        if (tag != null && tag.contains("Effects", Tag.TAG_LIST)) {
            ListTag effects = tag.getList("Effects", Tag.TAG_COMPOUND);

            for (Tag entry : effects) {
                if (!(entry instanceof CompoundTag effectTag)) continue;
                if (!effectTag.contains("forge:effect_id", Tag.TAG_STRING)) continue;

                String effectString = effectTag.getString("forge:effect_id");
                ResourceLocation effectId = ResourceLocation.parse(effectString);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);

                if (effect != null)
                    return effect.getCategory().equals(MobEffectCategory.HARMFUL);
            }
        }

        // This should capture most foods with negative effects
        for (Pair<MobEffectInstance, Float> effect : foodProperties.getEffects()) {
            return (effect.getFirst().getEffect().getCategory().equals(MobEffectCategory.HARMFUL));
        }
        return false;
    }

    private boolean isDoFeed() {
        boolean doFeed = false;
        int hunger = this.player.getFoodData().getFoodLevel(); // Max 20
        float saturation = this.player.getFoodData().getSaturationLevel();
        float health = this.player.getHealth();
        // If player is hurt and saturation gone then feed
        if (health < this.player.getMaxHealth() && hunger < 18 && saturation < 2) {
            doFeed = true;
        }
        // Feed if less than 3 hunger haunches
        if (hunger < 6) {
            doFeed = true;
        }
        return doFeed;
    }

    // SERVER SIDE
    public void applyRefillUpgrade() {
        if (this.itemStack.isEmpty() || this.player.level().isClientSide || !hasUpgrade(Util.REFILL_UPGRADE))
            return;
        // Item in main hand or offhand is less than max stack size, check for matching items in backpack and fill inventory stack
        refillHand(this.player.getMainHandItem(), false);
        refillHand(this.player.getOffhandItem(), true);
    }

    public void refillHand(@NotNull ItemStack handItem, boolean isOffHand) {
        if (handItem.isEmpty()) return;
        boolean success;
        int requiredItems = handItem.getMaxStackSize() - handItem.getCount();
        if (requiredItems > 0) {
            int offHandSlotIndex = 40;
            int ignorePlayerSlot = isOffHand ? offHandSlotIndex : this.player.getInventory().selected;
            // Check Player inventory first
            success = refillMatchingItem(handItem, requiredItems, this.player, getContainer(), 0, this.player.getInventory().getContainerSize(), ignorePlayerSlot);
            if (!success) {
                refillMatchingItem(handItem, requiredItems, null, getContainer(), Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE, -1);
            }
        }
    }

    public boolean refillMatchingItem(ItemStack itemStack, int requiredItems, Player player, IBackpackContainer
            container, int startIndex, int endIndex, int ignoreSlot) {
        int amountToPlace = requiredItems;

        if (player != null) {
            // Check player inventory
            Container inventory = player.getInventory();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (i == ignoreSlot) continue;
                ItemStack inventoryItem = inventory.getItem(i);
                if (ItemStack.isSameItemSameTags(itemStack, inventoryItem)) {
                    if (inventoryItem.getCount() < requiredItems) {
                        amountToPlace = inventoryItem.getCount();
                    }
                    ItemStack itemToTransfer = inventoryItem.copy();
                    itemToTransfer.setCount(amountToPlace);
                    inventoryItem.shrink(amountToPlace);
                    itemStack.grow(amountToPlace);
                    if (player.containerMenu instanceof BackpackMenu backpackMenu) {
                        backpackMenu.container.setDataChanged();
                    }
                    return true;
                }
            }
        } else {
            // Check backpack item slots
            IItemHandler itemHandler = container.getItemHandler();
            if (itemHandler != null) {
                for (int i = startIndex; i < endIndex; i++) {
                    if (i == ignoreSlot) continue;
                    ItemStack containerItem = itemHandler.getStackInSlot(i);
                    if (ItemStack.isSameItemSameTags(itemStack, containerItem)) {
                        if (containerItem.getCount() < requiredItems) {
                            amountToPlace = containerItem.getCount();
                        }
                        // New stuff
                        ItemStack itemToTransfer = containerItem.copy();
                        itemToTransfer.setCount(amountToPlace);
                        itemHandler.extractItem(i, amountToPlace, false);
                        itemStack.grow(amountToPlace);
                        container.setDataChanged();

                        return true;
                    }
                }
            }


        }
        return false;
    }

    public boolean applyFallDamageUpgrade() {
        return !this.itemStack.isEmpty() && !this.player.level().isClientSide && hasUpgrade(Util.FALLDAMAGE_UPGRADE);
    }

    // SERVER SIDE
    public void fromAttackBlockEvent(Player player, Level level, InteractionHand hand, BlockPos pos) {
        if (this.itemStack.isEmpty() || hand != InteractionHand.OFF_HAND && player.isSpectator() || level.isClientSide || !player.isAlive()
                || player.isSleeping() || player.isDeadOrDying() || !hasUpgrade(Util.TOOLSWAP_UPGRADE)) return;


        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, getContainer(), Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE);
        toolSwapHandler.doToolSwap(pos, null);
    }

    public void fromAttackEntityEvent(Player player, Level level, InteractionHand hand, LivingEntity entity) {
        if (this.itemStack.isEmpty() || hand != InteractionHand.OFF_HAND && player.isSpectator() || level.isClientSide || !player.isAlive()
                || player.isSleeping() || player.isDeadOrDying() || !hasUpgrade(Util.TOOLSWAP_UPGRADE)) return;

        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, getContainer(), Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE);
        toolSwapHandler.doToolSwap(null, entity);
    }

}
