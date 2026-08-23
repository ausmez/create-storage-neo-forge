package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPackFinders {

    private static final String VANILLA_BACKPORT_PACK = "packs/vanillabackport";

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        if (!ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT)) return;

        IModFileInfo modFileInfo = ModList.get().getModFileById(FXNTStorage.MOD_ID);
        if (modFileInfo == null) return;

        Path packPath = modFileInfo.getFile().findResource(VANILLA_BACKPORT_PACK);
        Pack pack = Pack.readMetaAndCreate(
                "builtin/fxntstorage_vanillabackport",
                Component.translatable("itemGroup.fxntstorage.main"),
                true,
                id -> new PathPackResources(id, packPath, true),
                PackType.SERVER_DATA,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
