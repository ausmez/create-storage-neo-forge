package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.passer.PasserSmartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.container.StorageBox.STORAGE_USED;

public class SmartPasserBlockScenes {
    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("smart_passer_intro", "Smart Passer Block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        Selection horzBox = util.select().fromTo(3, 1, 3, 1, 2, 3);

        ElementLink<WorldSectionElement> horzLink = scene.world().showIndependentSection(horzBox, Direction.DOWN);
        scene.world().moveSection(horzLink, util.vector().of(0, 0, -1), 0);
        scene.idle(10);

        BlockPos passer = util.grid().at(2, 2, 2);
        BlockPos srcBox = util.grid().at(3, 2, 3);
        BlockPos dstBox = util.grid().at(1, 2, 3);
        Vec3 filter = util.vector().blockSurface(passer, Direction.NORTH).add(new Vec3(-0.2, 0, 0));
        ItemStack sand = new ItemStack(Items.SAND);
        ItemStack gunpowder = new ItemStack(Items.GUNPOWDER);

        scene.overlay().showText(65).text("Smart Passer blocks pass items from one container to another").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
        scene.idle(90);

        scene.overlay().showText(40).text("They can be oriented horizontally...").placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
        scene.idle(55);

        scene.world().showSection(util.select().layersFrom(3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(40).text("...or vertically, using a wrench").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(1, 3, 2), Direction.WEST));
        scene.idle(55);
        scene.world().hideSection(util.select().layersFrom(3), Direction.UP);
        scene.idle(35);

        scene.world().modifyBlockEntity(srcBox, StorageBoxEntity.class, (t) -> {
            for (int i = 0; i < 10; i++) {
                t.getItemHandler().setStackInSlot(i, sand.copyWithCount(64));
            }
        });

        scene.addKeyframe();
        scene.overlay().showControls(util.vector().blockSurface(srcBox.north(), Direction.NORTH), Pointing.RIGHT, 20).withItem(sand);
        scene.world().modifyBlock(srcBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(30);

        scene.world().modifyBlockEntity(dstBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, sand.copyWithCount(64)));
        scene.world().modifyBlockEntity(srcBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, ItemStack.EMPTY));
        scene.world().modifyBlock(dstBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(20);

        scene.overlay().showFilterSlotInput(filter.add(0.35, 0.15, 0), Direction.NORTH, 50);
        scene.overlay().showText(60).text("Use the value panel to specify the extracted stack size").placeNearTarget().pointAt(filter.add(0.25, 0.15, 0));
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            scene.world().modifyBlockEntity(dstBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(1 + finalI, sand.copyWithCount(64)));
            scene.world().modifyBlockEntity(srcBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(1 + finalI, ItemStack.EMPTY));
            if (i < 2) scene.idle(20);
            else scene.idle(10);
        }
        scene.idle(35);

        scene.addKeyframe();
        scene.overlay().showControls(filter.add(0.35, 0.15, 0), Pointing.DOWN, 20).rightClick().withItem(gunpowder);
        scene.idle(7);
        scene.world().setFilterData(util.select().position(util.grid().at(2, 2, 3)), PasserSmartEntity.class, gunpowder);
        scene.idle(25);
        scene.overlay().showText(60).text("Items in the filter slot specify what to transfer").placeNearTarget().pointAt(filter.add(0.25, 0.15, 0));
        scene.idle(80);

        scene.world().setFilterData(util.select().position(util.grid().at(2, 2, 3)), PasserSmartEntity.class, new ItemStack(Items.AIR));
        scene.idle(15);
        scene.world().moveSection(horzLink, util.vector().of(0, 0, 1), 8);
        scene.world().showSection(util.select().fromTo(3, 1, 1, 2, 1, 2), Direction.SOUTH);
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            scene.world().modifyBlockEntity(dstBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(4 + finalI, sand.copyWithCount(64)));
            scene.world().modifyBlockEntity(srcBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(4 + finalI, ItemStack.EMPTY));
            if (i == 0) scene.idle(20);
            else scene.idle(10);
        }

        BlockPos lever = util.grid().at(3, 1, 1);
        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 1, 2, 1, 2).add(util.select().position(2, 2, 3)));
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(70).text("Redstone power will prevent Smart Passers transferring items").attachKeyFrame().colored(PonderPalette.RED).pointAt(util.vector().blockSurface(util.grid().at(2, 2, 3), Direction.UP).add(-0.25, 0, -0.1)).placeNearTarget();
        scene.idle(100);
        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 1, 2, 1, 2).add(util.select().position(2, 2, 3)));
        scene.idle(10);

        for (int i = 0; i < 4; i++) {
            int finalI = i;
            scene.world().modifyBlockEntity(dstBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(6 + finalI, sand.copyWithCount(64)));
            scene.world().modifyBlockEntity(srcBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(6 + finalI, ItemStack.EMPTY));
            if (i == 3)
                scene.world().modifyBlock(srcBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY), false);
            scene.idle(20);
        }

        scene.markAsFinished();
    }
}
