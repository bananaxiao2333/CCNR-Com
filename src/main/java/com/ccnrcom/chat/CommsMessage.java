/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.chat;

import java.util.Objects;
import java.util.UUID;

/** 通讯消息模型（纯数据，可单测）：对讲机 / 管理 / 场外 / 系统 */
public final class CommsMessage {
    public enum Type {
        RADIO,
        ADMIN,
        OOC,
        SYSTEM
    }

    private final Type type;
    private final String channel;
    private final UUID senderId;
    private final String senderName;
    private final long timestamp;
    private final String text;

    public CommsMessage(Type type, String channel, UUID senderId, String senderName, long timestamp, String text) {
        this.type = type;
        this.channel = channel;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.text = text;
    }

    public static CommsMessage radio(String channel, UUID senderId, String senderName, String text) {
        return new CommsMessage(Type.RADIO, channel, senderId, senderName, System.currentTimeMillis(), text);
    }

    public static CommsMessage admin(UUID senderId, String senderName, String text) {
        return new CommsMessage(Type.ADMIN, "", senderId, senderName, System.currentTimeMillis(), text);
    }

    public static CommsMessage ooc(UUID senderId, String senderName, String text) {
        return new CommsMessage(Type.OOC, "", senderId, senderName, System.currentTimeMillis(), text);
    }

    public static CommsMessage system(String text) {
        return new CommsMessage(Type.SYSTEM, "", null, "", System.currentTimeMillis(), text);
    }

    public Type getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommsMessage that)) return false;
        return timestamp == that.timestamp
                && type == that.type
                && Objects.equals(channel, that.channel)
                && Objects.equals(senderId, that.senderId)
                && Objects.equals(senderName, that.senderName)
                && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, channel, senderId, senderName, timestamp, text);
    }

    @Override
    public String toString() {
        return "CommsMessage{" + type + " " + (channel.isEmpty() ? "" : "[" + channel + "] ") + senderName + ": " + text
                + "}";
    }
}
