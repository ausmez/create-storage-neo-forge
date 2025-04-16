package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackBlockMenu;
import net.fxnt.fxntstorage.backpack.main.BackpackItemMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FXNTStorage.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StorageBoxMenu>> STORAGE_BOX_MENU = registerMenuType("storage_box_menu", StorageBoxMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<SimpleStorageBoxMenu>> SIMPLE_STORAGE_BOX_MENU = registerMenuType("simple_storage_box_menu", SimpleStorageBoxMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<BackpackItemMenu>> BACKPACK_ITEM_MENU = registerMenuType("backpack_item_menu", BackpackItemMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<BackpackBlockMenu>> BACKPACK_BLOCK_MENU = registerMenuType("backpack_block_menu", BackpackBlockMenu::new);

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

}
