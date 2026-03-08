package net.fxnt.fxntstorage.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSettings {
    // Map each player's UUID to their settings
    private static final Map<UUID, Map<String, Object>> PLAYER_SETTINGS = new ConcurrentHashMap<>();

    // Set or overwrite all settings for a player
    public static void set(UUID playerId, Map<String, Object> settings) {
        PLAYER_SETTINGS.put(playerId, settings);
    }

    // Get a value for a specific player
    public static Object get(UUID playerId, String key) {
        return PLAYER_SETTINGS.getOrDefault(playerId, Map.of()).get(key);
    }

    public static int getInt(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof Integer i ? i : 0;
    }

    public static double getDouble(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof Double d ? d : 0;
    }

    public static float getFloat(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof Float f ? f : 0;
    }

    public static boolean getBoolean(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof Boolean b && b;
    }

    public static String getString(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof String s ? s : "";
    }

    @SuppressWarnings("unchecked")
    public static List<String> getList(UUID playerId, String key) {
        Object val = get(playerId, key);
        return val instanceof List<?> list ? (List<String>) list : List.of();
    }

    public static void remove(UUID playerId) {
        PLAYER_SETTINGS.remove(playerId);
    }
}