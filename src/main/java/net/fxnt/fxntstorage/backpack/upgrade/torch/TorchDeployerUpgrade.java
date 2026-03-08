package net.fxnt.fxntstorage.backpack.upgrade.torch;

import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.IItemHandlerModifiable;

public class TorchDeployerUpgrade extends AbstractUpgrade {

    public TorchDeployerUpgrade() {
        super(UpgradeType.TORCHDEPLOYER);
    }

    @Override
    public boolean onBlockBreak(UpgradeContext context, BlockEvent.BreakEvent event) {
        Player player = context.player();

        if (event.getState().is(Blocks.TORCH) && BackpackHelper.isWearingBackpack(player)) {
            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            IBackpackContainer container = new BackpackContainer(player, backpack);

            if (UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), UpgradeType.TORCHDEPLOYER))
                TorchDeployerManager.resetCooldown(player);
        }
        return false; // don't cancel event
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void tickActive(UpgradeContext context) {
        Player player = context.player();
        BlockPos playerPos = player.blockPosition();
        BlockPos belowPos = playerPos.below();
        Level level = player.level();

        int lightLevel = ClientSettings.getInt(player.getUUID(), "TorchDeployerLightLevel");
        int cooldown = ClientSettings.getInt(player.getUUID(), "TorchDeployerCooldown");
        String sourceValue = ClientSettings.getString(player.getUUID(), "TorchDeployerLightSource");

        ConfigManager.ClientConfig.TorchDeployerLightSource lightSource;
        try {
            lightSource = ConfigManager.ClientConfig.TorchDeployerLightSource.valueOf(
                    sourceValue.isEmpty() ? "BLOCK_LIGHT" : sourceValue
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
            IBackpackContainer container = context.container();
            IItemHandlerModifiable itemHandler = container.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            for (int slot : layout.items().range()) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.is(Items.TORCH)) {
                    stack.shrink(1);
                    itemHandler.setStackInSlot(slot, stack);

                    level.setBlock(playerPos, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                    level.playSound(null, playerPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

                    container.setDataChanged();
                    if (!FMLEnvironment.production)
                        player.displayClientMessage(Component.literal("Placed §a1§r torch with §a" + stack.getCount() + "§r left in the stack"), false);

                    break;
                }
            }
        }
    }
}
