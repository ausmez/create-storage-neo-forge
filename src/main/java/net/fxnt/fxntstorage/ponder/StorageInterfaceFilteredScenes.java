package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.controller.StorageInterfaceFilteredEntity;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.util.Icons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StorageInterfaceFilteredScenes {
    private record StorageBoxEntry(BlockPos pos, ItemStack stack) {
    }

    private static void populateBox(CreateSceneBuilder scene, StorageBoxEntry entry, int count) {
        scene.world().modifyBlockEntity(entry.pos(), SimpleStorageBoxEntity.class, be -> {
            be.getItemHandler().setStackInSlot(0, entry.stack().copyWithCount(count));
            be.setFilter(entry.stack());
        });

        scene.world().modifyBlock(entry.pos(),
                s -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.getDefaultState()
                        .setValue(SimpleStorageBox.STORAGE_USED,
                                count > 0 ? EnumProperties.StorageUsed.HAS_ITEMS : EnumProperties.StorageUsed.EMPTY),
                false);
    }

    private static void extractOne(CreateSceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, SimpleStorageBoxEntity.class, be -> be.getItemHandler().extractItem(0, 1, false));
    }

    private static void populateAll(CreateSceneBuilder scene, List<StorageBoxEntry> boxes, int count) {
        boxes.forEach(box -> populateBox(scene, box, count));
    }

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_interface_filtered_intro", "Simple Storage Interface Filtered");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        Selection network = util.select().fromTo(2, 3, 2, 4, 1, 2);
        Selection focus = util.select().fromTo(0, 1, 2, 1, 1, 3);
        Selection redstone = util.select().fromTo(2, 1, 0, 1, 1, 1);
        BlockPos sfi = util.grid().at(1, 2, 2);
        BlockPos hopper = util.grid().at(0, 3, 2);
        Vec3 filter = util.vector().blockSurface(sfi, Direction.NORTH).add(new Vec3(-0.2, 0, 0));
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack cobb = new ItemStack(Items.COBBLESTONE);
        BlockPos lever = util.grid().at(2, 1, 0);
        BlockPos chest = util.grid().at(0, 1, 2);

        List<StorageBoxEntry> boxes = List.of(
                new StorageBoxEntry(util.grid().at(4, 3, 2), copper),
                new StorageBoxEntry(util.grid().at(3, 3, 2), gold),
                new StorageBoxEntry(util.grid().at(2, 3, 2), cobb)
        );

        // Populate storage boxes
        populateAll(scene, boxes, 4);

        scene.world().toggleRedstonePower(util.select().fromTo(1, 1, 0, 1, 1, 1));

        scene.world().showSection(network, Direction.DOWN);
        scene.idle(15);

        // Step 1
        scene.overlay().showText(65)
                .text("The Storage Filtered Interface connects to a Storage Network").placeNearTarget();
        scene.idle(85);

        ElementLink<WorldSectionElement> chestHopper = scene.world().showIndependentSection(focus, Direction.EAST);
        ElementLink<WorldSectionElement> redstoneLever = scene.world().showIndependentSection(redstone, Direction.SOUTH);
        scene.idle(10);
        scene.world().moveSection(scene.world().showIndependentSection(util.select().position(sfi), Direction.EAST), util.vector().of(0, 0, 0), 0);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("It allows filtered automation using both Vanilla and Create blocks")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sfi, Direction.NORTH).add(0, 0.45, 0));
        scene.idle(85);

        // Step 2
        scene.addKeyframe();
        scene.overlay().showText(70)
                .text("Without a filter, any available item is extracted")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sfi, Direction.NORTH).add(-0.05, -0.15, 0));
        scene.idle(60);
        scene.world().toggleRedstonePower(util.select().fromTo(1, 1, 0, 1, 1, 1).add(util.select().position(lever)));
        scene.idle(10);

        for (int i = 0; i < 12; i++) {
            if (i >= 0) extractOne(scene, boxes.get(0).pos());  // copper
            if (i >= 4) extractOne(scene, boxes.get(1).pos());  // gold
            if (i >= 8) extractOne(scene, boxes.get(2).pos());  // cobble

            scene.idle(10);
        }

        scene.overlay().showControls(util.vector().blockSurface(chest, Direction.UP), Pointing.DOWN, 20).withItem(copper);
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(chest, Direction.UP), Pointing.DOWN, 20).withItem(gold);
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(chest, Direction.UP), Pointing.DOWN, 20).withItem(cobb);
        scene.idleSeconds(3);

        // Step 3
        scene.addKeyframe();
        scene.overlay().showFilterSlotInput(filter.add(0.2, -0.2, 0), Direction.NORTH, 70);
        scene.overlay().showText(70)
                .text("With a filter installed, only matching items can be extracted")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sfi, Direction.NORTH).add(-0.1, -0.1, 0));
        scene.idle(40);

        scene.overlay().showControls(filter.add(0.2, -0.2, 0), Pointing.RIGHT, 20).rightClick().withItem(copper);
        scene.idle(7);
        scene.world().setFilterData(util.select().position(util.grid().at(1, 2, 2)), StorageInterfaceFilteredEntity.class, copper);
        scene.idle(25);

        // Repopulate storage boxes
        populateAll(scene, boxes, 4);

        scene.idle(50);
        for (int i = 0; i < 4; i++) {
            extractOne(scene, boxes.get(0).pos);
            scene.idle(10);
        }

        scene.idle(40);

        // Step 4
        scene.addKeyframe();
        scene.world().hideIndependentSection(chestHopper, Direction.WEST);
        scene.world().hideIndependentSection(redstoneLever, Direction.NORTH);
        scene.idle(10);

        scene.world().moveSection(
                scene.world().showIndependentSection(util.select().position(0, 2, 2), Direction.EAST),
                util.vector().of(1, -1, 0),
                0
        );

        scene.world().moveSection(
                scene.world().showIndependentSection(util.select().position(hopper), Direction.EAST),
                util.vector().of(0, -1, 0),
                0
        );
        scene.idle(20);

        scene.overlay().showText(70)
                .text("Insertion is filtered as well")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(hopper, Direction.NORTH).add(0, 0, 0));

        scene.overlay().showControls(util.vector().blockSurface(hopper.below(), Direction.UP), Pointing.RIGHT, 20).rightClick().withItem(cobb);
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(hopper.below(), Direction.UP), Pointing.RIGHT, 20).rightClick().withItem(gold);
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(hopper.below(), Direction.UP), Pointing.RIGHT, 20).rightClick().withItem(copper);
        scene.idleSeconds(3);

        scene.overlay().showText(50)
                .text("Non-matching items will not enter the network")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sfi, Direction.NORTH).add(-0.1, -0.1, 0));

        for (int i = 1; i < 5; i++) {
            populateBox(scene, boxes.get(0), i);
            scene.idle(10);
        }
        scene.idle(25);

        scene.markAsFinished();
    }

    public static void filter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_interface_filtered_filter", "Filtering Scope");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos sfi = util.grid().at(1, 2, 2);
        BlockPos threshold = util.grid().at(1, 2, 3);
        Selection network = util.select().fromTo(3, 1, 2, 1, 2, 2);
        Selection redstone = util.select().fromTo(1, 1, 3, 2, 2, 3).add(util.select().position(2, 3, 2));
        Selection nixie = util.select().position(2, 3, 2);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);

        List<StorageBoxEntry> boxes = List.of(
                new StorageBoxEntry(util.grid().at(3, 1, 2), new ItemStack(Items.COPPER_INGOT)),
                new StorageBoxEntry(util.grid().at(2, 1, 2), new ItemStack(Items.GOLD_INGOT)),
                new StorageBoxEntry(util.grid().at(1, 1, 2), new ItemStack(Items.COBBLESTONE))
        );

        scene.world().showSection(network, Direction.DOWN);
        scene.idle(15);

        scene.overlay().showCenteredScrollInput(sfi, Direction.UP, 60);
        scene.overlay().showText(60)
                .text("Filter Scope determines how storage space is considered")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sfi, Direction.UP).add(-0.15, 0, 0));


        populateAll(scene, boxes, 192);

        scene.idle(75);
        scene.world().showIndependentSection(redstone, Direction.NORTH);
        scene.idle(20);

        Component seven = Component.literal("7%");
        Component four = Component.literal("4%");
        Component nine = Component.literal("9%");
        scene.world().modifyBlockEntityNBT(nixie, NixieTubeBlockEntity.class, nbt -> {
            nbt.putString("RawCustomText", seven.getString());
            nbt.putString("CustomText", Component.Serializer.toJson(seven));
        });

        scene.world().cycleBlockProperty(threshold, ThresholdSwitchBlock.LEVEL);
        scene.idle(25);

        scene.overlay().showOutline(PonderPalette.GREEN, "sBox", util.select().fromTo(3, 1, 2, 1, 1, 2).add(util.select().position(2, 2, 2)), 75);
        scene.overlay().showText(95).text("Without a filter applied, it functions as a Simple Storage Interface").placeNearTarget();
        scene.idle(105);

        scene.overlay().showControls(util.vector().blockSurface(sfi, Direction.NORTH).add(0.2, -0.2, 0), Pointing.RIGHT, 20).rightClick().withItem(gold);
        scene.idle(7);
        scene.world().setFilterData(util.select().position(sfi), StorageInterfaceFilteredEntity.class, gold);
        scene.idle(35);

        scene.overlay().showControls(util.vector().blockSurface(sfi, Direction.UP), Pointing.DOWN, 45).showing(Icons.I_INCLUDE_EMPTY);
        scene.overlay().showText(55)
                .text("Filtered items and Empty storage")
                .placeNearTarget();
        scene.idle(60);

        scene.world().modifyBlockEntityNBT(nixie, NixieTubeBlockEntity.class, nbt -> {
            nbt.putString("RawCustomText", four.getString());
            nbt.putString("CustomText", Component.Serializer.toJson(four));
        });

        scene.overlay().showOutline(PonderPalette.GREEN, "sBox", util.select().fromTo(2, 1, 2, 2, 2, 2), 55);
        scene.overlay().showText(75)
                .text("This scope will expose storage with matching stacks as well as empty storage")
                .placeNearTarget();
        scene.idle(85);

        scene.overlay().showControls(util.vector().blockSurface(sfi, Direction.UP), Pointing.DOWN, 45).showing(Icons.I_EXCLUDE_EMPTY);
        scene.overlay().showText(45)
                .text("Filtered items only")
                .placeNearTarget();
        scene.idle(55);

        scene.world().modifyBlockEntityNBT(nixie, NixieTubeBlockEntity.class, nbt -> {
            nbt.putString("RawCustomText", nine.getString());
            nbt.putString("CustomText", Component.Serializer.toJson(nine));
        });

        scene.overlay().showOutline(PonderPalette.GREEN, "sBox", util.select().position(2, 1, 2), 55);
        scene.overlay().showText(65)
                .text("This scope will expose storage with matching stacks only")
                .placeNearTarget();
        scene.idle(75);

        scene.markAsFinished();
    }
}
