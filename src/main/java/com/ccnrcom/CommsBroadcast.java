/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import com.ccnrcom.network.CommsChatPacket;
import com.ccnrcom.network.RadioReceivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 服务端广播 API：向频道/管理/场外发送消息（供服务端命令或其他插件调用）。
 */
public final class CommsBroadcast {
    public static final String SERVER_NAME = "Server";

    private CommsBroadcast() {}

    /** 向指定对讲频道广播（发送者为 服务器） */
    public static int radio(MinecraftServer server, String channel, String message) {
        if (server == null || channel == null || channel.isEmpty()) return 0;
        RadioReceivePacket packet = new RadioReceivePacket(channel, SERVER_NAME, message);
        int sent = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (ChannelManager.isOnChannel(p.getUUID(), channel)) {
                CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
                sent++;
            }
        }
        return sent;
    }

    /** 向管理频道广播（仅 OP/权限玩家可见） */
    public static int admin(MinecraftServer server, String message) {
        if (server == null) return 0;
        CommsChatPacket packet = new CommsChatPacket(CommsChatPacket.TYPE_ADMIN, SERVER_NAME, message);
        int sent = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Permissions.canAdmin(p)) {
                CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
                sent++;
            }
        }
        return sent;
    }

    /** 向场外频道广播（全员可见） */
    public static int ooc(MinecraftServer server, String message) {
        if (server == null) return 0;
        CommsChatPacket packet = new CommsChatPacket(CommsChatPacket.TYPE_OOC, SERVER_NAME, message);
        int sent = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
            sent++;
        }
        return sent;
    }
}
