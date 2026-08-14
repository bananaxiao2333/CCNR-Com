package com.ccnrcom.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/** 客户端缓存的"我频道上的成员"（由 ChannelStatePacket 更新，HUD 区分对讲机/近场） */
@OnlyIn(Dist.CLIENT)
public class ClientChannelState {
    private static volatile Set<UUID> members = Collections.emptySet();

    public static void setMembers(Set<UUID> newMembers) {
        members = newMembers;
    }

    public static boolean isOnMyChannel(UUID playerId) {
        return members.contains(playerId);
    }
}
