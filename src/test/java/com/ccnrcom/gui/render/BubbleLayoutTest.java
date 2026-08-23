/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ccnrcom.chat.CommsMessage;
import com.ccnrcom.gui.theme.CommsTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class BubbleLayoutTest {

    private static final BubbleLayout.WidthProvider WP = s -> s.length() * 6;

    @Test
    void wrapText_splitsLongLines() {
        List<String> lines = BubbleLayout.wrapText("aaa bbb ccc ddd", WP, 60);
        assertTrue(lines.size() >= 2);
        assertTrue(lines.stream().allMatch(l -> WP.width(l) <= 60 || l.contains(" ")));
    }

    @Test
    void wrapText_shortTextSingleLine() {
        List<String> lines = BubbleLayout.wrapText("hi", WP, 60);
        assertEquals(1, lines.size());
        assertEquals("hi", lines.get(0));
    }

    @Test
    void wrapText_emptyText() {
        assertEquals(List.of(""), BubbleLayout.wrapText("", WP, 60));
        assertEquals(List.of(""), BubbleLayout.wrapText(null, WP, 60));
    }

    @Test
    void messageHeight_growsWithLines() {
        assertTrue(BubbleLayout.messageHeight(2) > BubbleLayout.messageHeight(1));
        assertTrue(BubbleLayout.messageStride(2) > BubbleLayout.messageStride(1));
    }

    @Test
    void nameColor_differsPerType() {
        CommsTheme.Colors c = CommsTheme.Colors.DARK;
        int radio = BubbleLayout.nameColorFor(CommsMessage.Type.RADIO, c);
        int admin = BubbleLayout.nameColorFor(CommsMessage.Type.ADMIN, c);
        int ooc = BubbleLayout.nameColorFor(CommsMessage.Type.OOC, c);
        assertTrue(radio != admin);
        assertTrue(admin != ooc);
    }

    @Test
    void bubbleColor_differsPerType() {
        CommsTheme.Colors c = CommsTheme.Colors.DARK;
        assertEquals(c.radioBubble, BubbleLayout.bubbleColorFor(CommsMessage.Type.RADIO, c));
        assertEquals(c.adminBubble, BubbleLayout.bubbleColorFor(CommsMessage.Type.ADMIN, c));
        assertEquals(c.oocBubble, BubbleLayout.bubbleColorFor(CommsMessage.Type.OOC, c));
    }
}
