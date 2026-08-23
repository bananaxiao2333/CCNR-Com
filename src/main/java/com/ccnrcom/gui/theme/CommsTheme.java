/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.theme;

import com.ccnrcom.Config;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * 主题 token（移植自 E33Chat 主题体系，MIT）：
 * light/dark 两套色板 + 形状/间距结构值；AUTO 跟随原版深色背景选项。
 * 纯计算部分（colorsFor / alphaBlend）可单测。
 */
public final class CommsTheme {
    public enum Mode {
        AUTO,
        LIGHT,
        DARK
    }

    // ---- 形状与间距 token（新 UI 一律从本表取值，禁止散落魔法数字） ----
    /** 中档圆角：气泡/面板/菜单 */
    public static final int RADIUS_MEDIUM = 8;
    /** 大档圆角：横幅等大浮层 */
    public static final int RADIUS_LARGE = 16;
    /** 面板内容边距 */
    public static final int PAD = 8;
    /** 气泡内边距 */
    public static final int BUBBLE_PAD_X = 6;

    public static final int BUBBLE_PAD_Y = 4;

    private CommsTheme() {}

    public static Mode mode() {
        String m = Config.THEME_MODE.get();
        try {
            return Mode.valueOf(m.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Mode.AUTO;
        }
    }

    /** 当前主题色板（AUTO 跟随原版深色背景选项） */
    public static Colors colors() {
        return colorsFor(mode(), isVanillaDark());
    }

    /** 纯函数：按模式与原版深色开关决定色板（可单测） */
    public static Colors colorsFor(Mode mode, boolean vanillaDark) {
        if (mode == Mode.LIGHT) return Colors.LIGHT;
        if (mode == Mode.DARK) return Colors.DARK;
        return vanillaDark ? Colors.DARK : Colors.LIGHT;
    }

    private static boolean isVanillaDark() {
        return Minecraft.getInstance().options.darkMojangStudiosBackground().get();
    }

    /** ARGB 合成：rgb + alpha 通道 */
    public static int alphaBlend(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** 主题色板 */
    public static final class Colors {
        public static final Colors DARK = new Colors(
                0xFF1E1E22,
                0xFF2E2E36,
                0xFFFFFFFF,
                0xFF9A9AA5,
                0xFF3A3A44,
                0xFF2F6BFF,
                0xFF9AD0FF,
                0xFF8B3A3A,
                0xFFFF9E9E,
                0xFF3A3A44,
                0xFFB0BEC5);
        public static final Colors LIGHT = new Colors(
                0xFFF5F6F8,
                0xFFE2E4EA,
                0xFF1C1C20,
                0xFF6B6B76,
                0xFFFFFFFF,
                0xFF1F6EFF,
                0xFF0B57D0,
                0xFFE53935,
                0xFFC62828,
                0xFFF0F1F4,
                0xFF78909C);

        public final int panelBg;
        public final int panelBorder;
        public final int textPrimary;
        public final int textSecondary;
        public final int panelBgAlt;
        public final int radioBubble;
        public final int radioText;
        public final int adminBubble;
        public final int adminText;
        public final int oocBubble;
        public final int oocText;

        private Colors(
                int panelBg,
                int panelBorder,
                int textPrimary,
                int textSecondary,
                int panelBgAlt,
                int radioBubble,
                int radioText,
                int adminBubble,
                int adminText,
                int oocBubble,
                int oocText) {
            this.panelBg = panelBg;
            this.panelBorder = panelBorder;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.panelBgAlt = panelBgAlt;
            this.radioBubble = radioBubble;
            this.radioText = radioText;
            this.adminBubble = adminBubble;
            this.adminText = adminText;
            this.oocBubble = oocBubble;
            this.oocText = oocText;
        }
    }
}
