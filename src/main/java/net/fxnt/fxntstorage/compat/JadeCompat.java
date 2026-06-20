package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SimpleStorageBoxComponentProvider.INSTANCE, SimpleStorageBox.class);
    }

    public enum SimpleStorageBoxComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
            if (!(blockAccessor.getBlockEntity() instanceof SimpleStorageBoxEntity box)) return;

            int capUpgrades = box.getCapacityUpgrades();

            MutableComponent text = Component.translatable("container.fxntstorage.simple_storage_box.max_capacity")
                    .append(Component.literal(": "))
                    .append(Component.literal(String.valueOf(box.getDisplayedMaxCapacity())));

            if (capUpgrades > 0)
                text.append(Component.literal(" (" + capUpgrades + ")"));

            iTooltip.add(text);
        }

        @Override
        public ResourceLocation getUid() {
            return ModBlocks.SIMPLE_STORAGE_BOX_OAK.getId();
        }
    }
}
