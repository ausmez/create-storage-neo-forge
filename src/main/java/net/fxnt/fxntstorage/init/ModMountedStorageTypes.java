package net.fxnt.fxntstorage.init;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorageType;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorageType;

import java.util.function.Supplier;

public class ModMountedStorageTypes {
    private static final CreateRegistrate REGISTRATE = FXNTStorage.REGISTRATE;

    public static final RegistryEntry<MountedItemStorageType<?>, StorageBoxMountedStorageType> STORAGE_BOX_MOUNTED = simpleItem("storagebox_mounted", StorageBoxMountedStorageType::new);
    public static final RegistryEntry<MountedItemStorageType<?>, SimpleStorageBoxMountedStorageType> SIMPLE_STORAGE_BOX_MOUNTED = simpleItem("simple_storagebox_mounted", SimpleStorageBoxMountedStorageType::new);

    private static <T extends MountedItemStorageType<?>> RegistryEntry<MountedItemStorageType<?>, T> simpleItem(String name, Supplier<T> supplier) {
        return REGISTRATE.mountedItemStorage(name, supplier).register();
    }

    public static void register() {
    }
}
