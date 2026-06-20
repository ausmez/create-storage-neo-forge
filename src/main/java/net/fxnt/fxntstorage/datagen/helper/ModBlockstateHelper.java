package net.fxnt.fxntstorage.datagen.helper;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageInterface;
import net.fxnt.fxntstorage.controller.StorageInterfaceFiltered;
import net.fxnt.fxntstorage.passer.PasserBlock;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModBlockstateHelper {

    public static NonNullBiConsumer<DataGenContext<Block, StorageBox>, RegistrateBlockstateProvider> storageBox(String name) {
        return (ctx, prov) -> {

            // Generate block light overlay models if they don't exist
            if (!prov.models().existingFileHelper.exists(modLoc("block/storage_box_void"), PackType.CLIENT_RESOURCES))
                ModModelHelper.storageBoxLight(prov);

            // Generate block model
            ModModelHelper.storageBox(prov, name);

            // Generate blockstate
            MultiPartBlockStateBuilder builder = prov.getMultipartBuilder(ctx.get());
            String basePath = "block/" + (name.equals("industrial_iron") ? "" : name + "_");

            BiConsumer<String, Consumer<MultiPartBlockStateBuilder.PartBuilder>> addDirectionalParts = (modelName, config) -> {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    MultiPartBlockStateBuilder.PartBuilder part = builder.part()
                            .modelFile(prov.models().getExistingFile(prov.modLoc(modelName)))
                            .rotationY(getRotationForDirection(dir))
                            .addModel()
                            .condition(StorageBox.FACING, dir);
                    config.accept(part);
                    part.end();
                }
            };

            // Base block
            addDirectionalParts.accept(basePath + "storage_box_base", part -> {
            });

            // Storage Box used levels
            for (EnumProperties.StorageUsed state : EnumProperties.StorageUsed.values()) {
                addDirectionalParts.accept("block/storage_box_" + state.getSerializedName(),
                        part -> part.condition(StorageBox.STORAGE_USED, state));
            }

            // Storage Box void enabled
            addDirectionalParts.accept("block/storage_box_void",
                    part -> part.condition(StorageBox.VOID_UPGRADE, true));
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, SimpleStorageBox>, RegistrateBlockstateProvider> simpleStorageBox(Block planks) {
        return (ctx, prov) -> generateSimpleStorageBox(ctx, prov, planks);
    }

    public static NonNullBiConsumer<DataGenContext<Block, SimpleStorageBox>, RegistrateBlockstateProvider> simpleStorageBox(Supplier<? extends Block> planks) {
        return (ctx, prov) -> generateSimpleStorageBox(ctx, prov, planks.get());
    }

    private static void generateSimpleStorageBox(DataGenContext<Block, SimpleStorageBox> ctx, RegistrateBlockstateProvider prov, Block planks) {
        String path = BuiltInRegistries.BLOCK.getKey(planks).getPath();
        String woodType = path.substring(0, path.indexOf("_planks"));

//        if (!prov.models().existingFileHelper.exists(modLoc("block/storage_box_void"), PackType.CLIENT_RESOURCES))
//            ModModelHelper.storageBoxLight(prov);

        ModModelHelper.simpleStorageBox(prov, woodType); // Generate blockModel

        MultiPartBlockStateBuilder builder = prov.getMultipartBuilder(ctx.get());

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            builder.part()
                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/" + woodType + "_simple_storage_box_base")))
                    .rotationY(getRotationForDirection(dir))
                    .addModel()
                    .condition(StorageBox.FACING, Direction.byName(dir.getName()))
                    .end();
        }
        for (EnumProperties.StorageUsed state : EnumProperties.StorageUsed.values()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                builder.part()
                        .modelFile(prov.models().getExistingFile(prov.modLoc("block/storage_box_" + state.getSerializedName())))
                        .rotationY(getRotationForDirection(dir))
                        .addModel()
                        .condition(StorageBox.FACING, Direction.byName(dir.getName()))
                        .condition(StorageBox.STORAGE_USED, state)
                        .end();
            }
        }
//        for (Direction dir : Direction.Plane.HORIZONTAL) {
//            builder.part()
//                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/storage_box_void")))
//                    .rotationY(getRotationForDirection(dir))
//                    .addModel()
//                    .condition(StorageBox.FACING, Direction.byName(dir.getName()))
//                    .condition(SimpleStorageBox.VOID_UPGRADE, true)
//                    .end();
//        }
    }

    public static NonNullBiConsumer<DataGenContext<Block, CasingBlock>, RegistrateBlockstateProvider> storageTrim(String name) {
        return (ctx, prov) -> prov.simpleBlock(ctx.get(),
                prov.models().cubeAll("block/" + ctx.getName(), modLoc("block/casings/" + name + "_casing")));
    }

    public static NonNullBiConsumer<DataGenContext<Block, BackpackBlock>, RegistrateBlockstateProvider> backpack(String name) {
        return (ctx, prov) -> {
            String type = name.equals("industrial_iron") ? "" : name + "_";

            if (!name.equals("industrial_iron")) // industrial_iron is the base model
                ModModelHelper.backpack(prov, name);

            MultiPartBlockStateBuilder builder = prov.getMultipartBuilder(ctx.get());

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                builder.part()
                        .modelFile(prov.models().getExistingFile(prov.modLoc("block/" + type + "backpack")))
                        .rotationY(getRotationForDirection(dir))
                        .addModel()
                        .condition(StorageBox.FACING, Direction.byName(dir.getName()))
                        .end();
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageController>, RegistrateBlockstateProvider> storageController() {
        return (ctx, prov) -> {
            prov.models().cube(
                    "storage_controller",
                    ResourceLocation.fromNamespaceAndPath("create", "block/dark_metal_block"),
                    modLoc("block/storage_controller_top"),
                    modLoc("block/storage_controller_front"),
                    modLoc("block/storage_controller_side"),
                    modLoc("block/storage_controller_side"),
                    modLoc("block/storage_controller_side")
            ).texture("particle", modLoc("block/storage_controller_side"));

            prov.models().withExistingParent("storage_controller_connected", modLoc("storage_controller"))
                    .texture("north", modLoc("block/storage_controller_front_connected"));

            MultiPartBlockStateBuilder builder = prov.getMultipartBuilder(ctx.get());

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                builder.part()
                        .modelFile(prov.models().getExistingFile(prov.modLoc("block/storage_controller")))
                        .rotationY(getRotationForDirection(dir))
                        .addModel()
                        .condition(StorageBox.FACING, Direction.byName(dir.getName()))
                        .end();
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                builder.part()
                        .modelFile(prov.models().getExistingFile(prov.modLoc("block/storage_controller_connected")))
                        .rotationY(getRotationForDirection(dir))
                        .addModel()
                        .condition(StorageController.FACING, Direction.byName(dir.getName()))
                        .condition(StorageController.CONNECTED, true)
                        .end();
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageInterface>, RegistrateBlockstateProvider> storageInterface() {
        return (ctx, prov) -> prov.simpleBlock(ctx.get(),
                prov.models().cubeBottomTop("storage_interface",
                                modLoc("block/storage_interface_side"),
                                ResourceLocation.fromNamespaceAndPath("create", "block/dark_metal_block"),
                                modLoc("block/storage_interface_top"))
                        .texture("particle", modLoc("block/storage_interface_top")));
    }

    public static NonNullBiConsumer<DataGenContext<Block, StorageInterfaceFiltered>, RegistrateBlockstateProvider> storageInterfaceFiltered() {
        return (ctx, prov) -> prov.simpleBlock(ctx.get(),
                prov.models().cubeBottomTop("storage_interface_filtered",
                        modLoc("block/storage_interface_filtered"),
                        ResourceLocation.fromNamespaceAndPath("create", "block/dark_metal_block"),
                        modLoc("block/storage_controller_top")
                ).texture("particle", modLoc("block/storage_interface_top")));
    }

    public static NonNullBiConsumer<DataGenContext<Block, PasserBlock>, RegistrateBlockstateProvider> passerBlock(boolean isSmart) {
        return (ctx, prov) -> {
            if (isSmart) {
                prov.models().withExistingParent(ctx.getName() + "_powered", prov.modLoc("block/smart_passer_block"))
                        .texture("main", prov.modLoc("block/smart_passer_block_powered"));
            }

            VariantBlockStateBuilder builder = prov.getVariantBuilder(ctx.get());
            boolean[] poweredStates = isSmart ? new boolean[]{false, true} : new boolean[]{false};

            for (boolean powered : poweredStates) {
                for (Direction dir : Direction.values()) {
                    String modelName = isSmart
                            ? "block/smart_passer_block" + (powered ? "_powered" : "")
                            : "block/passer_block";

                    ConfiguredModel.Builder<?> model = ConfiguredModel.builder()
                            .modelFile(prov.models().getExistingFile(prov.modLoc(modelName)));

                    switch (dir) {
                        case UP -> model.rotationX(180);
                        case SOUTH -> model.rotationX(90);
                        case WEST -> model.rotationX(90).rotationY(90);
                        case NORTH -> model.rotationX(90).rotationY(180);
                        case EAST -> model.rotationX(90).rotationY(270);
                    }

                    (isSmart
                            ? builder.partialState().with(BlockStateProperties.POWERED, powered)
                            : builder.partialState())
                            .with(BlockStateProperties.FACING, dir)
                            .addModels(model.build());
                }
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, ReserveStorageBox>, RegistrateBlockstateProvider> reserveStorageBox() {
        return (ctx, prov) -> {

            // Generate block light overlay models if they don't exist
            if (!prov.models().existingFileHelper.exists(modLoc("block/storage_box_void"), PackType.CLIENT_RESOURCES))
                ModModelHelper.storageBoxLight(prov);

            // Generate blockstate
            MultiPartBlockStateBuilder builder = prov.getMultipartBuilder(ctx.get());
            String basePath = "block/reserve_";

            BiConsumer<String, Consumer<MultiPartBlockStateBuilder.PartBuilder>> addDirectionalParts = (modelName, config) -> {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    MultiPartBlockStateBuilder.PartBuilder part = builder.part()
                            .modelFile(prov.models().getExistingFile(prov.modLoc(modelName)))
                            .rotationY(getRotationForDirection(dir))
                            .addModel()
                            .condition(StorageBox.FACING, dir);
                    config.accept(part);
                    part.end();
                }
            };

            // Base block
            addDirectionalParts.accept(basePath + "storage_box", part -> {
            });

            // Storage Box used levels
            for (EnumProperties.StorageUsed state : EnumProperties.StorageUsed.values()) {
                addDirectionalParts.accept("block/storage_box_" + state.getSerializedName(),
                        part -> part.condition(StorageBox.STORAGE_USED, state));
            }
        };
    }

    private static int getRotationForDirection(Direction dir) {
        return (dir.get2DDataValue() * 90 + 180) % 360;
    }
}
