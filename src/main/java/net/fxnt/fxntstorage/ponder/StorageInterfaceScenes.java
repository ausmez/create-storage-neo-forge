package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;
import static net.fxnt.fxntstorage.simple_storage.SimpleStorageBox.STORAGE_USED;

public class StorageInterfaceScenes {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("storage_interface_intro", "Storage Interface");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos iface = util.grid().at(2, 2, 2);
        BlockPos controller = util.grid().at(2, 2, 4);
        BlockPos largeCog = util.grid().at(5, 0, 0);
        BlockPos leftBox = util.grid().at(4, 2, 2);
        BlockPos rightBox = util.grid().at(0, 2, 2);
        BlockPos gearbox = util.grid().at(3, 2, 1);
        BlockPos funnel = util.grid().at(2, 2, 1);
        Selection focus = util.select().fromTo(0, 1, 2, 4, 2, 2);
        Selection trim = util.select().fromTo(2, 1, 3, 2, 1, 4);
        Selection belt = util.select().fromTo(2, 1, 0, 2, 2, 1);
        Selection smallCog = util.select().fromTo(5, 1, 1, 3, 1, 1);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack emerald = new ItemStack(Items.EMERALD);

        scene.world().showSection(focus, Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(65).text("Storage Interfaces only function within existing storage networks").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(iface, Direction.NORTH));
        scene.idle(25);
        scene.overlay().showOutline(PonderPalette.RED, "iface", util.select().position(iface), 35);
        scene.idle(55);
        scene.world().showSection(trim, Direction.NORTH);
        scene.world().showSection(util.select().position(controller), Direction.DOWN);
        scene.idle(20);
        scene.world().modifyBlock(controller, (s) -> ModBlocks.STORAGE_CONTROLLER.get().defaultBlockState().setValue(CONNECTED, true), false);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.GREEN, "iface", util.select().position(iface), 35);
        scene.idle(45);

        scene.overlay().showText(65).text("They cannot be directly interacted with by the player").placeNearTarget().pointAt(util.vector().blockSurface(iface, Direction.NORTH));
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(iface, Direction.NORTH), Pointing.RIGHT, 25).showing(AllIcons.I_MTD_CLOSE).withItem(emerald);
        scene.idle(75);

        scene.world().showSection(belt, Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(smallCog, Direction.WEST);
        scene.world().showSection(util.select().position(largeCog), Direction.UP);
        scene.world().setKineticSpeed(util.select().everywhere(), 32.0F);
        scene.world().setKineticSpeed(util.select().position(largeCog), -16.0F);
        scene.idle(25);

        scene.overlay().showText(55).text("Items can be inserted into the network with funnels, chutes, hoppers or passers").attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(iface, Direction.NORTH));
        for (int i = 0; i < 4; i++) {
            ElementLink<EntityElement> itemEntity = scene.world().createItemEntity(util.vector().centerOf(2, 4, 0).add(0, 0, -0.35), new Vec3(0, 0, 0), emerald);
            scene.idle(13);
            scene.world().modifyEntity(itemEntity, Entity::discard);
            scene.world().createItemOnBelt(util.grid().at(2, 1, 0), Direction.NORTH, emerald);
            if (i == 1) {
                scene.world().modifyBlock(rightBox, (s) -> ModBlocks.SIMPLE_STORAGE_BOX.get().defaultBlockState().setValue(STORAGE_USED, EnumProperties.StorageUsed.HAS_ITEMS), false);
                scene.world().modifyBlockEntity(rightBox, SimpleStorageBoxEntity.class, (t) -> t.setFilter(emerald));
            }
            if (i > 0) {
                scene.idle(4);
                scene.world().removeItemsFromBelt(funnel.below());
                scene.world().flapFunnel(funnel, false);
                int finalJ = i;
                scene.world().modifyBlockEntity(rightBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, emerald.copyWithCount(finalJ)));
            }
            scene.idle(2);

        }
        scene.idle(17);
        scene.world().modifyBlockEntity(rightBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, emerald.copyWithCount(4)));
        scene.world().removeItemsFromBelt(funnel.below());
        scene.world().flapFunnel(funnel, false);

        scene.idle(75);

        scene.world().hideSection(util.select().position(3, 1, 1), Direction.UP);
        scene.world().setKineticSpeed(belt, 0F);
        scene.idle(20);
        scene.world().moveSection(scene.world().showIndependentSection(util.select().position(gearbox), Direction.DOWN), util.vector().of(0, -1, 0), 0);
        scene.world().setKineticSpeed(belt, -32.0F);
        scene.idle(5);

        for (int i = 0; i < 4; i++) {
            scene.world().createItemOnBelt(funnel.below(), Direction.SOUTH, diamond);
            scene.world().modifyBlockEntity(leftBox, SimpleStorageBoxEntity.class, (t) -> t.setItem(0, diamond.copyWithCount(t.getItem(0).getCount() - 1)));
            scene.idle(19);
        }

        scene.markAsFinished();
    }

}
