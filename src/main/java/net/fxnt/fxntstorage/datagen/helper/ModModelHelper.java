package net.fxnt.fxntstorage.datagen.helper;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.container.StorageBoxItem;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModModelHelper {

    public static void backpack(RegistrateBlockstateProvider prov, String name) {
        prov.models().withExistingParent(name + "_backpack", modLoc("block/backpack"))
                .texture("0", modLoc("block/" + name + "_backpack"))
                .texture("particle", modLoc("block/" + name + "_backpack"));
    }

    public static NonNullBiConsumer<DataGenContext<Item, BackpackItem>, RegistrateItemModelProvider> backpackItem(String name) {
        return (ctx, prov) -> {
            String variant = name.equals("industrial_iron") ? "" : name + "_";

            prov.withExistingParent(ctx.getName(), ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "block/" + variant + "backpack"))
                    .transforms()
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(-90, 150, -180)
                    .translation(0, 0, -3.25F)
                    .scale(0.65F, 0.65F, 0.65F)
                    .end()
                    .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    .rotation(-90, 150, -180)
                    .translation(0, 0, -3.25F)
                    .scale(0.65F, 0.65F, 0.65F)
                    .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(0, -90, 12.5F)
                    .translation(1.13F, 3.5F, 2F)
                    .scale(0.4F, 0.4F, 0.4F)
                    .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(0, -90, 12.5F)
                    .translation(1.13F, 3.5F, 2F)
                    .scale(0.4F, 0.4F, 0.4F)
                    .end()
                    .transform(ItemDisplayContext.GROUND)
                    .translation(0, 2F, 0)
                    .scale(0.5F, 0.5F, 0.5F)
                    .end()
                    .transform(ItemDisplayContext.GUI)
                    .rotation(30, -225, 0)
                    .translation(-0.25F, 1.5F, 0)
                    .end()
                    .transform(ItemDisplayContext.HEAD)
                    .rotation(0, 180, 0)
                    .translation(0, 14.5F, 0)
                    .end()
                    .transform(ItemDisplayContext.FIXED)
                    .rotation(0, 180, 0)
                    .translation(0, 2.25F, 0)
                    .end()
                    .end();
        };
    }

    public static void storageBox(RegistrateBlockstateProvider prov, String name) {
        record TextureSet(String back, String casing, String top) {
        }
        Map<String, TextureSet> textureSets = Map.of(
                "cardboard", new TextureSet("create:block/cardboard_block_front", "create:block/cardboard_block_side", "create:block/cardboard_block_top"),
                "weathered", new TextureSet("create:block/weathered_iron_block", "create:block/weathered_iron_block", "create:block/weathered_iron_block_top"),
                "hardened", new TextureSet("create:block/railway_casing", "create:block/railway_casing", "create:block/railway_casing")
        );

        String modelBasePath = "block/" + name + "_storage_box_base";
        if (!name.equals("industrial_iron")) {
            TextureSet textures = textureSets.getOrDefault(name, new TextureSet(
                    "create:block/" + name + "_casing",
                    "create:block/" + name + "_casing",
                    "create:block/" + name + "_casing"
            ));
            prov.models().withExistingParent(modelBasePath, modLoc("block/storage_box_base"))
                    .texture("back", textures.back())
                    .texture("casing", textures.casing())
                    .texture("particle", textures.casing())
                    .texture("top", textures.top());
        }
    }

    public static NonNullBiConsumer<DataGenContext<Item, StorageBoxItem>, RegistrateItemModelProvider> storageBox(String name) {
        return (ctx, prov) -> {
            String type = (name.equals("industrial_iron")) ? "storage_box_base" : name + "_storage_box_base";
            prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + type));
        };
    }

    public static void simpleStorageBox(RegistrateBlockstateProvider prov, String name) {
        prov.models().withExistingParent("block/" + name + "_simple_storage_box_base", modLoc("block/storage_box_base"))
                .texture("casing", modLoc("block/casings/" + name + "_casing"))
                .texture("particle", modLoc("block/casings/" + name + "_casing"))
                .texture("top", modLoc("block/casings/" + name + "_casing"))
                .texture("back", modLoc("block/casings/" + name + "_casing"));

    }

    public static void simpleStorageBox(DataGenContext<Item, SimpleStorageBoxItem> ctx, RegistrateItemModelProvider prov, String name) {
        prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + name + "_simple_storage_box_base"));
    }

    public static void storageBoxLight(RegistrateBlockstateProvider prov) {
        Map<String, String> lightTextures = Map.of(
                "empty", "blue",
                "full", "red",
                "has_items", "green",
                "slots_filled", "orange",
                "void", "purple"
        );

        lightTextures.forEach((fillLevel, lightColor) ->
                prov.models().withExistingParent("block/storage_box_" + fillLevel, modLoc("block/storage_box_light"))
                        .texture("light", modLoc("block/storage_box/" + lightColor + "_light")));
    }
}