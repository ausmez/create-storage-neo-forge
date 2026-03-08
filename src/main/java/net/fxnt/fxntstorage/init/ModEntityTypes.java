package net.fxnt.fxntstorage.init;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.magnet.MagnetPickupEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {
    private static final CreateRegistrate REGISTRATE = FXNTStorage.REGISTRATE;

    public static final EntityEntry<MagnetPickupEntity> MAGNET_PICKUP_ENTITY = REGISTRATE
            .entity("magnet_pickup_entity", MagnetPickupEntity::new, MobCategory.MISC)
            .properties(b -> b
                    .sized(0, 0)
                    .clientTrackingRange(10)
                    .updateInterval(20))
            .register();

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MAGNET_PICKUP_ENTITY.get(), ArmorStand.createLivingAttributes().build());
    }
}
