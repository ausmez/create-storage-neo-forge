package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

import static net.fxnt.fxntstorage.container.StorageBox.STORAGE_USED;
import static net.fxnt.fxntstorage.container.StorageBox.VOID_UPGRADE;

public class StorageBoxScenes {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_box_intro", "Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);

        BlockPos cardboard = util.grid().at(3, 1, 1);
        BlockPos iron = util.grid().at(2, 1, 1);
        BlockPos weathered = util.grid().at(1, 1, 1);
        BlockPos andesite = util.grid().at(3, 2, 2);
        BlockPos copper = util.grid().at(2, 2, 2);
        BlockPos brass = util.grid().at(1, 2, 2);
        BlockPos hardened = util.grid().at(2, 3, 3);

        List<BlockPos> blockPosList = Arrays.asList(cardboard, iron, weathered, andesite, copper, brass, hardened);

        scene.overlay().showText(80).text("");
        scene.idle(90);

        for (BlockPos blockPos : blockPosList) {
            if (blockPos.equals(blockPosList.getFirst())) {
                scene.overlay().showText(30).text("").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(blockPos, Direction.NORTH).add(0, 0.1, 0));
            } else {
                scene.overlay().showText(30).text("").placeNearTarget().pointAt(util.vector().blockSurface(blockPos, Direction.NORTH).add(0, 0.1, 0));
            }
            scene.idle(40);
        }

        scene.markAsFinished();
    }

    public static void interact(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_box_interact", "Interacting with Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 2, 2), Direction.DOWN);
        scene.idle(10);

        Selection largeCog = util.select().position(4, 0, 5);
        BlockPos srcStorageBox = util.grid().at(4, 2, 2);
        BlockPos dstStorageBox = util.grid().at(0, 2, 2);
        BlockPos beltPos = util.grid().at(3, 1, 2);
        Vec3 indicatorLight = new Vec3(0.5, 2.5, 2.5);
        ItemStack sand = new ItemStack(Items.SAND);

        scene.overlay().showText(60).text("Display panel shows the total items stored and used percentage").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(srcStorageBox, Direction.NORTH).add(-0.1, 0.1, 0));
        scene.idle(70);

        scene.overlay().showControls(util.vector().blockSurface(srcStorageBox, Direction.NORTH).add(0, 0.15, 0), Pointing.DOWN, 30).rightClick().withItem(sand);
        scene.overlay().showText(60).text("Items can be inserted directly using the front panel").placeNearTarget().pointAt(util.vector().blockSurface(srcStorageBox, Direction.NORTH).add(-0.1, 0.1, 0));
        scene.world().modifyBlockEntity(srcStorageBox, StorageBoxEntity.class, (t) -> {
            for (int i = 0; i < 18; i++) {
                t.getItemHandler().setStackInSlot(i, sand.copyWithCount(64));
            }
        });
        scene.world().modifyBlock(srcStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(70);

        scene.overlay().showControls(util.vector().blockSurface(srcStorageBox, Direction.NORTH).add(0, 0.15, 0), Pointing.DOWN, 30).rightClick().whileSneaking();
        scene.overlay().showText(70).text("Sneak right-click with an empty hand to open the inventory").placeNearTarget().pointAt(util.vector().blockSurface(srcStorageBox, Direction.NORTH).add(-0.1, 0.1, 0));
        scene.idle(80);

        scene.overlay().showText(60).text("Indicator light shows the fill status of the storage box").attachKeyFrame().placeNearTarget().pointAt(indicatorLight);
        scene.idle(70);

        scene.world().showSection(largeCog, Direction.UP);
        for (int i = 3; i < 6; i++) {
            scene.idle(5);
            scene.world().showSection(util.select().position(3, 1, i), Direction.DOWN);
        }
        scene.overlay().showText(60).text("Blue indicates the storage box is empty").placeNearTarget().pointAt(indicatorLight);
        scene.world().modifyBlock(srcStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(30);

        scene.world().setKineticSpeed(util.select().everywhere(), 32.0F);
        scene.world().setKineticSpeed(largeCog, -16.0F);

        for (int i = 0; i < 14; ++i) {
            int finalI = i;
            scene.idle(4);
            switch (i) {
                case 2:
                    scene.world().modifyBlock(dstStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
                    scene.overlay().showText(70).text("Green indicates the box has items and available slots").placeNearTarget().pointAt(indicatorLight);
                    break;
                case 7:
                    scene.overlay().showText(80).text("Orange indicates all slots are filled, but not full stacks").placeNearTarget().pointAt(indicatorLight);
                    scene.world().modifyBlock(dstStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.SLOTS_FILLED), false);
                    scene.world().modifyBlockEntity(dstStorageBox, StorageBoxEntity.class, (t) -> {
                        for (int j = 0; j < t.getContainerSize(); j++) {
                            t.getItemHandler().setStackInSlot(j, sand.copyWithCount(32));
                        }
                    });
                    break;
                case 13:
                    scene.overlay().showText(60).text("Red indicates all slots and stacks are full").placeNearTarget().pointAt(indicatorLight);
                    scene.world().modifyBlock(dstStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL), false);
                    scene.world().modifyBlockEntity(dstStorageBox, StorageBoxEntity.class, (t) -> {
                        for (int j = 0; j < t.getContainerSize(); j++) {
                            t.getItemHandler().setStackInSlot(j, sand.copyWithCount(64));
                        }
                    });
                    break;
            }
            if (i > 1) {
                scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
                scene.world().modifyBlockEntity(dstStorageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(finalI, sand.copyWithCount(64)));
            }
            scene.idle(5);
            scene.world().modifyBlockEntity(srcStorageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(finalI, ItemStack.EMPTY));
            scene.world().createItemOnBelt(beltPos, Direction.EAST, sand);
            scene.idle(9);
        }

        scene.world().modifyBlockEntity(srcStorageBox, StorageBoxEntity.class, (t) -> {
            for (int i = 0; i < t.getContainerSize(); i++) {
                t.getItemHandler().setStackInSlot(i, (i < 4) ? sand.copyWithCount(64) : ItemStack.EMPTY);
            }
        });
        scene.idle(60);

        scene.overlay().showControls(util.vector().blockSurface(dstStorageBox, Direction.NORTH).add(0, 0.15, 0), Pointing.RIGHT, 30).rightClick().withItem(new ItemStack(AllItems.WRENCH.asItem()));
        scene.overlay().showText(60).text("Right-click with a wrench to activate Void Mode").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(dstStorageBox, Direction.NORTH).add(-0.1, 0.1, 0));
        scene.world().modifyBlockEntity(dstStorageBox, StorageBoxEntity.class, StorageBoxEntity::toggleVoidUpgrade);
        scene.world().modifyBlock(dstStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(VOID_UPGRADE, true), false);
        scene.idle(70);

        scene.overlay().showText(60).text("When enabled, items added beyond capacity will be voided").placeNearTarget().pointAt(util.vector().blockSurface(dstStorageBox, Direction.NORTH).add(-0.45, -0.25, 0));

        scene.world().createItemOnBelt(beltPos, Direction.EAST, sand);
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            scene.idle(4);
            scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
            scene.idle(5);
            scene.world().modifyBlockEntity(srcStorageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(finalI, ItemStack.EMPTY));
            scene.world().createItemOnBelt(beltPos, Direction.EAST, sand);
            scene.idle(9);
        }
        scene.world().modifyBlock(srcStorageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY), false);
        scene.idle(4);
        scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void filter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_box_filter", "Using the Filter Slot");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos largeCog = util.grid().at(0, 0, 5);
        BlockPos storageBox = util.grid().at(2, 2, 2);
        BlockPos brassFunnel = util.grid().at(1, 2, 2);
        Selection centerColumn = util.select().column(2, 2);
        Selection belt = util.select().fromTo(1, 1, 2, 0, 1, 2);
        Selection shaft = util.select().fromTo(1, 1, 3, 1, 1, 6);
        Vec3 filter = util.vector().blockSurface(storageBox, Direction.NORTH).add(new Vec3(0, 0.2, 0));
        ItemStack brassHand = AllItems.BRASS_HAND.asStack();
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack coal = new ItemStack(Items.COAL);

        scene.world().showSection(centerColumn, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showFilterSlotInput(filter, Direction.NORTH, 100);
        scene.overlay().showControls(filter, Pointing.DOWN, 30).rightClick().withItem(diamond);
        scene.idle(20);
        scene.overlay().showText(80).text("Using items on the filter slot will restrict the items allowed for insertion through the front panel").attachKeyFrame().placeNearTarget().pointAt(filter.add(-0.1, 0, 0));
        scene.world().setFilterData(util.select().position(storageBox), StorageBoxEntity.class, diamond);
        scene.idle(90);

        scene.overlay().showControls(util.vector().blockSurface(storageBox, Direction.NORTH).add(0, -0.2, 0), Pointing.RIGHT, 30).showing(AllIcons.I_MTD_CLOSE).withItem(coal);
        scene.idle(60);
        scene.overlay().showControls(util.vector().blockSurface(storageBox, Direction.NORTH).add(0, -0.2, 0), Pointing.RIGHT, 30).showing(AllIcons.I_CONFIRM).withItem(diamond);
        scene.world().modifyBlock(storageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.world().modifyBlockEntity(storageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, diamond));
        scene.idle(60);

        scene.overlay().showControls(util.vector().blockSurface(storageBox, Direction.NORTH).add(0, -0.2, 0), Pointing.RIGHT, 90).showing(AllIcons.I_RMB).withItem(brassHand);
        scene.idle(10);
        scene.overlay().showText(100).text("Double right-click with an empty hand and filter set to insert all matching items from inventory").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(storageBox, Direction.NORTH).add(-0.2, -0.1, 0));
        scene.world().modifyBlockEntity(storageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, diamond.copyWithCount(6)));
        scene.idle(110);

        scene.world().showSection(belt, Direction.EAST);
        scene.world().showSection(util.select().position(brassFunnel), Direction.DOWN);
        scene.world().showSection(shaft, Direction.DOWN);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);

        scene.overlay().showText(60).text("Use brass funnels or smart passers to filter items for extraction").attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(util.grid().at(1, 2, 2)));
        scene.world().setFilterData(util.select().position(brassFunnel), FunnelBlockEntity.class, diamond);
        for (int i = 0; i < 6; i++) {
            scene.idle(9);
            scene.world().createItemOnBelt(util.grid().at(1, 1, 2), Direction.EAST, diamond);
            scene.world().modifyBlockEntity(storageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, diamond.copyWithCount(t.getItem(0).getCount() - 1)));
            scene.idle(9);
        }
        scene.world().modifyBlock(storageBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY), false);
        scene.world().modifyBlockEntity(storageBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, ItemStack.EMPTY));
        scene.idle(40);

        scene.markAsFinished();
    }

}
