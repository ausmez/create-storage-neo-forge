package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncClientSettingsPacket(CompoundTag settings) {

    public static void encode(SyncClientSettingsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.settings);
    }

    public static SyncClientSettingsPacket decode(FriendlyByteBuf buffer) {
        CompoundTag settings = buffer.readNbt();
        return new SyncClientSettingsPacket(settings);
    }

    public static void handle(SyncClientSettingsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            Map<String, Object> map = new HashMap<>();
            CompoundTag tag = packet.settings();

            for (String key : tag.getAllKeys()) {
                Tag t = tag.get(key);
                if (t instanceof IntTag i) map.put(key, i.getAsInt());
                else if (t instanceof DoubleTag d) map.put(key, d.getAsDouble());
                else if (t instanceof FloatTag f) map.put(key, f.getAsFloat());
                else if (t instanceof ByteTag b) map.put(key, b.getAsByte() != 0);
                else if (t instanceof StringTag s) map.put(key, s.getAsString());
                else if (t instanceof ListTag list) {
                    List<String> l = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) l.add(list.getString(i));
                    map.put(key, l);
                }
            }

            ClientSettings.set(player.getUUID(), map);
        });
        context.get().setPacketHandled(true);
    }
}
