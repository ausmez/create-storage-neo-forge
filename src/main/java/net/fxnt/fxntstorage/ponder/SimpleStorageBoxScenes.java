package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.util.Arrays;
import java.util.List;

import static net.fxnt.fxntstorage.container.StorageBox.STORAGE_USED;

public class SimpleStorageBoxScenes {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_storage_intro", "Simple Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        List<Selection> storageBoxes;
        Selection supportBlocks1, supportBlocks2;
        BlockState state1, state2, state3;

        if (ModList.get().isLoaded(ModCompats.VANILLA_BACKPORT)) {
            storageBoxes = Arrays.asList(
                    util.select().position(4, 1, 1), util.select().position(3, 1, 1),
                    util.select().position(2, 1, 1), util.select().position(1, 1, 1),
                    util.select().position(0, 1, 1), util.select().position(4, 2, 2), util.select().position(3, 2, 2),
                    util.select().position(2, 2, 2), util.select().position(1, 2, 2), util.select().position(0, 2, 2),
                    util.select().position(3, 3, 3), util.select().position(1, 3, 3)
            );
            supportBlocks1 = util.select().fromTo(4, 1, 2, 0, 1, 2);
            supportBlocks2 = util.select().fromTo(3, 1, 3, 1, 2, 3);
            state1 = ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state2 = ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state3 = ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
        } else {
            storageBoxes = Arrays.asList(
                    util.select().position(4, 1, 1), util.select().position(3, 1, 1),
                    util.select().position(2, 1, 1), util.select().position(1, 1, 1),
                    util.select().position(0, 1, 1), util.select().position(3, 2, 2),
                    util.select().position(2, 2, 2), util.select().position(1, 2, 2),
                    util.select().position(3, 3, 3), util.select().position(2, 3, 3),
                    util.select().position(1, 3, 3)
            );
            supportBlocks1 = util.select().fromTo(3, 1, 2, 1, 1, 2);
            supportBlocks2 = util.select().fromTo(3, 1, 3, 1, 2, 3);
            state1 = ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state2 = ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state3 = ModBlocks.SIMPLE_STORAGE_BOX_WARPED.getDefaultState().setValue(SimpleStorageBox.STORAGE_USED, EnumProperties.StorageUsed.FULL);
        }

        int counter = 0;
        for (Selection selection : storageBoxes) {
            scene.world().showSection(selection, Direction.DOWN);
            scene.idle(4);
            if (counter == 5) {
                scene.world().showSection(supportBlocks1, Direction.NORTH);
                scene.world().showSection(supportBlocks2, Direction.NORTH);
            }
            counter++;
        }

        scene.overlay().showText(60).text("Simple Storage Boxes are available in common wood types").attachKeyFrame();
        scene.idle(70);
        scene.overlay().showText(60).text("All Simple Storage Boxes have the same attributes");
        scene.idle(70);
        scene.overlay().showText(60).text("Each box can hold up to 32x max stack size of one item").attachKeyFrame();
        scene.idle(70);

        ItemStack sand = new ItemStack(Items.SAND);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        BlockPos b1 = util.grid().at(2, 1, 1);
        BlockPos b2 = util.grid().at(3, 2, 2);
        BlockPos b3 = util.grid().at(1, 3, 3);

        scene.world().modifyBlockEntity(b1, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(sand);
            t.setItem(0, sand.copyWithCount(2048));
        });
        scene.world().modifyBlock(b1, s -> state1, false);
        scene.overlay().showText(60).text("Sand Block = 2048 (64 per stack)").placeNearTarget().pointAt(util.vector().blockSurface(b1, Direction.NORTH).add(-0.2, 0.25, 0));
        scene.idle(65);

        scene.world().modifyBlockEntity(b2, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(pearl);
            t.setItem(0, pearl.copyWithCount(512));
        });
        scene.world().modifyBlock(b2, s -> state2, false);
        scene.overlay().showText(60).text("Ender Pearl = 512 (16 per stack)").placeNearTarget().pointAt(util.vector().blockSurface(b2, Direction.NORTH).add(-0.2, 0.25, 0));
        scene.idle(65);

        scene.world().modifyBlockEntity(b3, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(water);
            t.setItem(0, water.copyWithCount(32));
        });
        scene.world().modifyBlock(b3, s -> state3, false);
        scene.overlay().showText(60).text("Water Bucket = 32 (1 per stack)").placeNearTarget().pointAt(util.vector().blockSurface(b3, Direction.NORTH).add(-0.2, 0.25, 0));
        scene.idle(80);

        scene.markAsFinished();
    }

    public static void interact(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_storage_interact", "Interacting with Simple Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos sSBox = util.grid().at(2, 2, 2);
        BlockPos funnel = util.grid().at(3, 2, 2);
        BlockPos largeCog = util.grid().at(3, 0, 5);
        Selection belt = util.select().fromTo(4, 1, 2, 3, 1, 2);
        Selection shaft = util.select().fromTo(4, 1, 3, 4, 1, 5);

        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        scene.world().setKineticSpeed(util.select().everywhere(), 32.0F);
        scene.world().setKineticSpeed(util.select().position(largeCog), -16.0F);

        scene.world().showSection(util.select().column(2, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(50).text("The front panel displays the percentage used").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        scene.idle(60);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 80).rightClick().withItem(gold);
        scene.overlay().showText(80).text("Items can be inserted directly by right-clicking the front panel").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(10);
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(gold);
            t.setItem(0, gold.copyWithCount(64));
        });
        scene.world().modifyBlock(sSBox, (s) -> ModBlocks.SIMPLE_STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(80);

        scene.world().showSection(belt, Direction.EAST);
        scene.world().showSection(shaft, Direction.DOWN);
        scene.world().showSection(util.select().position(funnel), Direction.DOWN);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);
        scene.idle(15);

        scene.overlay().showText(80).text("...or via automation").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(funnel, Direction.UP));
        for (int j = 0; j < 6; j++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(4, 4, 2), new Vec3(0, 0, 0), gold);
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(4, 1, 2), Direction.EAST, gold);
            if (j > 0) {
                scene.idle(4);
                scene.world().removeItemsFromBelt(util.grid().at(3, 1, 2));
                int finalJ = j + 64;
                scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, gold.copyWithCount(finalJ)));
            }
            scene.idle(2);
        }
        scene.idle(17);
        scene.world().removeItemsFromBelt(util.grid().at(3, 1, 2));
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, gold.copyWithCount(t.getItem(0).getCount() + 1)));
        scene.idle(50);

        scene.overlay().showText(50).text("The filter is set automatically when the first item is added").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(60);

        scene.overlay().showText(60).text("Use the wrench to remove the filter (only when the box is empty)").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(70);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 50).leftClick();
        scene.overlay().showText(50).independent().text("Left-click the front panel to extract a single item").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 2, 1), new Vec3(0, 0, 0), gold);
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, gold.copyWithCount(t.getItem(0).getCount() - 1)));
        scene.idle(60);

        scene.world().modifyEntity(itemEntity, Entity::discard);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 50).leftClick().whileSneaking();
        scene.overlay().showText(50).text("Left-click while sneaking to remove a single item stack").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 2, 1), new Vec3(0, 0, 0), gold.copyWithCount(64));
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, gold.copyWithCount(t.getItem(0).getCount() - 64)));
        scene.idle(60);

        scene.world().modifyEntity(itemEntity, Entity::discard);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 60).rightClick().whileSneaking();
        scene.overlay().showText(60).text("Right-click with an empty hand to access the inventory menu").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        scene.idle(70);

        scene.markAsFinished();
    }

    public static void upgrades(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_storage_upgrades", "Simple Storage Box Upgrades");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos leftBox = util.grid().at(3, 2, 2);
        BlockPos rightBox = util.grid().at(1, 2, 2);
        ItemStack vUpgrade = new ItemStack(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
        ItemStack cUpgrade = new ItemStack(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get());
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);

        scene.world().showSection(util.select().layers(1, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(70).text("Two upgrades are available for Simple Storage Boxes, Void Upgrade and Capacity Upgrades").attachKeyFrame().placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(70).text("Apply an upgrade by interacting with the front panel using the upgrade item...").placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(105).text("...or via the interface by right-clicking while sneaking").placeNearTarget();
        scene.idle(115);

        scene.overlay().showControls(util.vector().blockSurface(leftBox, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(vUpgrade);
        scene.overlay().showText(65).text("Void upgrade will void any item added beyond the max capacity").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(leftBox, Direction.WEST));
        scene.idle(40);
        scene.world().modifyBlock(leftBox, (s) -> ModBlocks.SIMPLE_STORAGE_BOX.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL), false);
        scene.world().modifyBlockEntity(leftBox, SimpleStorageBoxEntity.class, (t) -> {
            t.setItem(3, vUpgrade);
            t.voidUpgrade = true;
            t.setFilter(iron);
            t.setItem(0, iron.copyWithCount(2048));
        });
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(rightBox, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(cUpgrade);
        scene.overlay().showText(65).text("Capacity upgrade will double the storage for each upgrade").placeNearTarget().pointAt(util.vector().blockSurface(rightBox, Direction.WEST));
        scene.idle(40);
        scene.world().modifyBlock(rightBox, (s) -> ModBlocks.SIMPLE_STORAGE_BOX.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL), false);
        scene.world().modifyBlockEntity(rightBox, SimpleStorageBoxEntity.class, (t) -> {
            for (int i = 4; i < 13; i++) {
                t.setItem(i, cUpgrade);
            }
            t.setFilter(gold);
            t.setItem(0, gold.copyWithCount(1048576));
        });
        scene.idle(50);

        scene.overlay().showText(70).text("A total of 9 capacity upgrades and 1 void upgrade can be added").placeNearTarget();

        scene.markAsFinished();
    }

}
