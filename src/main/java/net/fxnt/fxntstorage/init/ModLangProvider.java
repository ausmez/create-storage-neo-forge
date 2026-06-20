package net.fxnt.fxntstorage.init;

import com.google.gson.JsonElement;
import com.simibubi.create.foundation.utility.FilesHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.List;
import java.util.function.BiConsumer;

public class ModLangProvider {
    private static final List<String> LANG_FILES = List.of("interface", "ponder", "tooltips");

    public static void provide(LanguageProvider prov) {
        BiConsumer<String, String> consumer = prov::add;
        LANG_FILES.forEach(file -> loadLangFile(file, consumer));
    }

    private static void loadLangFile(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + FXNTStorage.MOD_ID + "/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);

        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }

        jsonElement.getAsJsonObject()
                .entrySet()
                .forEach(entry -> consumer.accept(entry.getKey(), entry.getValue().getAsString()));
    }
}
