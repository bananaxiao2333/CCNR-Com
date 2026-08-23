/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;

    /** /r 消息的最大长度 */
    public static final ForgeConfigSpec.IntValue MAX_MESSAGE_LENGTH;
    /** 频道格式正则（需包含 ^ 和 $） */
    public static final ForgeConfigSpec.ConfigValue<String> CHANNEL_PATTERN;
    /** 是否允许跨维度收发 */
    public static final ForgeConfigSpec.BooleanValue CROSS_DIMENSION;
    /** /r 文字消息的距离限制（方块），-1 无限制 */
    public static final ForgeConfigSpec.IntValue CHAT_RANGE;
    /** 发送者是否回显自己的消息 */
    public static final ForgeConfigSpec.BooleanValue ECHO_TO_SENDER;
    /** 客户端收到消息时是否播放提示音 */
    public static final ForgeConfigSpec.BooleanValue BEEP_ON_RECEIVE;
    /** 开麦/关麦时在 ActionBar 显示状态反馈 */
    public static final ForgeConfigSpec.BooleanValue ACTION_BAR_FEEDBACK;

    /** GUI 主题：auto / light / dark */
    public static final ForgeConfigSpec.ConfigValue<String> THEME_MODE;
    /** 气泡对话界面总开关（全屏屏 + HUD 悬浮窗） */
    public static final ForgeConfigSpec.BooleanValue BUBBLE_CHAT;
    /** 本地消息历史上限（每桶） */
    public static final ForgeConfigSpec.IntValue HISTORY_LIMIT;
    /** 语音 HUD 总开关 */
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    /** HUD 位置：left / right */
    public static final ForgeConfigSpec.ConfigValue<String> HUD_POSITION;
    /** HUD 水平偏移（像素，GUI 缩放后） */
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_X;
    /** HUD 垂直偏移（像素，GUI 缩放后） */
    public static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;
    /** HUD 缩放 */
    public static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    /** 最多同时显示的说话者 */
    public static final ForgeConfigSpec.IntValue HUD_MAX_ENTRIES;
    /** 背景基础颜色（RRGGBB，默认蓝色；饱和度随音量电平增强） */
    public static final ForgeConfigSpec.ConfigValue<Integer> HUD_COLOR;
    /** 背景最小透明度（0~1，音量越高透明度越高） */
    public static final ForgeConfigSpec.DoubleValue HUD_MIN_ALPHA;

    /** 是否启用 Plasmo Voice 语音联动 */
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;
    /** 语音转发距离限制（方块），-1 无限制 */
    public static final ForgeConfigSpec.IntValue VOICE_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("chat");
        MAX_MESSAGE_LENGTH = builder.comment("The maximum length of a radio message (/r <message>).")
                .defineInRange("maxMessageLength", 256, 1, 1024);
        CHANNEL_PATTERN = builder.comment("Regex pattern for valid radio channels (must include ^ and $).")
                .define("channelPattern", "^\\d{1,4}\\.\\d{1,4}$");
        CROSS_DIMENSION = builder.comment("Allow sending and receiving across dimensions.")
                .define("crossDimension", true);
        CHAT_RANGE = builder.comment("Max distance in blocks for /r chat delivery. -1 = unlimited.")
                .defineInRange("chatRange", -1, -1, 100000000);
        ECHO_TO_SENDER = builder.comment("Echo the message back to the sender.").define("echoToSender", true);
        builder.pop();

        builder.push("voice");
        VOICE_ENABLED = builder.comment(
                        "Enable Plasmo Voice integration: while you talk, players on your channel hear you remotely.")
                .define("voiceEnabled", true);
        VOICE_RANGE = builder.comment("Max distance in blocks for radio voice forwarding. -1 = unlimited.")
                .defineInRange("voiceRange", -1, -1, 100000000);
        BEEP_ON_RECEIVE = builder.comment("Play a beep on the client when receiving a radio message.")
                .define("beepOnReceive", true);
        ACTION_BAR_FEEDBACK = builder.comment(
                        "Show action bar feedback (channel, transmitting status) when pushing to talk.")
                .define("actionBarFeedback", true);
        builder.pop();

        builder.push("gui");
        THEME_MODE = builder.comment("GUI theme: auto (follows vanilla dark background), light, dark.")
                .define("themeMode", "auto");
        BUBBLE_CHAT = builder.comment("Enable the bubble chat UI (fullscreen panel + HUD window).")
                .define("bubbleChat", true);
        HISTORY_LIMIT =
                builder.comment("Max local message history per bucket.").defineInRange("historyLimit", 100, 10, 1000);
        builder.pop();

        builder.push("hud");
        HUD_ENABLED = builder.comment("Show the voice HUD (who is talking) on the side of the screen.")
                .define("hudEnabled", true);
        HUD_POSITION = builder.comment("HUD position: left or right.").define("hudPosition", "right");
        HUD_OFFSET_X = builder.comment("HUD horizontal offset in pixels.").defineInRange("hudOffsetX", 4, 0, 400);
        HUD_OFFSET_Y = builder.comment("HUD vertical offset in pixels.").defineInRange("hudOffsetY", 8, 0, 400);
        HUD_SCALE = builder.comment("HUD scale (0.5 ~ 2.0).").defineInRange("hudScale", 1.0, 0.5, 2.0);
        HUD_MAX_ENTRIES =
                builder.comment("Max speakers shown in the HUD at once.").defineInRange("hudMaxEntries", 8, 1, 12);
        HUD_COLOR = builder.comment(
                        "HUD background base color in RRGGBB (default blue). Saturation grows with voice volume.")
                .define("hudColor", 0x1F6EFF);
        HUD_MIN_ALPHA = builder.comment("HUD background minimum opacity (0~1); volume level raises it toward 1.")
                .defineInRange("hudMinAlpha", 0.25, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }
}
