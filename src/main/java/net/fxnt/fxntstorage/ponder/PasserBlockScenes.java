package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.fxnt.fxntstorage.containers.StorageBoxEntity;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static net.fxnt.fxntstorage.containers.StorageBox.STORAGE_USED;

public class PasserBlockScenes {
    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("passer_intro", "Storage Boxes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layers(1, 2), Direction.DOWN);
        scene.idle(10);

        BlockPos passer = util.grid().at(2, 2, 2);
        BlockPos srcBox1 = util.grid().at(3, 2, 2);
        BlockPos dstBox = util.grid().at(1, 2, 2);
        ItemStack sand = new ItemStack(Items.SAND);

        scene.overlay().showText(65).text("Passer blocks pass items from one container to another").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
        scene.idle(90);

        scene.overlay().showText(40).text("They can be oriented horizontally...").placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
        scene.idle(55);

        scene.world().showSection(util.select().layersFrom(3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(40).text("...or vertically, using a wrench").placeNearTarget().pointAt(util.vector().blockSurface(util.grid().at(1, 3, 2), Direction.WEST));
        scene.idle(55);
        scene.world().hideSection(util.select().layersFrom(3), Direction.UP);
        scene.idle(35);

        scene.addKeyframe();
        scene.overlay().showControls(util.vector().blockSurface(srcBox1, Direction.NORTH), Pointing.RIGHT, 20).withItem(sand);
        scene.world().modifyBlockEntity(srcBox1, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, sand.copyWithCount(12)));
        scene.world().modifyBlock(srcBox1, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
        scene.idle(40);

        scene.overlay().showText(60).text("They will only transfer one item at a time (like a hopper)").placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
        for (int i = 0; i < 12; i++) {
            if (i == 0)
                scene.world().modifyBlock(dstBox, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
            scene.world().modifyBlockEntity(srcBox1, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, sand.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() - 1)));
            scene.world().modifyBlockEntity(dstBox, StorageBoxEntity.class, (t) -> t.getItemHandler().setStackInSlot(0, sand.copyWithCount(t.getItemHandler().getStackInSlot(0).getCount() + 1)));
            if (i == 7)
                scene.overlay().showText(75).text("Unlike hoppers, Passers lack intermediate storage or inventory").placeNearTarget().pointAt(util.vector().blockSurface(passer, Direction.UP).add(0, -0.15, 0));
            if (i == 11)
                scene.world().modifyBlock(srcBox1, (s) -> ModBlocks.STORAGE_BOX.getDefaultState().setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY), false);
            scene.idle(15);
        }

        scene.markAsFinished();
    }
}
