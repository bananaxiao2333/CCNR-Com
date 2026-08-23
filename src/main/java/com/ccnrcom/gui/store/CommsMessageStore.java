/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.store;

import com.ccnrcom.chat.CommsMessage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 客户端消息存储：按频道/类型分桶，带上限与搜索（纯 Java，可单测） */
public final class CommsMessageStore {
    /** 管理通讯桶 */
    public static final String BUCKET_ADMIN = "*admin";
    /** 场外通讯桶 */
    public static final String BUCKET_OOC = "*ooc";
    /** 系统消息桶 */
    public static final String BUCKET_SYSTEM = "*system";

    private static final CommsMessageStore INSTANCE = new CommsMessageStore(100);

    private final Map<String, Deque<CommsMessage>> buckets = new HashMap<>();
    private int maxPerBucket;

    public CommsMessageStore(int maxPerBucket) {
        this.maxPerBucket = Math.max(1, maxPerBucket);
    }

    /** 全局客户端实例 */
    public static CommsMessageStore get() {
        return INSTANCE;
    }

    /** 客户端启动时按配置调整上限 */
    public void setMaxPerBucket(int max) {
        this.maxPerBucket = Math.max(1, max);
    }

    /** 消息所属桶：对讲按频道，其余按类型 */
    public static String bucketKey(CommsMessage m) {
        switch (m.getType()) {
            case RADIO:
                return m.getChannel().isEmpty() ? BUCKET_SYSTEM : "r:" + m.getChannel();
            case ADMIN:
                return BUCKET_ADMIN;
            case OOC:
                return BUCKET_OOC;
            default:
                return BUCKET_SYSTEM;
        }
    }

    public synchronized void add(CommsMessage m) {
        Deque<CommsMessage> bucket = buckets.computeIfAbsent(bucketKey(m), k -> new ArrayDeque<>());
        bucket.addLast(m);
        while (bucket.size() > maxPerBucket) {
            bucket.removeFirst();
        }
    }

    /** 某桶消息（旧→新） */
    public synchronized List<CommsMessage> forBucket(String key) {
        Deque<CommsMessage> bucket = buckets.get(key);
        return bucket == null ? List.of() : new ArrayList<>(bucket);
    }

    /** 全部消息（旧→新） */
    public synchronized List<CommsMessage> all() {
        List<CommsMessage> out = new ArrayList<>();
        for (Deque<CommsMessage> bucket : buckets.values()) {
            out.addAll(bucket);
        }
        out.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return out;
    }

    /** 搜索：文本 + 类型 + 频道过滤（空串/空 = 不限） */
    public synchronized List<CommsMessage> search(String text, CommsMessage.Type type, String channel) {
        List<CommsMessage> out = new ArrayList<>();
        String needle = text == null ? "" : text.toLowerCase();
        for (Deque<CommsMessage> bucket : buckets.values()) {
            for (CommsMessage m : bucket) {
                if (type != null && m.getType() != type) continue;
                if (channel != null && !channel.isEmpty() && !m.getChannel().equalsIgnoreCase(channel)) continue;
                if (!needle.isEmpty()
                        && !m.getText().toLowerCase().contains(needle)
                        && !m.getSenderName().toLowerCase().contains(needle)) continue;
                out.add(m);
            }
        }
        out.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return out;
    }

    /** 已知对讲频道（有消息记录的） */
    public synchronized List<String> knownChannels() {
        return buckets.keySet().stream()
                .filter(k -> k.startsWith("r:"))
                .map(k -> k.substring(2))
                .distinct()
                .sorted()
                .toList();
    }
}
