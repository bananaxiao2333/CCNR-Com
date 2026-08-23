/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

/**
 * 滚动条纯计算（移植自 E33Chat ChatScrollbar，MIT）。
 * 渲染部分在 HUD 窗口阶段（阶段 3）接入。
 */
public final class CommsScrollbar {
    public static final int WIDTH = 6;
    public static final int MIN_THUMB_H = 8;
    public static final int HOVER_ZONE = 20;
    public static final long FADE_MS = 1000;

    private CommsScrollbar() {}

    /** 滑块高度：按内容/轨道比例，带最小与上限钳制 */
    public static int thumbHeight(int trackH, int totalH) {
        if (totalH <= 0) return trackH;
        int h = Math.max(MIN_THUMB_H, (int) ((long) trackH * trackH / totalH));
        return Math.min(h, trackH);
    }

    /** 滑块 Y：按滚动比例映射到轨道内 */
    public static int thumbY(int trackTop, int trackH, int thumbH, int scrollOffset, int maxScroll) {
        int travelRange = trackH - thumbH;
        if (travelRange <= 0) return trackTop;
        return trackTop + (int) ((long) scrollOffset * travelRange / maxScroll);
    }

    /** 透明度目标：悬停/拖拽/刚滚动过才可见 */
    public static float alphaTarget(boolean inZone, boolean dragging, long sinceMs) {
        return (inZone || dragging || sinceMs < FADE_MS) ? 1f : 0f;
    }

    public static boolean isHoveringThumb(double mouseX, double mouseY, int trackX, int thumbY, int thumbH) {
        return mouseX >= trackX && mouseX < trackX + WIDTH && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    public static boolean isInZone(
            double mouseX, int panelX, int panelW, double mouseY, int msgTop, int effectiveMsgBottom) {
        return mouseX >= panelX + panelW - HOVER_ZONE
                && mouseX <= panelX + panelW
                && mouseY >= msgTop
                && mouseY < effectiveMsgBottom;
    }
}
