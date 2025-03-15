package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.EntityElement;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.utility.Pointing;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;
import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBox.STORAGE_USED;

public class StorageControllerScenes {

    public static void intro(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("storage_controller_intro", "Storage Controller");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos controller = util.grid.at(2, 2, 2);
        BlockPos leftBox = util.grid.at(4, 2, 2);
        BlockPos rightBox = util.grid.at(0, 2, 2);
        Selection storage = util.select.fromTo(3, 2, 2, 1, 3, 2);
        Selection trim = util.select.fromTo(4, 1, 3, 0, 2, 3);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        scene.world.showSection(storage, Direction.DOWN);
        scene.world.showSection(util.select.fromTo(3, 1, 2, 1, 1, 2), Direction.DOWN);
        scene.idle(25);

        scene.overlay.showText(65).text("Serves as the main input and output point for a Storage Network").attachKeyFrame().placeNearTarget().pointAt(util.vector.blockSurface(controller, Direction.NORTH).add(-0.15, 0, 0));
        scene.idle(75);

        scene.overlay.showText(40).text("When placed next to Simple Storage Boxes...").placeNearTarget().pointAt(util.vector.blockSurface(util.grid.at(1, 2, 2), Direction.NORTH));
        scene.idle(75);
        scene.overlay.showText(65).text("...it creates a network of connected storage").placeNearTarget().pointAt(util.vector.blockSurface(util.grid.at(1, 2, 3), Direction.NORTH));
        scene.idle(20);
        scene.overlay.showOutline(PonderPalette.GREEN, "storageoutline", storage, 70);
        scene.idle(95);

        scene.addKeyframe();
        scene.overlay.showControls((new InputWindowElement(util.vector.blockSurface(controller, Direction.NORTH), Pointing.RIGHT)).withItem(gold), 25);
        scene.overlay.showText(35).text("Items can be inserted into the network via the front panel").placeNearTarget().pointAt(util.vector.blockSurface(controller, Direction.NORTH));
        scene.idle(15);
        scene.world.modifyBlockEntity(util.grid.at(3, 3, 2), SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(gold);
            t.setItem(0, gold.copyWithCount(64));
        });
        scene.world.modifyBlock(util.grid.at(3, 3, 2), (s) -> ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(60);

        // TODO: flesh this out
        scene.overlay.showText(35).text("Or via automation using funnels, chutes, hoppers or passers").placeNearTarget().pointAt(util.vector.blockSurface(controller, Direction.NORTH));

        scene.overlay.showControls((new InputWindowElement(util.vector.blockSurface(controller, Direction.NORTH), Pointing.RIGHT)).withItem(diamond), 25);
        scene.idle(15);
        scene.world.modifyBlockEntity(util.grid.at(2, 3, 2), SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(diamond);
            t.setItem(0, diamond.copyWithCount(64));
        });
        scene.world.modifyBlock(util.grid.at(2, 3, 2), (s) -> ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(60);

        scene.world.showSection(util.select.position(controller.north()), Direction.DOWN);
        scene.idle(10);
        scene.overlay.showText(35).text("Items can can only be extracted with funnels, chutes, hoppers or passers").placeNearTarget().pointAt(util.vector.blockSurface(controller, Direction.NORTH));
        scene.idle(20);
        ElementLink<EntityElement> itemEntity = scene.world.createItemEntity(util.vector.centerOf(controller.north()).add(0, -0.2, 0), util.vector.of(0, 0, 0), gold.copyWithCount(64));
        scene.world.modifyBlockEntity(util.grid.at(3, 3, 2), SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(new ItemStack(Items.AIR));
            t.setItem(0, ItemStack.EMPTY);
        });
        scene.world.modifyBlock(util.grid.at(3, 3, 2), (s) -> ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY), false);
        scene.idle(55);

        scene.world.modifyEntity(itemEntity, Entity::discard);

        scene.world.hideSection(storage.add(util.select.position(controller.north())), Direction.UP);
        scene.world.hideSection(util.select.fromTo(3, 1, 2, 1, 1, 2), Direction.UP);
        scene.idle(20);
        ElementLink<WorldSectionElement> storageLink = scene.world.showIndependentSection(trim, Direction.DOWN);
        scene.world.moveSection(storageLink, util.vector.of(0, 0, -1), 0);
        scene.idle(40);

        scene.overlay.showText(60).text("Simple Storage Boxes can be connected by Storage Trim...").attachKeyFrame().placeNearTarget().pointAt(util.vector.blockSurface(util.grid.at(0, 1, 2), Direction.WEST));
        scene.overlay.showOutline(PonderPalette.GREEN, "trimoutline", util.select.fromTo(4, 1, 2, 0, 1, 2), 45);
        scene.overlay.showOutline(PonderPalette.GREEN, "leftboxoutline", util.select.position(leftBox), 45);
        scene.overlay.showOutline(PonderPalette.GREEN, "rightboxoutline", util.select.position(rightBox), 45);
        scene.idle(70);

        scene.overlay.showText(50).text("...to a Storage Network over larger areas (up to 64 blocks)").placeNearTarget().pointAt(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.NORTH).add(-0.2, -0.2, 0));
        scene.idle(60);
        scene.world.destroyBlock(util.grid.at(2, 1, 3));
        scene.idle(10);
        scene.world.modifyBlock(util.grid.at(2, 2, 3), (s) -> ModBlocks.STORAGE_CONTROLLER.get().defaultBlockState().setValue(CONNECTED, false), false);
        scene.idle(5);
        scene.overlay.showOutline(PonderPalette.RED, "leftboxoutline", util.select.position(leftBox), 45);
        scene.overlay.showOutline(PonderPalette.RED, "rightboxoutline", util.select.position(rightBox), 45);
        scene.idle(55);

        scene.overlay.showText(65).text("Illuminates when connected to at least one Simple Storage Box in the network").placeNearTarget().pointAt(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.NORTH).add(-0.2, -0.2, 0));

        scene.world.restoreBlocks(util.select.position(2, 1, 3));
        scene.idle(10);
        scene.world.modifyBlock(util.grid.at(2, 2, 3), (s) -> ModBlocks.STORAGE_CONTROLLER.get().defaultBlockState().setValue(CONNECTED, true), false);
        scene.idle(5);
        scene.overlay.showOutline(PonderPalette.GREEN, "trimoutline", util.select.fromTo(4, 1, 2, 0, 1, 2), 45);
        scene.overlay.showOutline(PonderPalette.GREEN, "leftboxoutline", util.select.position(leftBox), 45);
        scene.overlay.showOutline(PonderPalette.GREEN, "rightboxoutline", util.select.position(rightBox), 45);
        scene.idle(30);

        scene.markAsFinished();
    }
}
