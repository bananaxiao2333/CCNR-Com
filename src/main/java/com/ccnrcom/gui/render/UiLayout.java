/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

/** 基础布局纯计算（移植自 E33Chat UiLayout，MIT） */
public final class UiLayout {
    private UiLayout() {}

    public static int centerX(int containerLeft, int containerWidth, int elemWidth) {
        return containerLeft + (containerWidth - elemWidth) / 2;
    }

    public static int clampX(int x, int minX) {
        return Math.max(x, minX);
    }

    public static int clampW(int x, int w, int maxX) {
        if (x + w > maxX) return maxX - x;
        return w;
    }
}
