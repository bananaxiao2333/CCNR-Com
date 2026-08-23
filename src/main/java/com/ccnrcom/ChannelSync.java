/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import com.ccnrcom.network.ChannelStatePacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** 把每个在线玩家的"频道成员列表"同步给对应客户端（HUD 区分对讲机/近场语音用） */
public class ChannelSync {

    /** 频道状态变化或有人上下线时，全量同步 */
    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            syncTo(server, p);
        }
    }

    /** 同步单个玩家的频道成员列表 */
    public static void syncTo(MinecraftServer server, ServerPlayer player) {
        String channel = ChannelManager.get(player.getUUID()).orElse(null);
        List<UUID> members = new ArrayList<>();
        if (channel != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (ChannelManager.isOnChannel(p.getUUID(), channel)) {
                    members.add(p.getUUID());
                }
            }
        }
        CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ChannelStatePacket(members));
    }
}
