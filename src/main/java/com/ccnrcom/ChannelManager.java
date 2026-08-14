package com.ccnrcom;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 服务端玩家频道管理（UUID -> 频道）。
 * 频道格式可通过配置 channelPattern 调整（默认 数字.数字，例如 123.4、462.5625）。
 */
public class ChannelManager {
    private static final Map<UUID, String> CHANNELS = new ConcurrentHashMap<>();

    public static boolean isValid(String channel) {
        if (channel == null) return false;
        try {
            return Pattern.compile(Config.CHANNEL_PATTERN.get()).matcher(channel).matches();
        } catch (Exception e) {
            return false;
        }
    }

    public static void set(UUID playerId, String channel) {
        CHANNELS.put(playerId, channel);
    }

    public static Optional<String> get(UUID playerId) {
        return Optional.ofNullable(CHANNELS.get(playerId));
    }

    public static void clear(UUID playerId) {
        CHANNELS.remove(playerId);
    }

    public static boolean isOnChannel(UUID playerId, String channel) {
        return channel.equals(CHANNELS.get(playerId));
    }

    public static Set<UUID> playersOnChannel(String channel) {
        return CHANNELS.entrySet().stream()
                .filter(e -> channel.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static void reset() {
        CHANNELS.clear();
    }
}