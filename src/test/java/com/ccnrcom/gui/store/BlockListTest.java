/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.store;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockListTest {

    @Test
    void blockAndUnblock() {
        BlockList list = new BlockList();
        UUID id = UUID.randomUUID();
        assertFalse(list.isBlocked(id));
        list.block(id);
        assertTrue(list.isBlocked(id));
        list.unblock(id);
        assertFalse(list.isBlocked(id));
    }

    @Test
    void nullIdNeverBlocked() {
        BlockList list = new BlockList();
        assertFalse(list.isBlocked(null));
    }

    @Test
    void clearRemovesAll() {
        BlockList list = new BlockList();
        list.block(UUID.randomUUID());
        list.block(UUID.randomUUID());
        list.clear();
        assertTrue(list.all().isEmpty());
    }
}
