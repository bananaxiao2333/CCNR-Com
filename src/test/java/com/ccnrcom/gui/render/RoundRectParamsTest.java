/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoundRectParamsTest {

    @Test
    void clampedRadius_beyondHalfShortSide() {
        assertEquals(5f, RoundRectParams.clampedRadius(10f, 10, 20));
    }

    @Test
    void clampedRadius_smallRadiusKept() {
        assertEquals(4f, RoundRectParams.clampedRadius(4f, 10, 20));
    }

    @Test
    void clampedRadius_zeroSize() {
        assertEquals(0f, RoundRectParams.clampedRadius(8f, 0, 10));
    }

    @Test
    void scaledRadius_followsPoseScale() {
        assertEquals(16f, RoundRectParams.scaledRadius(8f, 2f));
        assertEquals(16f, RoundRectParams.scaledRadius(8f, -2f));
        assertEquals(0f, RoundRectParams.scaledRadius(8f, 0f));
    }
}
