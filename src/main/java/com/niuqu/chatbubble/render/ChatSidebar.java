/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.niuqu.chatbubble.render;

import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.texture.UiElement;
import com.niuqu.chatbubble.texture.UiTextureManager;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ChatSidebar {

    // CCNR-Com：频道图标
    private static final net.minecraft.resources.ResourceLocation ICON_ADMIN =
            new net.minecraft.resources.ResourceLocation("ccnrcom", "textures/gui/icon_admin.png");
    private static final net.minecraft.resources.ResourceLocation ICON_OOC =
            new net.minecraft.resources.ResourceLocation("ccnrcom", "textures/gui/icon_ooc.png");
    private static final net.minecraft.resources.ResourceLocation ICON_RADIO =
            new net.minecraft.resources.ResourceLocation("ccnrcom", "textures/gui/icon_radio.png");

    public static final int WIDTH = 90;
    private static final int ITEM_H = 22;
    private static final int ICON_S = 20;
    private static final int SEARCH_H = 14;

    private ChatSidebar() {}

    // ---- Hit testing (called from ChatBubbleScreen) ----

    public static boolean handleMouseClicked(
            double mouseX,
            double mouseY,
            String whisperPartner,
            Font font,
            int screenX,
            boolean visible,
            EditBox searchBox,
            int scrollOffset) {
        if (!visible) return false;
        int localX = (int) mouseX - screenX;
        if (localX < 0 || localX > WIDTH) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return false;

        // Search box
        if (mouseY >= 2 && mouseY <= 2 + SEARCH_H) return true;
        // Public tab
        int y = 2 + SEARCH_H + 3;
        if (mouseY >= y && mouseY <= y + ITEM_H) return true;
        // Channel list (CCNR-Com 适配，活跃频道在前)
        y += ITEM_H + 2;
        String filter = searchBox.getValue().toLowerCase().trim();
        int scrollY = y - scrollOffset;
        java.util.List<String> targets = channelList();
        for (String target : targets) {
            String label = target.equals("*admin")
                    ? Component.translatable("ccnrcom.gui.channel.admin").getString()
                    : target.equals("*ooc")
                            ? Component.translatable("ccnrcom.gui.channel.ooc").getString()
                            : target;
            if (!filter.isEmpty() && !label.toLowerCase().contains(filter)) continue;
            if (mouseY >= scrollY && mouseY <= scrollY + ITEM_H) return true;
            scrollY += ITEM_H + 2;
        }
        return false;
    }

    // ---- Rendering (called from ChatBubbleScreen) ----

    public static int render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            ChatBubbleTheme.Colors c,
            int panelW,
            int msgBottom,
            String whisperPartner,
            ResourceLocation publicIcon,
            ResourceLocation noOnlineIcon,
            ResourceLocation privateTipIcon,
            EditBox searchBox,
            int scrollOffset,
            int prevMaxScroll,
            float alpha) {
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                g, UiTextureManager.rl(UiElement.SIDEBAR_BG), 0, 0, WIDTH, 999, alpha);
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                g, UiTextureManager.rl(UiElement.DIVIDER), WIDTH - 1, 0, 1, 999, alpha);

        Minecraft mc = Minecraft.getInstance();
        int y = 2;

        // Search box
        int sbx = 2;
        int sby = 2;
        int sbw = WIDTH - 5;
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                g, UiTextureManager.rl(UiElement.INPUT_BG), sbx - 1, sby, sbw + 1, SEARCH_H, alpha);
        boolean hoverSearch = mouseX >= sbx - 1 && mouseX <= sbx + sbw && mouseY >= sby && mouseY <= sby + SEARCH_H;
        if (hoverSearch || searchBox.isFocused()) g.renderOutline(sbx - 1, sby, sbw + 1, SEARCH_H, c.textMuted());
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(font, Component.translatable("e33chat.sidebar.search"), sbx, sby + 3, c.textMuted(), false);
        }
        y = sby + SEARCH_H + 3;

        // Public tab
        boolean isPublic = whisperPartner == null;
        boolean hoverTab = mouseX >= 0 && mouseX <= WIDTH && mouseY >= y && mouseY <= y + ITEM_H;
        if (isPublic)
            com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                    g, UiTextureManager.rl(UiElement.SIDEBAR_SELECTED), 0, y, WIDTH, ITEM_H, alpha);
        else if (hoverTab)
            com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                    g, UiTextureManager.rl(UiElement.SIDEBAR_HOVER), 0, y, WIDTH, ITEM_H, alpha);
        drawIcon(g, publicIcon, 2, y + 1, ICON_S, alpha);
        int nameX = 2 + ICON_S + 3;
        String publicLabel = Component.translatable("e33chat.sidebar.public").getString();
        g.drawString(font, Component.literal(publicLabel), nameX, y + 1, c.textPrimary(), false);
        ChatMessageStore.ChatMessage latestPub = ChatMessageStore.getLatestPublicMessage();
        if (latestPub != null) {
            int previewMaxW = WIDTH - nameX - 4;
            String preview = ChatMessageStore.singleLine(latestPub.content().getString());
            String previewDisplay = font.plainSubstrByWidth(preview, previewMaxW - font.width("..."));
            if (!previewDisplay.equals(preview)) previewDisplay += "...";
            g.drawString(font, Component.literal(previewDisplay), nameX, y + 1 + font.lineHeight, c.textMuted(), false);
        }
        y += ITEM_H + 2;

        int newMaxScroll = prevMaxScroll;
        // ===== CCNR-Com 适配：侧边栏显示通讯频道（管理/场外/对讲频道），点击即切换发送目标 =====
        // 活跃对讲频道（与当前设定一致）黄色置前，非活跃灰色置底
        java.util.List<String> channels = channelList();
        String filter = searchBox.getValue().toLowerCase().trim();
        String myChannel = com.ccnrcom.client.ClientChannelState.getMyChannel();
        int startY = y;
        int visibleBottom = msgBottom > 0 ? msgBottom : 300;
        java.util.List<String> allTargets = new java.util.ArrayList<>();
        java.util.List<String> allLabels = new java.util.ArrayList<>();
        for (String target : channels) {
            String label;
            if (target.equals("*admin")) {
                label = Component.translatable("ccnrcom.gui.channel.admin").getString();
            } else if (target.equals("*ooc")) {
                label = Component.translatable("ccnrcom.gui.channel.ooc").getString();
            } else {
                label = Component.translatable("ccnrcom.gui.channel.prefix").getString() + " " + target;
            }
            if (filter.isEmpty() || label.toLowerCase().contains(filter)) {
                allTargets.add(target);
                allLabels.add(label);
            }
        }
        int totalH = allTargets.size() * (ITEM_H + 2);
        if (totalH == 0) {
            drawIcon(g, noOnlineIcon, (WIDTH - 32) / 2, startY + 8, 32, alpha);
            String noPlayers =
                    Component.translatable("e33chat.sidebar.no_players").getString();
            int textW = font.width(noPlayers);
            g.drawString(
                    font, Component.literal(noPlayers), (WIDTH - textW) / 2, startY + 8 + 32 + 4, c.textMuted(), false);
        } else {
            newMaxScroll = Math.max(0, totalH - (visibleBottom - startY));
            int clampedOffset = Math.min(scrollOffset, newMaxScroll);
            g.enableScissor(0, startY, WIDTH, visibleBottom);
            int scrollY = startY - clampedOffset;
            for (int i = 0; i < allTargets.size(); i++) {
                String target = allTargets.get(i);
                String label = allLabels.get(i);
                if (scrollY + ITEM_H > startY && scrollY < visibleBottom) {
                    boolean sel = target.equals(whisperPartner);
                    boolean active =
                            target.equals("*admin") || target.equals("*ooc") || target.equalsIgnoreCase(myChannel);
                    boolean hoverRow =
                            active && mouseX >= 0 && mouseX <= WIDTH && mouseY >= scrollY && mouseY <= scrollY + ITEM_H;
                    if (sel)
                        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                                g, UiTextureManager.rl(UiElement.SIDEBAR_SELECTED), 0, scrollY, WIDTH, ITEM_H, alpha);
                    else if (hoverRow)
                        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                                g, UiTextureManager.rl(UiElement.SIDEBAR_HOVER), 0, scrollY, WIDTH, ITEM_H, alpha);

                    drawIcon(
                            g,
                            target.equals("*admin") ? ICON_ADMIN : target.equals("*ooc") ? ICON_OOC : ICON_RADIO,
                            2,
                            scrollY + 1,
                            ICON_S,
                            alpha);
                    int maxNameW = WIDTH - nameX - 4 - 2;
                    String displayName = font.plainSubstrByWidth(label, maxNameW - font.width("..."));
                    if (!displayName.equals(label)) displayName += "...";
                    int labelColor = target.equals("*admin")
                            ? 0xFFFF5252
                            : target.equals("*ooc")
                                    ? 0xFFB0BEC5
                                    : target.equalsIgnoreCase(myChannel) ? 0xFFFFD54F : c.textMuted();
                    g.drawString(font, Component.literal(displayName), nameX, scrollY + 1, labelColor, false);

                    // 该频道最新消息预览
                    String preview = latestPreviewFor(target);
                    if (!preview.isEmpty()) {
                        String previewDisplay = font.plainSubstrByWidth(preview, maxNameW - font.width("..."));
                        if (!previewDisplay.equals(preview)) previewDisplay += "...";
                        g.drawString(
                                font,
                                Component.literal(previewDisplay),
                                nameX,
                                scrollY + 1 + font.lineHeight,
                                c.textMuted(),
                                false);
                    }
                }
                scrollY += ITEM_H + 2;
            }
            g.disableScissor();
        }
        return newMaxScroll;
    }

    /** CCNR-Com：侧边栏频道列表（管理/场外 + 活跃对讲频道在前，其余按名排序） */
    public static java.util.List<String> channelList() {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("*admin");
        out.add("*ooc");
        String myChannel = com.ccnrcom.client.ClientChannelState.getMyChannel();
        java.util.List<String> channels =
                com.ccnrcom.gui.store.CommsMessageStore.get().knownChannels();
        for (String ch : channels) {
            if (ch.equalsIgnoreCase(myChannel) && !out.contains(ch)) out.add(ch);
        }
        for (String ch : channels) {
            if (!ch.equalsIgnoreCase(myChannel) && !out.contains(ch)) out.add(ch);
        }
        return out;
    }

    /** CCNR-Com：某目标的最新消息预览 */
    private static String latestPreviewFor(String target) {
        if (target == null) return "";
        if (target.equals("*admin")) {
            var msgs = com.ccnrcom.gui.store.CommsMessageStore.get()
                    .forBucket(com.ccnrcom.gui.store.CommsMessageStore.BUCKET_ADMIN);
            return msgs.isEmpty()
                    ? ""
                    : com.niuqu.chatbubble.store.ChatMessageStore.singleLine(
                            msgs.get(msgs.size() - 1).getText());
        }
        if (target.equals("*ooc")) {
            var msgs = com.ccnrcom.gui.store.CommsMessageStore.get()
                    .forBucket(com.ccnrcom.gui.store.CommsMessageStore.BUCKET_OOC);
            return msgs.isEmpty()
                    ? ""
                    : com.niuqu.chatbubble.store.ChatMessageStore.singleLine(
                            msgs.get(msgs.size() - 1).getText());
        }
        var msgs = com.ccnrcom.gui.store.CommsMessageStore.get().forBucket("r:" + target);
        return msgs.isEmpty()
                ? ""
                : com.niuqu.chatbubble.store.ChatMessageStore.singleLine(
                        msgs.get(msgs.size() - 1).getText());
    }

    // ---- Helpers ----

    private static void drawPlayerHead(
            GuiGraphics g, ResourceLocation skin, int x, int y, int baseSize, int hatSize, float alpha) {
        if (alpha <= 0.003f) return;
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                g, skin, x, y, baseSize, baseSize, 8.0F, 8.0F, 8, 8, 64, 64, alpha);
        int hatOff = (hatSize - baseSize) / 2;
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                g, skin, x - hatOff, y - hatOff, hatSize, hatSize, 40.0F, 8.0F, 8, 8, 64, 64, alpha);
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation tex, int x, int y, int size, float alpha) {
        if (alpha <= 0.003f) return;
        if (size < 16) {
            // 同 ChatBars.drawIcon：采样内容区 14x14（偏移1,1）完整绘制，避免切掉图标右/下缘
            com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                    g, tex, x, y, size, size, 1f, 1f, 14, 14, 16, 16, alpha);
        } else {
            com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
                    g, tex, x, y, size, size, 0f, 0f, size, size, size, size, alpha);
        }
    }
}
