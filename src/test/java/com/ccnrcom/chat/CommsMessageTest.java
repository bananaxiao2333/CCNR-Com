/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommsMessageTest {

    @Test
    void radioFactory_setsTypeChannelSender() {
        UUID id = UUID.randomUUID();
        CommsMessage m = CommsMessage.radio("462.5", id, "Steve", "hello");
        assertEquals(CommsMessage.Type.RADIO, m.getType());
        assertEquals("462.5", m.getChannel());
        assertEquals(id, m.getSenderId());
        assertEquals("Steve", m.getSenderName());
        assertEquals("hello", m.getText());
    }

    @Test
    void adminAndOocFactories_haveEmptyChannel() {
        UUID id = UUID.randomUUID();
        assertEquals(
                CommsMessage.Type.ADMIN, CommsMessage.admin(id, "Admin", "x").getType());
        assertEquals(CommsMessage.Type.OOC, CommsMessage.ooc(id, "P", "y").getType());
        assertEquals("", CommsMessage.admin(id, "A", "x").getChannel());
        assertEquals("", CommsMessage.ooc(id, "P", "y").getChannel());
    }

    @Test
    void systemFactory_noSender() {
        CommsMessage m = CommsMessage.system("频道已设定");
        assertEquals(CommsMessage.Type.SYSTEM, m.getType());
        assertNull(m.getSenderId());
        assertEquals("", m.getSenderName());
    }

    @Test
    void equality_sameFields() {
        UUID id = UUID.randomUUID();
        CommsMessage a = CommsMessage.radio("1.1", id, "A", "t");
        CommsMessage b = CommsMessage.radio("1.1", id, "A", "t");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, CommsMessage.radio("1.2", id, "A", "t"));
        assertNotEquals(a, CommsMessage.ooc(id, "A", "t"));
    }
}
