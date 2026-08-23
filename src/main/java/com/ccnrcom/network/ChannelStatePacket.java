/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.network;

import com.ccnrcom.client.ClientPacketHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** S2C：我的频道 + 频道成员列表（UUID + 名字） */
public class ChannelStatePacket {
    private final String myChannel;
    private final Map<UUID, String> members;

    public ChannelStatePacket(String myChannel, Map<UUID, String> members) {
        this.myChannel = myChannel == null ? "" : myChannel;
        this.members = members;
    }

    public static void encode(ChannelStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.myChannel);
        buf.writeVarInt(msg.members.size());
        for (Map.Entry<UUID, String> e : msg.members.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    public static ChannelStatePacket decode(FriendlyByteBuf buf) {
        String myChannel = buf.readUtf();
        int size = buf.readVarInt();
        Map<UUID, String> members = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            members.put(buf.readUUID(), buf.readUtf());
        }
        return new ChannelStatePacket(myChannel, members);
    }

    public static void handle(ChannelStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get()
                .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT, () -> () -> ClientPacketHandler.handleChannelState(msg)));
        ctx.get().setPacketHandled(true);
    }

    public String getMyChannel() {
        return myChannel;
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public List<UUID> getMemberIds() {
        return new ArrayList<>(members.keySet());
    }
}
