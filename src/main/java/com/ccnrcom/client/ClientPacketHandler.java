/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import com.ccnrcom.Config;
import com.ccnrcom.chat.CommsMessage;
import com.ccnrcom.gui.store.CommsMessageStore;
import com.ccnrcom.network.ChannelStatePacket;
import com.ccnrcom.network.CommsChatPacket;
import com.ccnrcom.network.RadioReceivePacket;
import com.ccnrcom.network.VoiceFeedbackPacket;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.store.ChatMessageStore.SenderMeta;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 仅客户端：处理服务端推送的各类消息 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    public static void handle(RadioReceivePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component text = Component.translatable("ccnrcom.radio.prefix", msg.getChannel())
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(msg.getSenderName() + ": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(msg.getMessage()).withStyle(ChatFormatting.AQUA));
        mc.player.displayClientMessage(text, false);
        CommsMessageStore.get()
                .add(CommsMessage.radio(
                        msg.getChannel(), resolveSenderId(msg.getSenderName()), msg.getSenderName(), msg.getMessage()));
        if (Config.BEEP_ON_RECEIVE.get()) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5F, 1.8F);
        }
    }

    /** 开麦/关麦 ActionBar 反馈 + 提示音 */
    public static void handleVoiceFeedback(VoiceFeedbackPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component text = msg.getArg().isEmpty()
                ? Component.translatable(msg.getKey())
                : Component.translatable(msg.getKey(), msg.getArg());
        mc.player.displayClientMessage(text.copy().withStyle(ChatFormatting.YELLOW), true);
        if (msg.getPitch() > 0F) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.6F, msg.getPitch());
        }
    }

    /** 管理通讯 / 场外通讯消息 */
    public static void handleCommsChat(CommsChatPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component text;
        if (msg.getType() == CommsChatPacket.TYPE_ADMIN) {
            // [名字]: 红色，消息绿色
            text = Component.literal("[" + msg.getSender() + "]:")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(" "))
                    .append(Component.literal(msg.getMessage()).withStyle(ChatFormatting.GREEN));
        } else {
            // [场外通讯] 灰色，玩家名: 黄色，消息重置为默认色
            text = Component.translatable("ccnrcom.ooc.prefix")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" "))
                    .append(Component.literal(msg.getSender() + ": ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(msg.getMessage()));
        }
        mc.player.displayClientMessage(text, false);
        UUID senderId = resolveSenderId(msg.getSender());
        if (msg.getType() == CommsChatPacket.TYPE_ADMIN) {
            CommsMessageStore.get().add(CommsMessage.admin(senderId, msg.getSender(), msg.getMessage()));
            ChatMessageStore.setPendingMeta(new SenderMeta(
                    senderId,
                    net.minecraft.network.chat.Component.literal(msg.getSender())
                            .withStyle(net.minecraft.ChatFormatting.RED),
                    net.minecraft.network.chat.Component.literal(msg.getMessage())
                            .withStyle(net.minecraft.ChatFormatting.GREEN),
                    false,
                    msg.getSender(),
                    false,
                    null));
        } else {
            CommsMessageStore.get().add(CommsMessage.ooc(senderId, msg.getSender(), msg.getMessage()));
            ChatMessageStore.setPendingMeta(new SenderMeta(
                    senderId,
                    net.minecraft.network.chat.Component.translatable("ccnrcom.ooc.prefix")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)
                            .append(net.minecraft.network.chat.Component.literal(msg.getSender() + ": ")
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW)),
                    net.minecraft.network.chat.Component.literal(msg.getMessage()),
                    false,
                    msg.getSender(),
                    false,
                    null));
        }
    }

    /** 频道成员状态更新 */
    public static void handleChannelState(ChannelStatePacket msg) {
        ClientChannelState.setState(msg.getMyChannel(), new java.util.LinkedHashMap<>(msg.getMembers()));
        CommsMessageStore.get().rememberChannel(msg.getMyChannel());
    }

    /** 根据玩家名找 UUID（仅本地显示用，找不到就随机） */
    private static UUID resolveSenderId(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (var p : mc.level.players()) {
                if (p.getGameProfile().getName().equals(name)) {
                    return p.getUUID();
                }
            }
        }
        return UUID.randomUUID();
    }
}
