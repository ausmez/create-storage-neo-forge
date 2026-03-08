package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncClientSettingsPacket(CompoundTag settings) implements CustomPacketPayload {
    public static final Type<SyncClientSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_client_settings"));

    public static final StreamCodec<FriendlyByteBuf, SyncClientSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncClientSettingsPacket::settings,
            SyncClientSettingsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Map<String, Object> map = new HashMap<>();
                CompoundTag tag = settings();

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
            }
        });
    }
}
