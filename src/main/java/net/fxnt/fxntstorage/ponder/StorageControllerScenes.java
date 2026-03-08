package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.util.Icons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;
import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBox.STORAGE_USED;

public class StorageControllerScenes {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_controller_intro", "Storage Controller");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos controller = util.grid().at(2, 2, 2);
        BlockPos leftBox = util.grid().at(4, 2, 2);
        BlockPos rightBox = util.grid().at(0, 2, 2);
        BlockPos leftBox2 = util.grid().at(4, 2, 3);
        BlockPos rightBox2 = util.grid().at(0, 2, 3);
        BlockPos topBox = util.grid().at(2, 3, 2);
        Selection storage = util.select().fromTo(3, 2, 2, 1, 3, 2);
        Selection trim = util.select().fromTo(4, 1, 3, 0, 1, 3);
        Selection belt = util.select().fromTo(2, 1, 0, 2, 2, 1);
        BlockPos funnel = util.grid().at(2, 2, 1);
        BlockPos gearbox = util.grid().at(3, 2, 1);
        BlockPos largeCog = util.grid().at(5, 0, 0);
        Selection smallCog = util.select().fromTo(5, 1, 1, 3, 1, 1);
        ItemStack brass_ingot = AllItems.BRASS_INGOT.asStack();
        ItemStack copper_block = new ItemStack(Items.COPPER_BLOCK);

        scene.world().showSection(storage, Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 1, 1, 2), Direction.DOWN);
        scene.idle(25);

        scene.overlay().showText(70).text("The Storage Controller serves as the central input and output of a Storage Network").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(controller, Direction.NORTH).add(-0.15, 0, 0));
        scene.idle(85);

        scene.overlay().showText(60).text("Place it adjacent to Simple Storage Boxes...").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(1, 2, 2), Direction.NORTH));
        scene.idle(75);
        scene.overlay().showText(65).text("...to automatically form a connected storage network").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(1, 2, 3), Direction.NORTH));
//        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.GREEN, "storageoutline", storage, 70);
        scene.idle(95);

        scene.addKeyframe();
        scene.overlay().showText(45).text("Items can be inserted into the network through the front panel...").placeNearTarget().pointAt(util.vector().blockSurface(controller, Direction.NORTH));
        scene.idle(35);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.RIGHT, 15).withItem(brass_ingot);
        scene.idle(10);
        scene.world().modifyBlockEntity(util.grid().at(3, 3, 2), SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(brass_ingot);
            t.getItemHandler().setStackInSlot(0, brass_ingot.copyWithCount(64));
        });
        scene.world().modifyBlock(util.grid().at(3, 3, 2), (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(60);

        scene.world().showSection(belt, Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(smallCog, Direction.WEST);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);
        scene.world().setKineticSpeed(util.select().everywhere(), 32.0F);
        scene.world().setKineticSpeed(util.select().position(largeCog), -16.0F);
        scene.idle(25);

        scene.overlay().showText(35).text("...or via automation using funnels, chutes, hoppers or passers").placeNearTarget().pointAt(util.vector().blockSurface(controller, Direction.NORTH));

        for (int i = 0; i < 4; i++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 4, 0).add(0, 0, -0.35), new Vec3(0, 0, 0), copper_block.copyWithCount(16));
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(2, 1, 0), Direction.NORTH, copper_block.copyWithCount(16));
            if (i == 1) {
                scene.world().modifyBlock(topBox, (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState()
                        .setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
                scene.world().modifyBlockEntity(topBox, SimpleStorageBoxEntity.class, (t) -> t.setFilter(copper_block));
            }
            if (i > 0) {
                scene.idle(4);
                scene.world().removeItemsFromBelt(funnel.below());
                scene.world().flapFunnel(funnel, false);
                int finalJ = i * 16;
                scene.world().modifyBlockEntity(topBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, copper_block.copyWithCount(finalJ)));
            }
            scene.idle(2);
        }
        scene.idle(17);
        scene.world().modifyBlockEntity(topBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, copper_block.copyWithCount(16 * 4)));
        scene.world().removeItemsFromBelt(funnel.below());
        scene.world().flapFunnel(funnel, false);

        scene.idle(55);

        scene.world().hideSection(util.select().position(3, 1, 1), Direction.UP);
        scene.world().setKineticSpeed(belt, 0F);
        scene.idle(20);
        ElementLink<WorldSectionElement> gBox = scene.world().showIndependentSection(util.select().position(gearbox), Direction.DOWN);
        scene.world().moveSection(gBox, util.vector().of(0, -1, 0), 0);
        scene.world().setKineticSpeed(belt, -32.0F);
        scene.idle(5);

        scene.overlay().showText(35).text("Items can can only be extracted using funnels, chutes, hoppers or passers").placeNearTarget().pointAt(util.vector().blockSurface(controller, Direction.NORTH));
        scene.idle(20);

        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(funnel.below(), Direction.SOUTH, brass_ingot);
            scene.world().modifyBlockEntity(topBox.east(), SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, brass_ingot.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() - 1)));
            scene.idle(19);
        }

        scene.idle(55);

        scene.world().hideSection(storage, Direction.UP);
        scene.world().hideSection(util.select().fromTo(3, 1, 2, 1, 1, 2), Direction.UP);
        scene.world().hideSection(belt, Direction.NORTH);
        scene.world().hideIndependentSection(gBox, Direction.NORTH);
        scene.world().hideSection(smallCog, Direction.EAST);
        scene.world().hideSection(util.select().position(largeCog), Direction.DOWN);
        scene.idle(20);
        ElementLink<WorldSectionElement> storageLink = scene.world().showIndependentSection(trim, Direction.DOWN);
        ElementLink<WorldSectionElement> storageBoxes = scene.world().showIndependentSection(util.select().position(4, 2, 3).add(util.select().position(0, 2, 3)), Direction.DOWN);
        scene.world().moveSection(storageLink, util.vector().of(0, 0, -1), 0);
        scene.world().moveSection(storageBoxes, util.vector().of(0, 0, -1), 0);
        scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
        scene.idle(40);

        scene.overlay().showText(60).text("Connect Simple Storage Boxes using Storage Trim...").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(0, 1, 2), Direction.WEST));
        scene.overlay().showOutline(PonderPalette.GREEN, "network", util.select().fromTo(4, 2, 2, 4, 1, 2)
                        .add(util.select().fromTo(3, 1, 2, 0, 1, 2))
                        .add(util.select().position(0, 2, 2))
                , 140);
        scene.idle(85);

        scene.overlay().showText(60).text("...to extend the network over large areas (up to 64 blocks)").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH).add(-0.2, -0.2, 0));
        scene.idle(75);
        scene.world().destroyBlock(util.grid().at(2, 1, 3));
        scene.idle(10);
        scene.world().modifyBlock(util.grid().at(2, 2, 2), (s) -> ModBlocks.STORAGE_CONTROLLER.get().defaultBlockState().setValue(CONNECTED, false), false);
        scene.idle(5);
        scene.overlay().showOutline(PonderPalette.RED, "leftboxoutline", util.select().position(leftBox), 45);
        scene.overlay().showOutline(PonderPalette.RED, "rightboxoutline", util.select().position(rightBox), 45);
        scene.idle(55);

        scene.overlay().showText(80).text("The controller lights up when connected to at least one Simple Storage Box").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH).add(-0.2, -0.2, 0));

        scene.world().restoreBlocks(util.select().position(2, 1, 3));
        scene.idle(10);
        scene.world().modifyBlock(util.grid().at(2, 2, 2), (s) -> ModBlocks.STORAGE_CONTROLLER.get().defaultBlockState().setValue(CONNECTED, true), false);
        scene.idle(5);
        scene.overlay().showOutline(PonderPalette.GREEN, "network", util.select().fromTo(4, 2, 2, 4, 1, 2)
                        .add(util.select().fromTo(3, 1, 2, 0, 1, 2))
                        .add(util.select().position(0, 2, 2))
                , 45);

        scene.idle(90);

        scene.world().showSection(util.select().position(3, 2, 2), Direction.NORTH);
        scene.world().showSection(util.select().position(1, 2, 2), Direction.NORTH);

        Vec3 filter = util.vector().blockSurface(controller, Direction.UP);
        scene.overlay().showText(80).text("The Insertion Mode on the controller can be set to use empty storage boxes when storage is full").placeNearTarget().pointAt(filter.add(-0.1, 0, 0)).attachKeyFrame();
        scene.overlay().showFilterSlotInput(filter, Direction.UP, 80);
        scene.idle(95);

        scene.overlay().showControls(filter.add(0, 0.2, 0), Pointing.DOWN, 80).showing(Icons.I_ALLOW_EMPTY_FILL);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .pointAt(topBox.getBottomCenter())
                .placeNearTarget()
                .text("This setting will use any empty storage boxes when existing boxes are full");

        scene.idle(80);

        scene.world().showSection(belt, Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(smallCog, Direction.WEST);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);
        scene.world().setKineticSpeed(util.select().everywhere(), 32.0F);
        scene.world().setKineticSpeed(util.select().position(largeCog), -16.0F);
        scene.idle(25);

        for (int i = 0; i < 4; i++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 4, 0).add(0, 0, -0.35), new Vec3(0, 0, 0), brass_ingot);
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(2, 1, 0), Direction.NORTH, brass_ingot);
            if (i == 1) {
                scene.world().modifyBlock(leftBox2, (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState()
                        .setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
                scene.world().modifyBlockEntity(leftBox2, SimpleStorageBoxEntity.class, (t) -> t.setFilter(brass_ingot));
            }
            if (i > 0) {
                scene.idle(4);
                scene.world().removeItemsFromBelt(funnel.below());
                scene.world().flapFunnel(funnel, false);
                int finalJ = i;
                scene.world().modifyBlockEntity(leftBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, brass_ingot.copyWithCount(finalJ)));
            }
            scene.idle(2);
        }
        scene.idle(17);
        scene.world().modifyBlockEntity(leftBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, brass_ingot.copyWithCount(4)));
        scene.world().removeItemsFromBelt(funnel.below());
        scene.world().flapFunnel(funnel, false);

        scene.overlay().showControls(filter.add(0, 0.2, 0), Pointing.DOWN, 80).showing(Icons.I_DENY_EMPTY_FILL);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .pointAt(topBox.getBottomCenter())
                .placeNearTarget()
                .text("This setting will insert items into existing storage boxes only");
        scene.idle(80);

        for (int i = 5; i < 9; i++) {
            ItemStack stack = (i > 6) ? copper_block : brass_ingot;
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 4, 0).add(0, 0, -0.35), new Vec3(0, 0, 0), stack);
            scene.idle(11);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(2, 1, 0), Direction.NORTH, stack);
            scene.idle(3);

            if (i == 6) {
                scene.overlay().showOutline(PonderPalette.GREEN, "controller", util.select().position(controller), 20);
                scene.overlay().showOutline(PonderPalette.GREEN, "leftbox2", util.select().position(leftBox2.north()), 20);
            }

            if (i > 5 && i < 8) {
                scene.world().removeItemsFromBelt(funnel.below());
                scene.world().flapFunnel(funnel, false);
                int finalJ = i;
                scene.world().modifyBlockEntity(leftBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, brass_ingot.copyWithCount(finalJ)));
                scene.idle(2);
            }

        }
        scene.overlay().showOutline(PonderPalette.RED, "controller", util.select().position(controller), 20);
        scene.idle(20);

        scene.overlay().showText(60).text("Items cannot enter the network until a storage box has been allocated for them").pointAt(Vec3.atCenterOf(controller.north())).placeNearTarget();
        scene.idle(65);

        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(0, 2, 2), Direction.NORTH), Pointing.RIGHT, 15).withItem(copper_block);
        scene.idle(10);
        scene.world().modifyBlockEntity(rightBox2, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(copper_block);
            t.getItemHandler().insertItem(0, copper_block, false);
        });
        scene.world().modifyBlock(rightBox2, (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(25);

        scene.world().modifyBlock(rightBox2, (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState()
                .setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.world().modifyBlockEntity(rightBox2, SimpleStorageBoxEntity.class, (t) -> t.setFilter(copper_block));

        scene.overlay().showOutline(PonderPalette.GREEN, "controller", util.select().position(controller), 20);
        scene.overlay().showOutline(PonderPalette.GREEN, "rightbox2", util.select().position(rightBox2.north()), 20);
        scene.idle(25);

        for (int i = 3; i < 5; i++) {
            scene.world().modifyBlockEntity(rightBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, copper_block.copyWithCount(2)));
            scene.world().removeItemsFromBelt(funnel.below());
            scene.world().flapFunnel(funnel, false);

            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 4, 0).add(0, 0, -0.35), new Vec3(0, 0, 0), copper_block);
            scene.idle(11);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(2, 1, 0), Direction.NORTH, copper_block);

            scene.world().modifyBlock(rightBox2, (s) -> ModBlocks.SIMPLE_STORAGE_BOX_OAK.get().defaultBlockState()
                    .setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
            scene.world().modifyBlockEntity(topBox, SimpleStorageBoxEntity.class, (t) -> t.setFilter(copper_block));

            scene.idle(3);
            scene.world().removeItemsFromBelt(funnel.below());
            scene.world().flapFunnel(funnel, false);
            int finalJ = i;
            scene.world().modifyBlockEntity(rightBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, copper_block.copyWithCount(finalJ)));

            scene.idle(2);
        }
        scene.idle(17);
        scene.world().modifyBlockEntity(rightBox2, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, copper_block.copyWithCount(5)));
        scene.world().removeItemsFromBelt(funnel.below());
        scene.world().flapFunnel(funnel, false);
    }

    public static void highlighting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_controller_highlighting", "Highlighting Network Components");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos controller = util.grid().at(2, 2, 2);

        scene.world().showIndependentSection(util.select().layers(1, 3), Direction.DOWN);

        scene.overlay().showText(60).text("Network highlighting visually displays all connected storage in the system").placeNearTarget().attachKeyFrame();
        scene.idle(65);

        scene.overlay().showText(80).text("Right-click the controller front panel with a wrench to enable network highlighting").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH).add(-0.2, 0.2, 0));

        scene.idle(85);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.RIGHT, 15).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(15);
        scene.addInstruction(
                new CsOutlineInstruction(PonderPalette.OUTPUT, "network", util.select().fromTo(4, 2, 1, 4, 1, 1)
                        .add(util.select().fromTo(4, 1, 2, 0, 1, 2))
                        .add(util.select().fromTo(0, 2, 3, 0, 1, 3))
                        .add(util.select().fromTo(0, 1, 4, 2, 1, 4))
                        .add(util.select().fromTo(2, 2, 4, 2, 3, 4))
                        .add(util.select().position(controller))
                        , 120)
        );
        scene.idle(45);
        scene.overlay().showText(60).text("Right-click again to turn highlighting off").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH).add(-0.2, 0.2, 0));
        scene.idle(65);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.RIGHT, 15).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(45);

        scene.markAsFinished();
    }
}
