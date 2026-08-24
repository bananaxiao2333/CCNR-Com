/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.niuqu.chatbubble.packets;

import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -> client sync of the top notification-banner switches (id 12).
 * Server config takes priority: once received, the client uses these values
 * instead of its own e33chat-client.toml banner_* keys until disconnected.
 * Registered as its own id so older clients (which never decode this id) drop
 * the packet harmlessly instead of desyncing the wire format.
 */
public class BannerConfigSyncPacket {
    private final boolean mentionEnabled;
    private final boolean whisperEnabled;
    private final boolean systemEnabled;

    public BannerConfigSyncPacket(boolean mentionEnabled, boolean whisperEnabled, boolean systemEnabled) {
        this.mentionEnabled = mentionEnabled;
        this.whisperEnabled = whisperEnabled;
        this.systemEnabled = systemEnabled;
    }

    public static void encode(BannerConfigSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.mentionEnabled);
        buf.writeBoolean(packet.whisperEnabled);
        buf.writeBoolean(packet.systemEnabled);
    }

    public static BannerConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new BannerConfigSyncPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get()
                .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                ChatMessageStore.setServerBannerConfig(mentionEnabled, whisperEnabled, systemEnabled)));
        ctx.get().setPacketHandled(true);
    }
}
