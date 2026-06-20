package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBox;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ReserveStorageBoxScenes {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reserve_storage_intro", "Reserve Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().column(2, 2), Direction.DOWN);

        BlockPos blockPos = util.grid().at(2, 2, 2);
        Selection largeCog = util.select().position(1, 0, 5);
        Selection smallCogs = util.select().fromTo(3, 1, 3, 1, 1, 3);
        Selection shaftCog = util.select().fromTo(2, 1, 4, 2, 1, 5);
        Selection eastBelt = util.select().fromTo(4, 1, 2, 3, 2, 2);
        Selection westBelt = util.select().fromTo(1, 1, 2, 0, 2, 2);

        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);

        scene.idle(20);
        scene.overlay().showText(50)
                .attachKeyFrame()
                .placeNearTarget()
                .text("Reserve Storage Boxes maintain a minimum stock of selected items");
        scene.idle(65);

        scene.overlay().showText(50)
                .placeNearTarget()
                .text("Configure up to 9 items and the quantity to keep in reserve");
        scene.idle(65);

        scene.overlay().showText(50)
                .placeNearTarget()
                .text("Reserved items and quantities can be configured in the block's menu");
        scene.idle(65);

        CsInputWindowElement.showControls(scene, util.vector().blockSurface(blockPos, Direction.NORTH).add(0, 0.5, 0), Pointing.DOWN, 25).withItem(gold).scroll().withCount(6);
        scene.idle(25);
        scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, e ->
                e.setItem(27, gold.copyWithCount(6)));
        scene.idle(15);

        CsInputWindowElement.showControls(scene, util.vector().blockSurface(blockPos, Direction.NORTH).add(0, 0.5, 0), Pointing.DOWN, 25).withItem(iron).scroll().withCount(6);
        scene.idle(25);
        scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, e ->
                e.setItem(28, iron.copyWithCount(6)));
        scene.idle(15);

        CsInputWindowElement.showControls(scene, util.vector().blockSurface(blockPos, Direction.NORTH).add(0, 0.5, 0), Pointing.DOWN, 25).withItem(copper).scroll().withCount(6);
        scene.idle(25);
        scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, e ->
                e.setItem(29, copper.copyWithCount(6)));
        scene.idle(40);

        scene.overlay().showText(40)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(blockPos, Direction.NORTH).add(-0.3, 0.26, 0))
                .text("The status bar indicates the state of each reserve");
        scene.idle(55);

        scene.world().setKineticSpeed(largeCog, -16);
        scene.world().setKineticSpeed(shaftCog, 32);
        scene.world().setKineticSpeed(smallCogs, -32);
        scene.world().setKineticSpeed(westBelt, -32);
        scene.world().setKineticSpeed(eastBelt, -32);

        scene.world().showSection(largeCog, Direction.NORTH);
        scene.world().showSection(smallCogs, Direction.DOWN);
        scene.world().showSection(shaftCog, Direction.DOWN);
        scene.world().showSection(eastBelt, Direction.WEST);
        scene.world().showSection(westBelt, Direction.EAST);

        scene.idle(20);
        for (int i = 0; i < 6; i++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(0, 4, 2), new Vec3(0, 0, 0), gold.copy());
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(0, 1, 2), Direction.WEST, gold.copy());
            if (i == 1) {
                scene.world().modifyBlock(blockPos, s -> ModBlocks.RESERVE_STORAGE_BOX.getDefaultState()
                        .setValue(ReserveStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
            }
            if (i > 0) {
                scene.world().flapFunnel(util.grid().at(1, 2, 2), false);
                scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
                int finalI = i;
                scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, t -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(finalI)));
            }
            scene.idle(7);
        }
        scene.idle(14);
        scene.world().flapFunnel(util.grid().at(1, 2, 2), false);
        scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
        scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, t -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(6)));
        scene.idle(40);

        scene.overlay().showText(40)
                .attachKeyFrame()
                .placeNearTarget()
                .text("Once a reserve is satisfied, excess items can be extracted by automation");
        scene.idle(45);

        for (int i = 0; i < 6; i++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(0, 4, 2), new Vec3(0, 0, 0), gold.copy());
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(0, 1, 2), Direction.WEST, gold.copy());
            if (i > 0) {
                scene.world().flapFunnel(util.grid().at(1, 2, 2), false);
                scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
                scene.world().createItemOnBelt(util.grid().at(3, 1, 2), Direction.WEST, gold.copy());
                scene.world().flapFunnel(util.grid().at(3, 1, 2), true);
            }
            scene.idle(7);
        }
        scene.idle(14);
        scene.world().flapFunnel(util.grid().at(1, 2, 2), false);
        scene.world().removeItemsFromBelt(util.grid().at(1, 1, 2));
        scene.world().createItemOnBelt(util.grid().at(3, 1, 2), Direction.WEST, gold.copy());
        scene.world().flapFunnel(util.grid().at(3, 1, 2), true);
        scene.idle(40);

        scene.overlay().showText(40)
                .placeNearTarget()
                .text("The player can always extract items below the minimum");
        scene.idle(45);

        scene.overlay().showControls(util.vector().blockSurface(blockPos, Direction.NORTH).add(0, -0.15, 0), Pointing.UP, 25).withItem(gold).rightClick();
        scene.idle(25);
        scene.world().modifyBlockEntity(blockPos, ReserveStorageBoxEntity.class, e ->
                e.setItem(0, gold.copyWithCount(5)));
        scene.idle(35);

        scene.markAsFinished();
    }

    public static void treeFarm(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reserve_storage_tree_farm", "Reserve Storage Boxes on Contraptions");
        scene.configureBasePlate(0, 1, 7);
        scene.world().setBlock(util.grid().at(2, 0, 3), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        scene.showBasePlate();
        scene.idle(5);

        Selection tree = util.select().column(2, 3)
                .add(util.select().fromTo(0, 4, 1, 4, 6, 5));
        Selection belt = util.select().fromTo(3, 0, 0, 5, 0, 0);
        BlockPos bearing = util.grid().at(4, 1, 5);
        BlockPos reserveBox = util.grid().at(1, 3, 6);
        BlockPos saw = util.grid().at(1, 1, 5);
        BlockPos deployer = util.grid().at(1, 3, 5);
        ItemStack sapling = new ItemStack(Items.OAK_SAPLING);
        ItemStack logs = new ItemStack(Items.OAK_LOG);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack apple = new ItemStack(Items.APPLE);

        ElementLink<WorldSectionElement> contraption = scene.world().showIndependentSection(
                util.select().fromTo(4, 2, 4, 1, 3, 7)
                        .add(util.select().fromTo(1, 1, 5, 1, 1, 6)),
                Direction.DOWN
        );
        scene.world().showSection(util.select().position(bearing), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(50)
                .attachKeyFrame()
                .text("Reserve Storage Boxes are useful for self-sustaining farms");
        scene.idle(65);

        scene.idle(20);
        scene.overlay().showText(50)
                .text("Saplings can be kept in reserve for replanting");
        scene.idle(65);

        CsInputWindowElement.showControls(
                scene, util.vector().blockSurface(reserveBox, Direction.WEST), Pointing.DOWN, 25
        ).withItem(sapling).scroll().withCount(2);
        scene.idle(25);
        scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, e ->
                e.setItem(27, sapling.copyWithCount(2)));
        scene.idle(35);

        scene.world().showSection(tree, Direction.UP);
        scene.idle(15);

        scene.world().setKineticSpeed(util.select().position(saw), 16);
        scene.world().configureCenterOfRotation(contraption, util.vector().centerOf(bearing));
        scene.world().rotateBearing(bearing, -20, 15);
        scene.world().rotateSection(contraption, 0, -20, 0, 15);

        for (int d = 0; d < 1; d++) {
            scene.world().moveDeployer(deployer, 1, 9);
            scene.idle(10);
            scene.world().moveDeployer(deployer, -1, 9);
            scene.idle(10);
        }

        for (int i = 0; i < 10; i++) {
            scene.idle(5);
            scene.world().incrementBlockBreakingProgress(util.grid().at(2, 1, 3));
        }

        scene.world().replaceBlocks(tree, Blocks.AIR.defaultBlockState(), true);
        scene.world().modifyBlock(reserveBox, b -> b.setValue(ReserveStorageBox.STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, e -> {
            e.setItem(0, logs.copyWithCount(10));
            e.setItem(1, sapling.copyWithCount(2));
        });
        scene.idle(5);

        scene.overlay().showText(60)
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.DOWN))
                .placeNearTarget()
                .colored(PonderPalette.GREEN)
                .text("Deployers will ignore reserve limits when placing items");
        scene.idle(65);

        scene.world().setKineticSpeed(belt, -32);
        scene.world().showSection(util.select().fromTo(3, 0, 0, 5, 2, 0), Direction.UP);
        scene.idle(10);

        scene.world().rotateBearing(bearing, -70, 40);
        scene.world().rotateSection(contraption, 0, -70, 0, 40);

        for (int d = 0; d < 2; d++) {
            scene.world().moveDeployer(deployer, 1, 9);
            if (d == 1) {
                scene.world().setBlocks(util.select().position(2, 1, 3), Blocks.OAK_SAPLING.defaultBlockState(), false);
                scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, b ->
                        b.setItem(1, sapling.copy())
                );
            }
            scene.idle(10);
            scene.world().moveDeployer(deployer, -1, 9);
            scene.idle(10);
        }

        Selection psi = util.select().position(1, 2, 6).add(util.select().position(3, 2, 0));
        scene.world().modifyBlockEntityNBT(psi, PortableItemInterfaceBlockEntity.class, nbt -> {
            nbt.putFloat("Distance", 1);
            nbt.putFloat("Timer", 12);
        });
        scene.idle(10);

        scene.overlay().showText(50)
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(3, 3, 2)))
                .placeNearTarget()
                .text("Configured items remain in storage until reserve is satisfied");
        scene.idle(50);

        scene.world().createItemOnBelt(util.grid().at(3, 0, 0), Direction.WEST, logs.copyWithCount(5));
        scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, e ->
                e.setItem(0, logs.copyWithCount(5))
        );
        scene.idle(15);
        scene.world().createItemOnBelt(util.grid().at(3, 0, 0), Direction.WEST, stick.copyWithCount(2));
        scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, e ->
                e.setItem(0, logs.copy())
        );
        scene.idle(15);
        scene.world().createItemOnBelt(util.grid().at(3, 0, 0), Direction.WEST, apple);
        scene.world().modifyBlockEntity(reserveBox, ReserveStorageBoxEntity.class, e ->
                e.setItem(0, ItemStack.EMPTY)
        );
        scene.idle(40);
        scene.markAsFinished();
    }
}
