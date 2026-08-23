/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 客户端缓存的频道状态（我的频道 + 成员 UUID→名字，由 ChannelStatePacket 更新） */
@OnlyIn(Dist.CLIENT)
public class ClientChannelState {
    private static volatile String myChannel = "";
    private static volatile Map<UUID, String> members = Collections.emptyMap();

    public static void setState(String channel, Map<UUID, String> newMembers) {
        myChannel = channel == null ? "" : channel;
        members = newMembers;
    }

    public static String getMyChannel() {
        return myChannel;
    }

    public static boolean isOnMyChannel(UUID playerId) {
        return members.containsKey(playerId);
    }

    public static String nameOf(UUID playerId) {
        return members.getOrDefault(playerId, "");
    }

    public static Map<UUID, String> all() {
        return members;
    }
}
