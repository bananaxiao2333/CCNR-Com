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

/** S2C：开麦/关麦时向玩家发送 ActionBar 反馈（key 为语言键，arg 为频道，pitch>0 播放提示音） */
public class VoiceFeedbackPacket {
    private final String key;
    private final String arg;
    private final float pitch;

    public VoiceFeedbackPacket(String key, String arg, float pitch) {
        this.key = key;
        this.arg = arg;
        this.pitch = pitch;
    }

    public static void encode(VoiceFeedbackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.key);
        buf.writeUtf(msg.arg);
        buf.writeFloat(msg.pitch);
    }

    public static VoiceFeedbackPacket decode(FriendlyByteBuf buf) {
        return new VoiceFeedbackPacket(buf.readUtf(), buf.readUtf(), buf.readFloat());
    }

    public static void handle(VoiceFeedbackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get()
                .enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT, () -> () -> ClientPacketHandler.handleVoiceFeedback(msg)));
        ctx.get().setPacketHandled(true);
    }

    public String getKey() {
        return key;
    }

    public String getArg() {
        return arg;
    }

    public float getPitch() {
        return pitch;
    }
}
