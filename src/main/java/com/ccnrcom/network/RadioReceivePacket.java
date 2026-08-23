/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.network;

import com.ccnrcom.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** S2C：服务器把一条对讲机消息推送给同频道的客户端 */
public class RadioReceivePacket {
    private final String channel;
    private final String senderName;
    private final String message;

    public RadioReceivePacket(String channel, String senderName, String message) {
        this.channel = channel;
        this.senderName = senderName;
        this.message = message;
    }

    public static void encode(RadioReceivePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.channel);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.message);
    }

    public static RadioReceivePacket decode(FriendlyByteBuf buf) {
        return new RadioReceivePacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(RadioReceivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get()
                .enqueueWork(
                        () -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handle(msg)));
        ctx.get().setPacketHandled(true);
    }

    public String getChannel() {
        return channel;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }
}
