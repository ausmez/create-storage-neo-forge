package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.List;

import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBox.*;
import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.CAPACITY_UPGRADE_SLOT_START;
import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity.SLOT_COUNT;

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
            state1 = ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state2 = ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state3 = ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
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
            state1 = ModBlocks.SIMPLE_STORAGE_BOX_BIRCH.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state2 = ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
            state3 = ModBlocks.SIMPLE_STORAGE_BOX_WARPED.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL);
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
        scene.overlay().showText(60).text("Each box can hold up to 32x the max stack size of one item").attachKeyFrame();
        scene.idle(70);

        ItemStack sand = new ItemStack(Items.SAND);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        BlockPos b1 = util.grid().at(2, 1, 1);
        BlockPos b2 = util.grid().at(3, 2, 2);
        BlockPos b3 = util.grid().at(1, 3, 3);

        scene.world().modifyBlockEntity(b1, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(sand);
            t.getItemHandler().setStackInSlot(0, sand.copyWithCount(2048));
        });
        scene.world().modifyBlock(b1, s -> state1, false);
        scene.overlay().showText(60).text("Sand Block = 2048 (64 per stack)").placeNearTarget().pointAt(util.vector().blockSurface(b1, Direction.NORTH).add(-0.2, 0.25, 0));
        scene.idle(65);

        scene.world().modifyBlockEntity(b2, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(pearl);
            t.getItemHandler().setStackInSlot(0, pearl.copyWithCount(512));
        });
        scene.world().modifyBlock(b2, s -> state2, false);
        scene.overlay().showText(60).text("Ender Pearl = 512 (16 per stack)").placeNearTarget().pointAt(util.vector().blockSurface(b2, Direction.NORTH).add(-0.2, 0.25, 0));
        scene.idle(65);

        scene.world().modifyBlockEntity(b3, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(water);
            t.getItemHandler().setStackInSlot(0, water.copyWithCount(32));
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

        scene.overlay().showText(50)
                .text("The front panel displays the item count and percentage used")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        scene.idle(60);

        scene.overlay().showText(80)
                .text("Items can be inserted directly by right-clicking the front panel...")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(gold);
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> {
            t.setFilter(gold);
            t.getItemHandler().setStackInSlot(0, gold.copyWithCount(64));
        });
        scene.world().modifyBlock(sSBox, s -> s.setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
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
                scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(finalJ)));
            }
            scene.idle(2);
        }
        scene.idle(17);
        scene.world().removeItemsFromBelt(util.grid().at(3, 1, 2));
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() + 1)));
        scene.idle(50);

        scene.overlay().showText(50).text("The filter is set automatically when the first item is added").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(60);

        scene.overlay().showText(60).text("Use the wrench to remove the filter (only when the box is empty)").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.25, 0.25, 0));
        scene.idle(70);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 50).leftClick();
        scene.overlay().showText(50).independent().text("Left-click front panel to extract a single item").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 2, 1), new Vec3(0, 0, 0), gold);
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() - 1)));
        scene.idle(60);

        scene.world().modifyEntity(itemEntity, Entity::discard);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 50).leftClick().whileSneaking();
        scene.overlay().showText(50).text("Left-click while sneaking to remove a whole item stack").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 2, 1), new Vec3(0, 0, 0), gold.copyWithCount(64));
        scene.world().modifyBlockEntity(sSBox, SimpleStorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, gold.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() - 64)));
        scene.idle(60);

        scene.world().modifyEntity(itemEntity, Entity::discard);

        scene.overlay().showControls(util.vector().blockSurface(sSBox, Direction.NORTH), Pointing.RIGHT, 60).rightClick().whileSneaking();
        scene.overlay().showText(60).text("Right-click with an empty hand while sneaking to open the inventory menu").placeNearTarget().pointAt(util.vector().blockSurface(sSBox, Direction.NORTH).add(-0.15, -0.05, 0));
        scene.idle(70);

        scene.markAsFinished();
    }

    public static void upgrades(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_storage_upgrades", "Void & Capacity Upgrades");
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

        scene.overlay().showText(70)
                .text("Void and Capacity Upgrades are available for Simple Storage Boxes").
                attachKeyFrame()
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("Upgrades are applied by using the item on the front panel...")
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(105)
                .text("...or adding via the interface accessed by right-clicking while sneaking")
                .placeNearTarget();
        scene.idle(115);

        scene.overlay().showControls(util.vector().blockSurface(leftBox, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(vUpgrade);
        scene.overlay().showText(65)
                .text("Void Upgrade will void (delete) any item added beyond the max capacity")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().blockSurface(leftBox, Direction.WEST));
        scene.idle(40);
        scene.world().modifyBlock(leftBox, s -> s.setValue(VOID_UPGRADE, true), false);
        scene.world().modifyBlockEntity(leftBox, SimpleStorageBoxEntity.class, (t) -> {
            t.getItemHandler().setStackInSlot(3, vUpgrade);
            t.voidUpgrade = true;
            t.setFilter(iron);
            t.getItemHandler().setStackInSlot(0, iron.copyWithCount(2048));
        });
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(rightBox, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(cUpgrade);
        scene.overlay().showText(65)
                .text("Capacity Upgrade will double the storage for each upgrade")
                .placeNearTarget().pointAt(util.vector().blockSurface(rightBox, Direction.WEST));
        scene.idle(40);
        scene.world().modifyBlock(rightBox, s -> s.setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL), false);
        scene.world().modifyBlockEntity(rightBox, SimpleStorageBoxEntity.class, (t) -> {
            for (int i = CAPACITY_UPGRADE_SLOT_START; i < SLOT_COUNT; i++) {
                t.getItemHandler().setStackInSlot(i, cUpgrade);
            }
            t.setFilter(gold);
            t.getItemHandler().setStackInSlot(0, gold.copyWithCount(1048576));
        });
        scene.idle(50);

        scene.overlay().showText(70).text("A total of 9 Capacity Upgrades and 1 Void Upgrade can be added").placeNearTarget();

        scene.markAsFinished();
    }

    public static void compacting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_storage_compacting", "Compacting Upgrade");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos box = util.grid().at(2, 2, 2);
        ItemStack nugget = new ItemStack(Items.IRON_NUGGET);
        ItemStack ingot = new ItemStack(Items.IRON_INGOT);
        ItemStack block = new ItemStack(Blocks.IRON_BLOCK);
        ItemStack upgrade = ModItems.STORAGE_BOX_COMPACTING_UPGRADE.asStack();

        scene.world().modifyBlock(box, s -> s.setValue(STORAGE_USED, EnumProperties.StorageUsed.FULL), false);
        scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, t -> {
            t.setFilter(nugget);
            t.getItemHandler().setStackInSlot(0, nugget.copyWithCount(2048));
        });

        scene.world().showSection(util.select().column(2, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("Compacting Upgrade converts stored items into crafting tiers")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("Upgrade is applied by using the item on the front panel...")
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(box, Direction.NORTH), Pointing.RIGHT, 10).rightClick().withItem(upgrade);
        scene.idle(10);
        scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, t -> {
            t.getItemHandler().setStackInSlot(3, upgrade);
            t.compactingUpgrade = true;
            t.onCompactingUpgradeInstalled();
        });

        scene.overlay().showText(70)
                .text("...or adding via the interface accessed by right-clicking while sneaking")
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .text("When a valid compacting chain exists, items are available at every tier")
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(50)
                .text("Hold CTRL + scroll while looking at the front display...")
                .pointAt(util.vector().blockSurface(box, Direction.NORTH).add(-0.25, 0, 0))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(40);
        scene.overlay().showControls(util.vector().blockSurface(box, Direction.NORTH), Pointing.RIGHT, 15).scroll().whileCTRL();
        scene.idle(30);
        scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, b -> b.compactingSelectedTier = 1);
        scene.overlay().showText(65)
                .text("...to choose which compacting tier is displayed")
                .pointAt(util.vector().blockSurface(box, Direction.NORTH).add(-0.5, 0, 0))
                .placeNearTarget();
        scene.idle(40);
        scene.overlay().showControls(util.vector().blockSurface(box, Direction.NORTH), Pointing.RIGHT, 15).scroll().whileCTRL();
        scene.idle(30);
        scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, b -> b.compactingSelectedTier = 2);
        scene.idle(60);

        scene.overlay().showText(65)
                .text("The item displayed is the item available to players for extraction")
                .placeNearTarget();
        scene.idle(40);

        scene.overlay().showControls(util.vector().blockSurface(box, Direction.NORTH), Pointing.RIGHT, 20).leftClick();
        scene.world().modifyBlock(box, s -> s.setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 2, 1), new Vec3(0, 0, 0), nugget.copyWithCount(64));
        scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, t ->
                t.getItemHandler().setStackInSlot(0, nugget.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() - 64))
        );
        scene.idle(60);

        scene.overlay().showText(60)
                .text("The Compacting Wheel keybind can also be used to quickly select a tier")
                .placeNearTarget();
        scene.idle(70);

        scene.world().modifyEntity(itemEntity, Entity::discard);

        Selection belt = util.select().fromTo(1, 1, 2, 0, 1, 2);
        Selection shaft = util.select().fromTo(0, 1, 3, 0, 1, 5);
        BlockPos funnel = util.grid().at(1, 2, 2);
        BlockPos largeCog = util.grid().at(1, 0, 5);

        scene.world().showSection(belt, Direction.EAST);
        scene.world().showSection(util.select().position(funnel), Direction.EAST);
        scene.world().showSection(shaft, Direction.NORTH);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);

        scene.idle(10);

        scene.overlay().showText(60)
                .text("Automated extraction will always prefer the highest available tier")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.world().setKineticSpeed(util.select().position(largeCog), -16);
        scene.world().setKineticSpeed(shaft, 32);
        scene.world().setKineticSpeed(belt, 32);

        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(util.grid().at(1, 1, 2), Direction.EAST, block);
            scene.world().flapFunnel(funnel, true);
            scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, e -> {
                ItemStack slot0 = e.getItemHandler().getStackInSlot(0);
                e.getItemHandler().setStackInSlot(0, slot0.copyWithCount(Math.max(0, slot0.getCount() - 81)));
            });
            scene.idle(16);
        }
        scene.world().hideSection(util.select().position(funnel), Direction.UP);
        scene.idle(40);

        scene.world().setBlock(funnel, AllBlocks.BRASS_BELT_FUNNEL.getDefaultState()
                .setValue(FACING, Direction.WEST), false);

        scene.world().showSection(util.select().position(funnel), Direction.DOWN);
        scene.idle(30);

        scene.overlay().showText(60)
                .text("Use filters when automation requires a specific tier for extraction")
                .pointAt(util.vector().blockSurface(funnel, Direction.WEST).add(0.6, 0.3, 0.3))
                .attachKeyFrame()
                .placeNearTarget();
        scene.overlay().showFilterSlotInput(util.vector().blockSurface(funnel, Direction.WEST).add(0.6, 0.3, 0), 30);
        scene.idle(40);
        scene.world().setFilterData(util.select().position(funnel), FunnelBlockEntity.class, ingot);
        scene.idle(40);

        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(util.grid().at(1, 1, 2), Direction.EAST, ingot.copyWithCount(64));
            scene.world().flapFunnel(funnel, true);
            scene.world().modifyBlockEntity(box, SimpleStorageBoxEntity.class, e -> {
                ItemStack slot0 = e.getItemHandler().getStackInSlot(0);
                e.getItemHandler().setStackInSlot(0, slot0.copyWithCount(Math.max(0, slot0.getCount() - 384)));
            });
            scene.idle(16);
        }
        scene.idle(40);

        scene.markAsFinished();
    }
}
