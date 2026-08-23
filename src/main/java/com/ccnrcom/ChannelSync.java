/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import com.ccnrcom.network.ChannelStatePacket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** 把每个在线玩家的"频道成员列表"同步给对应客户端（HUD 侧边栏/对讲区分用） */
public class ChannelSync {

    /** 频道状态变化或有人上下线时，全量同步 */
    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            syncTo(server, p);
        }
    }

    /** 同步单个玩家的频道成员列表（UUID + 名字） */
    public static void syncTo(MinecraftServer server, ServerPlayer player) {
        String channel = ChannelManager.get(player.getUUID()).orElse(null);
        Map<UUID, String> members = new LinkedHashMap<>();
        if (channel != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (ChannelManager.isOnChannel(p.getUUID(), channel)) {
                    members.put(p.getUUID(), p.getGameProfile().getName());
                }
            }
        }
        CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ChannelStatePacket(channel, members));
    }
}
