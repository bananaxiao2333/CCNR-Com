/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

import com.ccnrcom.chat.CommsMessage;
import com.ccnrcom.gui.theme.CommsTheme;
import java.util.ArrayList;
import java.util.List;

/** 气泡布局纯计算（可单测）：换行、高度、颜色映射 */
public final class BubbleLayout {
    public static final int AVATAR_SIZE = 12;
    public static final int NAME_GAP = 4;
    public static final int LINE_H = 10;

    private BubbleLayout() {}

    /** 名字颜色：对讲=青蓝、管理=红、OOC=灰、系统=次要色 */
    public static int nameColorFor(CommsMessage.Type type, CommsTheme.Colors colors) {
        switch (type) {
            case RADIO:
                return colors.radioText;
            case ADMIN:
                return colors.adminText;
            case OOC:
                return colors.oocText;
            default:
                return colors.textSecondary;
        }
    }

    /** 气泡底色 */
    public static int bubbleColorFor(CommsMessage.Type type, CommsTheme.Colors colors) {
        switch (type) {
            case RADIO:
                return colors.radioBubble;
            case ADMIN:
                return colors.adminBubble;
            case OOC:
                return colors.oocBubble;
            default:
                return colors.panelBgAlt;
        }
    }

    /** 文本换行（宽度来自注入的 provider，保持可测） */
    public static List<String> wrapText(String text, WidthProvider widthProvider, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (widthProvider.width(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.append(candidate.isEmpty() ? word : (line.isEmpty() ? word : " " + word));
                if (line.length() == 0) line.append(word);
            }
        }
        if (line.length() > 0 || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** 消息块总高度 = 名字行 + 文本行 */
    public static int messageHeight(int lineCount) {
        return LINE_H + lineCount * LINE_H + CommsTheme.BUBBLE_PAD_Y * 2;
    }

    /** 聊天面板里每条消息的纵向步进（含间距） */
    public static int messageStride(int lineCount) {
        return messageHeight(lineCount) + 4;
    }

    /** 文本宽度提供者（测试注入假实现） */
    @FunctionalInterface
    public interface WidthProvider {
        int width(String s);
    }
}
