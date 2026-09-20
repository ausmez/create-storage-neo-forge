package net.fxnt.fxntstorage.compat;

import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.items.IItemHandler;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.*;

import java.util.ArrayList;
import java.util.List;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

    private static final BackpackSlotLayout BACKPACK_LAYOUT = BackpackSlotLayout.createLayout();
    private static final float UPGRADE_ICON_SCALE = 0.8f;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerItemStorage(BackpackItemStorageProvider.INSTANCE, BackpackEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BackpackComponentProvider.INSTANCE, BackpackBlock.class);
        registration.registerItemStorageClient(BackpackItemStorageProvider.INSTANCE);
        registration.registerBlockComponent(SimpleStorageBoxComponentProvider.INSTANCE, SimpleStorageBox.class);
    }

    public enum BackpackComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
            if (!(blockAccessor.getBlockEntity() instanceof BackpackEntity backpack)) return;

            IItemHandler itemHandler = backpack.getItemHandler();
            List<ItemStack> installed = new ArrayList<>();
            for (int i : BACKPACK_LAYOUT.upgrades().range()) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) installed.add(stack);
            }
            if (installed.isEmpty()) return;

            IElementHelper helper = IElementHelper.get();
            List<IElement> row = new ArrayList<>();
            row.add(helper.text(Component.translatable("tooltip.fxntstorage.upgrades")
                            .append(Component.literal(": "))
                            .withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()))
                    .translate(new Vec2(0.0f, 1.0f + 8.0f * UPGRADE_ICON_SCALE - 3.5f)));
            installed.stream().map(stack -> helper.item(stack, UPGRADE_ICON_SCALE)).forEach(row::add);
            iTooltip.add(row);
        }

        @Override
        public ResourceLocation getUid() {
            return ModBlocks.BACKPACK.getId();
        }
    }

    public enum BackpackItemStorageProvider implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
        INSTANCE;

        @Override
        public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof BackpackEntity backpack)) return null;

            IItemHandler itemHandler = backpack.getItemHandler();
            List<ItemStack> stored = new ArrayList<>();

            for (int i : BACKPACK_LAYOUT.items().range()) collect(itemHandler, i, stored);
            for (int i : BACKPACK_LAYOUT.tools().range()) collect(itemHandler, i, stored);

            return List.of(new ViewGroup<>(stored));
        }

        private static void collect(IItemHandler itemHandler, int slot, List<ItemStack> stored) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) return;

            for (ItemStack other : stored) {
                if (ItemStack.isSameItemSameComponents(other, stack)) {
                    other.grow(stack.getCount());
                    return;
                }
            }
            stored.add(stack.copy());
        }

        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
            return ClientViewGroup.map(groups, ItemView::new, null);
        }

        @Override
        public ResourceLocation getUid() {
            return ModBlocks.BACKPACK.getId();
        }
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
                text.append(
                        Component.literal(" (")
                                .append(Component.literal("" + capUpgrades).withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal(")")).withStyle(ChatFormatting.DARK_GRAY)
                );

            if (box.compactingUpgrade) {
                text.append(
                        Component.literal(" [")
                                .append(Component.literal("C").withStyle(ChatFormatting.GOLD))
                                .append(Component.literal("]")).withStyle(ChatFormatting.DARK_GRAY)
                );
            }

            if (box.voidUpgrade)
                text.append(
                        Component.literal(" [")
                                .append(Component.literal("V").withStyle(ChatFormatting.DARK_PURPLE))
                                .append(Component.literal("]")).withStyle(ChatFormatting.DARK_GRAY)
                );

            iTooltip.add(text);
        }

        @Override
        public ResourceLocation getUid() {
            return ModBlocks.SIMPLE_STORAGE_BOX_OAK.getId();
        }
    }
}
