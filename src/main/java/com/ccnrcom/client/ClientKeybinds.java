/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 客户端按键绑定 */
@OnlyIn(Dist.CLIENT)
public class ClientKeybinds {
    /** 打开对讲面板（默认 G） */
    public static KeyMapping CHAT_KEY;

    public static void register(RegisterKeyMappingsEvent event) {
        CHAT_KEY = new KeyMapping("key.ccnrcom.chat", GLFW.GLFW_KEY_G, "key.categories.ccnrcom");
        event.register(CHAT_KEY);
    }
}
