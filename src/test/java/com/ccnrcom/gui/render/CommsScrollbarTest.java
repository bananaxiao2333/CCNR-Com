/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommsScrollbarTest {

    @Test
    void thumbHeight_proportionalWhenTrackSmallerThanTotal() {
        assertEquals(50, CommsScrollbar.thumbHeight(100, 200));
    }

    @Test
    void thumbHeight_cappedAtTrackH() {
        assertEquals(100, CommsScrollbar.thumbHeight(100, 90));
    }

    @Test
    void thumbHeight_belowMinClampsUp() {
        assertEquals(8, CommsScrollbar.thumbHeight(100, 10000));
    }

    @Test
    void thumbHeight_totalHZeroReturnsTrackH() {
        assertEquals(100, CommsScrollbar.thumbHeight(100, 0));
    }

    @Test
    void thumbY_startOffset() {
        assertEquals(50, CommsScrollbar.thumbY(50, 300, 30, 0, 100));
    }

    @Test
    void thumbY_endOffset() {
        assertEquals(320, CommsScrollbar.thumbY(50, 300, 30, 100, 100));
    }

    @Test
    void thumbY_noTravelReturnsTop() {
        assertEquals(50, CommsScrollbar.thumbY(50, 30, 30, 50, 100));
    }

    @Test
    void alphaTarget_visibleWhenInZoneOrDraggingOrFresh() {
        assertEquals(1f, CommsScrollbar.alphaTarget(true, false, 100));
        assertEquals(1f, CommsScrollbar.alphaTarget(false, true, 100));
        assertEquals(1f, CommsScrollbar.alphaTarget(false, false, 500));
    }

    @Test
    void alphaTarget_hiddenAfterFade() {
        assertEquals(0f, CommsScrollbar.alphaTarget(false, false, CommsScrollbar.FADE_MS + 1));
    }

    @Test
    void isHoveringThumb_hitZone() {
        assertTrue(CommsScrollbar.isHoveringThumb(100, 50, 100, 50, 30));
        assertFalse(CommsScrollbar.isHoveringThumb(99, 50, 100, 50, 30));
        assertFalse(CommsScrollbar.isHoveringThumb(100, 49, 100, 50, 30));
    }

    @Test
    void isInZone_rightEdgeZone() {
        // 热区 = 面板右缘 20px 内
        assertTrue(CommsScrollbar.isInZone(290, 100, 200, 50, 10, 200));
        assertTrue(CommsScrollbar.isInZone(299, 100, 200, 50, 10, 200));
        assertFalse(CommsScrollbar.isInZone(279, 100, 200, 50, 10, 200));
        assertFalse(CommsScrollbar.isInZone(301, 100, 200, 50, 10, 200));
        assertFalse(CommsScrollbar.isInZone(290, 100, 200, 250, 10, 200));
    }
}
