package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackBlockMenu;
import net.fxnt.fxntstorage.backpack.main.BackpackItemMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxMenu;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FXNTStorage.MOD_ID);

    public static final RegistryObject<MenuType<StorageBoxMenu>> STORAGE_BOX_MENU = registerMenuType("storage_box_menu", StorageBoxMenu::new);
    public static final RegistryObject<MenuType<StorageBoxMountedMenu>> STORAGE_BOX_MOUNTED_MENU = registerMenuType("storage_box_mounted_menu", StorageBoxMountedMenu::new);
    public static final RegistryObject<MenuType<SimpleStorageBoxMenu>> SIMPLE_STORAGE_BOX_MENU = registerMenuType("simple_storage_box_menu", SimpleStorageBoxMenu::new);
    public static final RegistryObject<MenuType<SimpleStorageBoxMountedMenu>> SIMPLE_STORAGE_BOX_MOUNTED_MENU = registerMenuType("simple_storage_box_mounted_menu", SimpleStorageBoxMountedMenu::new);
    public static final RegistryObject<MenuType<BackpackItemMenu>> BACKPACK_ITEM_MENU = registerMenuType("backpack_item_menu", BackpackItemMenu::new);
    public static final RegistryObject<MenuType<BackpackBlockMenu>> BACKPACK_BLOCK_MENU = registerMenuType("backpack_block_menu", BackpackBlockMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
