/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

/**
 * 圆角矩形参数纯计算（无 MC 依赖，可单测）。
 * 移植自 E33Chat RoundRectRenderer 的几何逻辑（MIT）。
 */
public final class RoundRectParams {
    private RoundRectParams() {}

    /** 半径钳制：不超过矩形短边的一半 */
    public static float clampedRadius(float radius, int w, int h) {
        return Math.min(radius, Math.min(w, h) / 2f);
    }

    /** 半径随 Pose 缩放换算（GUI 无旋转，m00 即缩放，取绝对值） */
    public static float scaledRadius(float radius, float poseScale) {
        return radius * Math.abs(poseScale);
    }
}
