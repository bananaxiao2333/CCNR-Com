/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * 语言包一致性回归测试：防止"只更新 zh_cn 漏掉 en_us"（红字原始键）这类问题再次出现。
 * 上一版 en_us.json 只有 13 个键，而 zh_cn.json 有 71 个，英文客户端的侧边栏/按键绑定
 * 全部渲染成红色翻译键。
 */
class LangFileTest {

    /** 代码里实际使用、且历史上容易漏翻的关键键 */
    private static final Set<String> REQUIRED_CCNRCOM_KEYS = Set.of(
            "ccnrcom.radio.prefix",
            "ccnrcom.ooc.prefix",
            "ccnrcom.hud.radio",
            "ccnrcom.hud.on",
            "ccnrcom.hud.off",
            "ccnrcom.gui.channel.admin",
            "ccnrcom.gui.channel.ooc",
            "ccnrcom.gui.channel.prefix",
            "ccnrcom.gui.radio_bubble_name",
            "ccnrcom.theme.usage",
            "ccnrcom.theme.changed",
            "ccnrcom.chat.channel_inactive",
            "ccnrcom.broadcast.sent");

    private static Map<String, String> load(String path) {
        InputStream in = LangFileTest.class.getResourceAsStream(path);
        assertNotNull(in, "language file not on classpath: " + path);
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
        assertNotNull(map, "invalid JSON: " + path);
        return map;
    }

    private static Set<String> keySet(Map<String, String> map) {
        return new TreeSet<>(map.keySet());
    }

    @Test
    void ccnrcom_enAndZh_haveIdenticalKeys() {
        Map<String, String> en = load("/assets/ccnrcom/lang/en_us.json");
        Map<String, String> zh = load("/assets/ccnrcom/lang/zh_cn.json");
        assertEquals(keySet(zh), keySet(en), "en_us.json 与 zh_cn.json 键集合不一致");
        assertFalse(en.isEmpty());
    }

    @Test
    void ccnrcom_requiredKeys_areTranslated() {
        Map<String, String> en = load("/assets/ccnrcom/lang/en_us.json");
        Map<String, String> zh = load("/assets/ccnrcom/lang/zh_cn.json");
        for (String key : REQUIRED_CCNRCOM_KEYS) {
            assertTrue(en.containsKey(key), "en_us.json missing: " + key);
            assertTrue(zh.containsKey(key), "zh_cn.json missing: " + key);
            assertFalse(en.get(key).isBlank(), "en_us.json blank: " + key);
            assertFalse(zh.get(key).isBlank(), "zh_cn.json blank: " + key);
            // 值不应是原始键本身（说明误把 key 当文案写了）
            assertFalse(en.get(key).equals(key), "en_us.json key-as-value: " + key);
            assertFalse(zh.get(key).equals(key), "zh_cn.json key-as-value: " + key);
        }
    }

    @Test
    void e33chat_enAndZh_haveIdenticalKeys() {
        Map<String, String> en = load("/assets/e33chat/lang/en_us.json");
        Map<String, String> zh = load("/assets/e33chat/lang/zh_cn.json");
        assertEquals(keySet(zh), keySet(en), "e33chat en_us.json 与 zh_cn.json 键集合不一致");
        assertFalse(en.isEmpty());
    }
}
