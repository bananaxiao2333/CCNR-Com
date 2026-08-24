/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.niuqu.chatbubble.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server: request to download a server-hosted media file. */
public class MediaRequestPacket {
    private final String mediaId;

    public MediaRequestPacket(String mediaId) {
        this.mediaId = mediaId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(mediaId);
    }

    public static MediaRequestPacket decode(FriendlyByteBuf buf) {
        return new MediaRequestPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender != null) com.niuqu.chatbubble.server.MediaService.handleRequest(sender, mediaId);
        });
        ctx.get().setPacketHandled(true);
    }

    public String mediaId() {
        return mediaId;
    }
}
