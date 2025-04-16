package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosCompat {

    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerItem(
                CuriosCapability.ITEM,
                (stack, context) -> new BackpackCurio(stack),
                ModBlocks.BACKPACK.get().asItem(),
                ModBlocks.ANDESITE_BACKPACK.get().asItem(),
                ModBlocks.COPPER_BACKPACK.get().asItem(),
                ModBlocks.BRASS_BACKPACK.get().asItem(),
                ModBlocks.HARDENED_BACKPACK.get().asItem()
        );
    }

    public static void keepBackpack(DropRulesEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ConfigManager.CommonConfig.CURIOS_KEEP_BACKPACK.get()) {
                CuriosApi.getCuriosInventory(player).flatMap(curiosInv -> curiosInv.getStacksHandler("back")).ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getSlots(); ++i) {
                        if (stacksHandler.getStacks().getStackInSlot(i).getItem() instanceof BackpackItem) {
                            int finalI = i;
                            event.addOverride(itemStack -> itemStack == stacksHandler.getStacks().getStackInSlot(finalI), ICurio.DropRule.ALWAYS_KEEP);
                        }
                    }
                });
            }
        }
    }

    public static class BackpackCurio implements ICurio {
        private final ItemStack stack;

        private BackpackCurio(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public @NotNull SoundInfo getEquipSound(SlotContext slotContext) {
            return new SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        }

        @Override
        public boolean canEquip(SlotContext slotContext) {
            return !(slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).getItem().asItem() instanceof BackpackItem);
        }

        @Override
        public boolean canSync(SlotContext slotContext) {
            return true;
        }

    }

}
