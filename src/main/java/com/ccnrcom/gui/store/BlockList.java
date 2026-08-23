/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.store;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 客户端屏蔽列表（会话内，可单测） */
public final class BlockList {
    private static final BlockList INSTANCE = new BlockList();

    private final Set<UUID> blocked = new HashSet<>();

    public static BlockList get() {
        return INSTANCE;
    }

    public void block(UUID playerId) {
        blocked.add(playerId);
    }

    public void unblock(UUID playerId) {
        blocked.remove(playerId);
    }

    public boolean isBlocked(UUID playerId) {
        return playerId != null && blocked.contains(playerId);
    }

    public Set<UUID> all() {
        return Collections.unmodifiableSet(blocked);
    }

    public void clear() {
        blocked.clear();
    }
}
