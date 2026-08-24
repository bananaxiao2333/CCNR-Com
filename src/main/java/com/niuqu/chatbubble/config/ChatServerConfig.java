/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.niuqu.chatbubble.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public class ChatServerConfig {
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final ForgeConfigSpec.BooleanValue HISTORY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue USE_TPA;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAT_TEMPLATES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHISPER_TEMPLATES;
    public static final ForgeConfigSpec.BooleanValue TEMPLATE_DEBUG;
    public static final ForgeConfigSpec.BooleanValue MEDIA_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MEDIA_AUTO_CLEAN;
    /** 顶部消息横幅（@提及/引用）服务端开关：默认关闭，设置后优先级高于客户端设置 */
    public static final ForgeConfigSpec.BooleanValue BANNER_MENTION_ENABLED;
    /** 顶部消息横幅（私聊）服务端开关 */
    public static final ForgeConfigSpec.BooleanValue BANNER_WHISPER_ENABLED;
    /** 顶部消息横幅（系统消息）服务端开关 */
    public static final ForgeConfigSpec.BooleanValue BANNER_SYSTEM_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("E33Chat server settings");
        HISTORY_ENABLED = builder.comment("Send recent chat history to players when they join")
                .define("history_enabled", false);
        USE_TPA = builder.comment("Make the head-menu teleport with /tpa (request) instead of /tp")
                .define("use_tpa", false);
        CHAT_TEMPLATES = builder.comment(
                        "Message-format templates for public chat; empty list = disabled (heuristic guards only).",
                        "Placeholders: {prefix} {display_name} {name} {content}. Example: \"[{display_name}]: {content}\"",
                        "A template must end with {content} and contain one name placeholder; first match wins.")
                .defineList("chat_templates", List.of(), obj -> obj instanceof String);
        WHISPER_TEMPLATES = builder.comment(
                        "Message-format templates for private chat (whisper); empty list = disabled.",
                        "Placeholders: {sender} {target} {prefix} {display_name} {content}. Example: \"{sender} → {target}: {content}\"")
                .defineList("whisper_templates", List.of(), obj -> obj instanceof String);
        TEMPLATE_DEBUG = builder.comment("Log failed template matches and parse diagnostics to the client chat log")
                .define("template_debug", false);
        MEDIA_ENABLED = builder.comment(
                        "Host chat image uploads on the server (e33chat://media/<id>, permanent) instead of the third-party host",
                        "When false, clients fall back to the configured third-party host")
                .define("media_enabled", true);
        MEDIA_AUTO_CLEAN = builder.comment(
                        "Auto-delete server-hosted media files older than 7 days (checked on server start, then at most every 6h after uploads)",
                        "When false, uploaded images are kept forever")
                .define("media_auto_clean", true);

        builder.push("banner");
        BANNER_MENTION_ENABLED = builder.comment(
                        "Show the top notification banner for @mentions/quotes to all players.",
                        "Default off (the client default is also off); when set here, this OVERRIDES the client setting")
                .define("mention_enabled", false);
        BANNER_WHISPER_ENABLED = builder.comment(
                        "Show the top notification banner for private/whisper messages to all players. Default off; overrides the client setting")
                .define("whisper_enabled", false);
        BANNER_SYSTEM_ENABLED = builder.comment(
                        "Show the top notification banner for system messages (deaths/joins/broadcasts). Default off; overrides the client setting")
                .define("system_enabled", false);
        builder.pop();

        SERVER_CONFIG = builder.build();
    }
}
