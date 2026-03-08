package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PotatoCannonItem.class)
public abstract class PotatoCannonItemMixin {

    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V")
    )

    private void fxnt$redirectShrink(ItemStack ammoStack, int amount, Level level, Player player, InteractionHand hand) {
        // Check player inventory first
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, ammoStack)) {
                stack.shrink(amount);
                return;
            }
        }

        // Then check equipped backpack
        if (ClientSettings.getBoolean(player.getUUID(), "CheckBackpackForProjectiles")
                && BackpackHelper.isWearingBackpack(player)) {

            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            IBackpackContainer backpackContainer = new BackpackContainer(player, backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            // Shrink ammo stack from backpack
            for (int i : layout.getItemsAndToolsRange()) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (ItemStack.isSameItemSameTags(stack, ammoStack)) {
                    stack.shrink(amount);
                    itemHandler.setStackInSlot(i, stack);
                    if (!player.level().isClientSide) backpackContainer.setDataChanged();
                    return;
                }
            }
        }

        // Fallback
        ammoStack.shrink(amount);
    }
}
