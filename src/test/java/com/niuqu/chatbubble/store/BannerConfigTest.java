/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.niuqu.chatbubble.store;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.niuqu.chatbubble.config.ChatBubbleConfig;
import org.junit.jupiter.api.Test;

/**
 * 顶部横幅"服务端配置优先"语义：默认（未同步）用客户端配置（默认全部关闭）；
 * 收到服务端 BannerConfigSyncPacket 后以服务端为准；断开/重置后回退客户端值。
 */
class BannerConfigTest {

    @Test
    void clientDefaults_areAllOff() {
        try {
            ChatMessageStore.setServerBannerConfig(null, null, null);
            assertFalse(ChatMessageStore.bannerMentionEnabled());
            assertFalse(ChatMessageStore.bannerWhisperEnabled());
            assertFalse(ChatMessageStore.bannerSystemEnabled());
            // 与客户端配置文件默认值一致
            assertFalse(ChatBubbleConfig.MENTION_BANNER_ENABLED.get());
            assertFalse(ChatBubbleConfig.MENTION_WHISPER_BANNER.get());
            assertFalse(ChatBubbleConfig.SYSTEM_BANNER_ENABLED.get());
        } finally {
            ChatMessageStore.setServerBannerConfig(null, null, null);
        }
    }

    @Test
    void serverValues_overrideClient() {
        try {
            ChatMessageStore.setServerBannerConfig(true, false, true);
            assertTrue(ChatMessageStore.bannerMentionEnabled());
            assertFalse(ChatMessageStore.bannerWhisperEnabled());
            assertTrue(ChatMessageStore.bannerSystemEnabled());
        } finally {
            ChatMessageStore.setServerBannerConfig(null, null, null);
        }
    }

    @Test
    void reset_fallsBackToClientDefaults() {
        try {
            ChatMessageStore.setServerBannerConfig(true, true, true);
            assertTrue(ChatMessageStore.bannerMentionEnabled());
            ChatMessageStore.setServerBannerConfig(null, null, null);
            assertFalse(ChatMessageStore.bannerMentionEnabled());
        } finally {
            ChatMessageStore.setServerBannerConfig(null, null, null);
        }
    }
}
