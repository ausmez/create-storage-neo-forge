package net.fxnt.fxntstorage.mixin;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    // Never Despawn Backpack
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void fxnt$init(CallbackInfo info) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (itemEntity.getItem().getItem() instanceof BackpackItem) {
            itemEntity.setUnlimitedLifetime();
        }
    }

    @Shadow
    private int pickupDelay;
    @Shadow
    private @Nullable UUID target;

    @Inject(method = "playerTouch", at = @At(value = "HEAD"), cancellable = true)
    private void fxnt$onPlayerPickUpItem(Player player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (player == null || player.isSpectator() || player.level().isClientSide || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
            return;
        if (!BackpackHelper.isWearingBackpack(player)) return;
        if (new BackpackOnBackUpgradeHandler(player).applyItemPickupUpgrade(itemEntity, target, pickupDelay)) {
            ci.cancel();
        }
    }
}