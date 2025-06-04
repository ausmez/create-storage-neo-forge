package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosCompat implements ICurio {
    public final ItemStack stack;

    public CuriosCompat(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public @NotNull SoundInfo getEquipSound(SlotContext slotContext) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.0F);
    }

    @Override
    public boolean canEquip(SlotContext slotContext) {
        return !(slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).getItem().asItem() instanceof BackpackItem);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return false;
    }

    @Override
    public boolean canSync(SlotContext slotContext) {
        return true;
    }

    @Override
    public @NotNull DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel, boolean recentlyHit) {
        return (ConfigManager.CommonConfig.CURIOS_KEEP_BACKPACK.get()) ? DropRule.ALWAYS_KEEP : DropRule.ALWAYS_DROP;
    }

}
