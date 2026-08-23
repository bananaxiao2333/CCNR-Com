/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ccnrcom.chat.CommsMessage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommsMessageStoreTest {

    private static CommsMessage radio(String ch, String sender, String text) {
        return CommsMessage.radio(ch, UUID.randomUUID(), sender, text);
    }

    @Test
    void bucketKey_radioByChannel() {
        assertEquals("r:462.5", CommsMessageStore.bucketKey(radio("462.5", "A", "hi")));
        assertEquals(
                CommsMessageStore.BUCKET_ADMIN,
                CommsMessageStore.bucketKey(CommsMessage.admin(UUID.randomUUID(), "A", "x")));
        assertEquals(
                CommsMessageStore.BUCKET_OOC,
                CommsMessageStore.bucketKey(CommsMessage.ooc(UUID.randomUUID(), "A", "x")));
        assertEquals(CommsMessageStore.BUCKET_SYSTEM, CommsMessageStore.bucketKey(CommsMessage.system("x")));
    }

    @Test
    void add_and_read_keepsOrder() {
        CommsMessageStore store = new CommsMessageStore(50);
        store.add(radio("1.1", "A", "one"));
        store.add(radio("1.1", "B", "two"));
        List<CommsMessage> all = store.all();
        assertEquals(2, all.size());
        assertEquals("one", all.get(0).getText());
        assertEquals("two", all.get(1).getText());
    }

    @Test
    void bucketLimit_trimsOldest() {
        CommsMessageStore store = new CommsMessageStore(3);
        store.add(radio("1.1", "A", "m1"));
        store.add(radio("1.1", "A", "m2"));
        store.add(radio("1.1", "A", "m3"));
        store.add(radio("1.1", "A", "m4"));
        List<CommsMessage> all = store.all();
        assertEquals(3, all.size());
        assertEquals("m2", all.get(0).getText());
    }

    @Test
    void knownChannels_distinctSorted() {
        CommsMessageStore store = new CommsMessageStore(50);
        store.add(radio("9.1", "A", "x"));
        store.add(radio("1.1", "B", "x"));
        store.add(radio("9.1", "C", "x"));
        assertEquals(List.of("1.1", "9.1"), store.knownChannels());
    }

    @Test
    void search_byTextTypeChannel() {
        CommsMessageStore store = new CommsMessageStore(50);
        store.add(radio("1.1", "Alice", "hello world"));
        store.add(CommsMessage.admin(UUID.randomUUID(), "Root", "hello admin"));
        store.add(radio("2.2", "Bob", "hi"));

        assertEquals(2, store.search("hello", null, null).size());
        assertEquals(1, store.search("hello", CommsMessage.Type.RADIO, null).size());
        assertEquals(1, store.search("", null, "2.2").size());
        assertTrue(store.search("nope", null, null).isEmpty());
    }
}
