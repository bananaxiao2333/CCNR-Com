/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ccnrcom.gui.theme.CommsTheme.Colors;
import com.ccnrcom.gui.theme.CommsTheme.Mode;
import org.junit.jupiter.api.Test;

class CommsThemeTest {

    @Test
    void lightAndDark_palettesDiffer() {
        assertNotEquals(Colors.LIGHT.panelBg, Colors.DARK.panelBg);
        assertNotEquals(Colors.LIGHT.textPrimary, Colors.DARK.textPrimary);
    }

    @Test
    void auto_followsVanillaDarkOption() {
        assertEquals(Colors.DARK, CommsTheme.colorsFor(Mode.AUTO, true));
        assertEquals(Colors.LIGHT, CommsTheme.colorsFor(Mode.AUTO, false));
    }

    @Test
    void explicitModes_overrideVanilla() {
        assertEquals(Colors.LIGHT, CommsTheme.colorsFor(Mode.LIGHT, true));
        assertEquals(Colors.DARK, CommsTheme.colorsFor(Mode.DARK, false));
    }

    @Test
    void bothPalettes_defineAllTokens() {
        for (Colors c : new Colors[] {Colors.LIGHT, Colors.DARK}) {
            assertTrue(c.panelBg != 0, "panelBg");
            assertTrue(c.panelBorder != 0, "panelBorder");
            assertTrue(c.textPrimary != 0, "textPrimary");
            assertTrue(c.radioBubble != 0, "radioBubble");
            assertTrue(c.adminBubble != 0, "adminBubble");
            assertTrue(c.oocBubble != 0, "oocBubble");
        }
    }

    @Test
    void alphaBlend_combinesRgbWithAlphaChannel() {
        assertEquals(0x80FFFFFF, CommsTheme.alphaBlend(0xFFFFFF, 0x80));
        assertEquals(0x001F6EFF, CommsTheme.alphaBlend(0x1F6EFF, 0x00));
    }

    @Test
    void shapeTokens_positive() {
        assertTrue(CommsTheme.RADIUS_MEDIUM > 0);
        assertTrue(CommsTheme.RADIUS_LARGE > CommsTheme.RADIUS_MEDIUM);
        assertTrue(CommsTheme.PAD > 0);
        assertTrue(CommsTheme.BUBBLE_PAD_X > 0);
        assertTrue(CommsTheme.BUBBLE_PAD_Y > 0);
    }
}
