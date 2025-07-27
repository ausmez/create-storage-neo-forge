package net.fxnt.fxntstorage.backpack.upgrade;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllItems;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    private void refillHand(@NotNull ItemStack handItem, boolean isOffHand) {
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

    private boolean refillMatchingItem(ItemStack itemStack, int requiredItems, Player player, IBackpackContainer
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
                || player.isSleeping() || player.isDeadOrDying() || !hasUpgrade(Util.TOOLSWAP_UPGRADE)
                || player.getMainHandItem().is(AllItems.WRENCH.asItem())) return;

        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, getContainer(), Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE);
        toolSwapHandler.doToolSwap(pos, null);
    }

    public void fromAttackEntityEvent(Player player, Level level, InteractionHand hand, LivingEntity entity) {
        if (this.itemStack.isEmpty() || hand != InteractionHand.OFF_HAND && player.isSpectator() || level.isClientSide || !player.isAlive()
                || player.isSleeping() || player.isDeadOrDying() || !hasUpgrade(Util.TOOLSWAP_UPGRADE)
                || player.getMainHandItem().is(AllItems.WRENCH.asItem())) return;

        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, getContainer(), Util.TOOL_SLOT_START_RANGE, Util.TOOL_SLOT_END_RANGE);
        toolSwapHandler.doToolSwap(null, entity);
    }

    public void applyOreMiningUpgrade(Level level, BlockPos origin, Player player, boolean mineAllBlocks, int maxBlocks) {
        BlockState targetState = level.getBlockState(origin);
        List<BlockPos> vein = findVein(level, origin, targetState, mineAllBlocks, maxBlocks);

        List<ItemStack> drops = new ArrayList<>();
        ItemStack tool = player.getMainHandItem();

        int blocksMined = 0;

        for (BlockPos pos : vein) {
            if (tool.isEmpty()) break;

            BlockState state = level.getBlockState(pos);
            if (level.isEmptyBlock(pos)) continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);

            LootParams.Builder lootParams = new LootParams.Builder((ServerLevel) level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, tool)
                    .withParameter(LootContextParams.BLOCK_STATE, state)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

            List<ItemStack> blockDrops = state.getDrops(lootParams);
            drops.addAll(blockDrops);

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            state.onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));

            if (state.getDestroySpeed(level, pos) >= 0.0F) {
                tool.hurtAndBreak(1, player, p -> {
                    p.broadcastBreakEvent(EquipmentSlot.MAINHAND);
                });
                if (!player.getAbilities().instabuild) {
                    player.causeFoodExhaustion(0.2F);
                }
            }

            blocksMined++;
        }

        List<ItemStack> condensed = new ArrayList<>();

        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                boolean merged = false;

                for (ItemStack existing : condensed) {
                    if (ItemStack.isSameItemSameTags(existing, stack) && existing.isStackable()) {
                        int transferable = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        if (transferable > 0) {
                            existing.grow(transferable);
                            stack.shrink(transferable);
                            if (stack.isEmpty()) {
                                merged = true;
                                break;
                            }
                        }
                    }
                }

                if (!merged && !stack.isEmpty()) {
                    condensed.add(stack.copy());
                }
            }
        }

        for (ItemStack drop : condensed) {
            Block.popResource(level, origin, drop);
        }

        if (!FMLEnvironment.production) {
            Component msg = Component.literal("Successfully mined §a" + blocksMined + "§r out of §a" + vein.size() + "§r");
            player.displayClientMessage(msg, false);
        }
    }

    private List<BlockPos> findVein(Level level, BlockPos start, BlockState targetState, boolean mineAllBlocks, int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start);

        while (!toVisit.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = toVisit.poll();
            if (!visited.add(current)) continue; // skip if already visited

            // Check all 26 surrounding positions in a 3x3x3 cube
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue; // Skip the center block

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState neighborState = level.getBlockState(neighbor);

                        if (!neighborState.getBlock().equals(targetState.getBlock())) continue;
                        if (!mineAllBlocks && !neighborState.is(ModTags.Blocks.ORE_MINING_BLOCK)) continue;

                        toVisit.add(neighbor);
                    }
                }
            }
        }

        // Sort positions by distance from player
        return visited.stream()
                .sorted(Comparator.comparingDouble(blockPos -> blockPos.distSqr(player.blockPosition())))
                .toList();
    }

    @SuppressWarnings("deprecation")
    public void applyTorchDeployerUpgrade(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockPos belowPos = playerPos.below();
        Level level = player.level();

        int lightLevel = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getInt("TorchDeployerLightLevel");
        int cooldown = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getInt("TorchDeployerCooldown");
        String sourceValue = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getString("TorchDeployerLightSource");

        ConfigManager.ClientConfig.TorchDeployerLightSource lightSource;
        try {
            lightSource = ConfigManager.ClientConfig.TorchDeployerLightSource.valueOf(
                    sourceValue == null || sourceValue.isEmpty() ? "BLOCK_LIGHT" : sourceValue
            );
        } catch (IllegalArgumentException e) {
            lightSource = ConfigManager.ClientConfig.TorchDeployerLightSource.BLOCK_LIGHT;
        }

        int blockLightLevel = (lightSource == ConfigManager.ClientConfig.TorchDeployerLightSource.SKY_LIGHT)
                ? level.getBrightness(LightLayer.BLOCK, playerPos)
                : level.getMaxLocalRawBrightness(playerPos);

        if (blockLightLevel <= lightLevel &&
                level.getBlockState(belowPos).isSolid() &&
                level.getBlockState(playerPos).isAir()) {

            if (!TorchDeployerManager.canPlaceTorch(player, cooldown)) return;

            // Place torch
            IBackpackContainer container = getContainer();
            IItemHandlerModifiable itemHandler = container.getItemHandler();

            for (int slot = Util.ITEM_SLOT_START_RANGE; slot < Util.ITEM_SLOT_END_RANGE; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.is(Items.TORCH)) {
                    stack.shrink(1);
                    itemHandler.setStackInSlot(slot, stack);

                    level.setBlock(playerPos, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                    level.playSound(null, playerPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS);

                    container.setDataChanged();
                    if (!FMLEnvironment.production) {
                        player.displayClientMessage(Component.literal("Placed §a1§r torch with §a" + stack.getCount() + "§r left in the stack"), false);
                    }

                    break;
                }
            }
        }
    }

}
