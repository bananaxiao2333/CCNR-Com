/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.screen;

import com.ccnrcom.Config;
import com.ccnrcom.chat.CommsMessage;
import com.ccnrcom.client.ClientChannelState;
import com.ccnrcom.gui.render.CommsScrollbar;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.ccnrcom.gui.store.BlockList;
import com.ccnrcom.gui.store.CommsMessageStore;
import com.ccnrcom.gui.theme.CommsTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** HUD 悬浮窗：消息流 / 频道成员，可拖拽、可折叠（E33Chat 悬浮窗风格） */
@OnlyIn(Dist.CLIENT)
public class CommsHudWindow {
    public static final int WIDTH = 260;
    public static final int TITLE_H = 16;
    public static final int CONTENT_H = 150;

    private static int winX = -1;
    private static int winY = -1;
    private static boolean collapsed = false;
    private static int tab = 0; // 0 消息 1 频道
    private static boolean dragging = false;
    private static int dragOffX;
    private static int dragOffY;
    private static float scroll = 0f;
    private static boolean stick = true;

    private CommsHudWindow() {}

    /** 客户端启动/配置变化时初始化位置 */
    public static void init() {
        winX = Config.HUD_WINDOW_X.get();
        winY = Config.HUD_WINDOW_Y.get();
        if (winX < 0) {
            winX = Minecraft.getInstance().getWindow().getGuiScaledWidth() - WIDTH - 8;
        }
        if (winY < 0) {
            winY = 8;
        }
    }

    public static int windowX() {
        return winX;
    }

    public static int windowY() {
        return winY;
    }

    public static boolean isCollapsed() {
        return collapsed;
    }

    public static void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Config.HUD_ENABLED.get() || !Config.BUBBLE_CHAT.get()) return;

        if (dragging) {
            winX = mouseX - dragOffX;
            winY = mouseY - dragOffY;
        }

        int w = WIDTH;
        int h = collapsed ? TITLE_H : TITLE_H + CONTENT_H;
        int x = winX;
        int y = winY;
        CommsTheme.Colors c = CommsTheme.colors();

        // 窗口底
        RoundRectRenderer.fill(g, x, y, x + w, y + h, CommsTheme.RADIUS_MEDIUM, CommsTheme.alphaBlend(c.panelBg, 0xE6));
        // 标题栏
        RoundRectRenderer.fill(
                g, x, y, x + w, y + TITLE_H, CommsTheme.RADIUS_MEDIUM, CommsTheme.alphaBlend(c.panelBorder, 0x55));
        g.drawString(mc.font, Component.translatable("ccnrcom.gui.title"), x + 6, y + 4, c.textPrimary);
        // 折叠按钮
        g.drawString(mc.font, collapsed ? "+" : "−", x + w - 12, y + 3, c.textSecondary);

        if (collapsed) return;

        // 标签
        String[] tabs = {"ccnrcom.gui.tab.messages", "ccnrcom.gui.tab.channels"};
        int tx = x + 4;
        for (int i = 0; i < tabs.length; i++) {
            int tw = mc.font.width(Component.translatable(tabs[i])) + 10;
            if (tab == i) {
                RoundRectRenderer.fill(
                        g,
                        tx,
                        y + TITLE_H - 2,
                        tx + tw,
                        y + TITLE_H + 10,
                        5f,
                        CommsTheme.alphaBlend(c.radioBubble, 0x60));
            }
            g.drawString(mc.font, Component.translatable(tabs[i]), tx + 5, y + TITLE_H + 1, c.textPrimary);
            tx += tw;
        }

        int cy = y + TITLE_H + 12;
        int ch = h - TITLE_H - 12;
        if (tab == 0) {
            renderMessages(g, x, cy, w, ch, c);
        } else {
            renderMembers(g, x, cy, w, ch, c);
        }
    }

    private static void renderMessages(GuiGraphics g, int x, int y, int w, int h, CommsTheme.Colors c) {
        Minecraft mc = Minecraft.getInstance();
        List<CommsMessage> msgs = new ArrayList<>();
        for (CommsMessage m : CommsMessageStore.get().all()) {
            if (BlockList.get().isBlocked(m.getSenderId())) continue;
            msgs.add(m);
        }
        if (msgs.size() > 40) {
            msgs = msgs.subList(msgs.size() - 40, msgs.size());
        }

        int lineH = 11;
        int contentH = msgs.size() * lineH;
        int maxScroll = Math.max(0, contentH - h);
        if (stick) {
            scroll = maxScroll;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        stick = scroll >= maxScroll - 2;

        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        int rowY = y;
        for (CommsMessage m : msgs) {
            if (rowY >= y - lineH && rowY <= y + h) {
                String line = "[" + m.getSenderName() + "] " + m.getText();
                if (mc.font.width(line) > w - 12) {
                    line = mc.font.plainSubstrByWidth(line, w - 14) + "…";
                }
                int color =
                        switch (m.getType()) {
                            case ADMIN -> c.adminText;
                            case OOC -> c.oocText;
                            case SYSTEM -> c.textSecondary;
                            default -> c.textPrimary;
                        };
                g.drawString(mc.font, line, x + 4, rowY, color);
            }
            rowY += lineH;
        }
        g.pose().popPose();

        if (maxScroll > 0) {
            int thumbH = CommsScrollbar.thumbHeight(h, contentH);
            int thumbY = CommsScrollbar.thumbY(y, h, thumbH, (int) scroll, maxScroll);
            g.fill(x + w - CommsScrollbar.WIDTH - 2, y, x + w - 2, y + h, CommsTheme.alphaBlend(c.panelBorder, 0x40));
            g.fill(
                    x + w - CommsScrollbar.WIDTH - 2,
                    thumbY,
                    x + w - 2,
                    thumbY + thumbH,
                    CommsTheme.alphaBlend(c.textSecondary, 0x90));
        }
    }

    private static void renderMembers(GuiGraphics g, int x, int y, int w, int h, CommsTheme.Colors c) {
        Minecraft mc = Minecraft.getInstance();
        Map<UUID, String> members = ClientChannelState.all();
        int lineH = 12;
        int rowY = y;
        String mine = ClientChannelState.getMyChannel();
        g.drawString(
                mc.font,
                Component.translatable("ccnrcom.gui.channel.current", mine.isEmpty() ? "-" : mine),
                x + 4,
                rowY,
                c.textPrimary);
        rowY += lineH + 4;
        for (Map.Entry<UUID, String> e : members.entrySet()) {
            if (rowY > y + h - lineH) break;
            g.drawString(mc.font, "• " + e.getValue(), x + 4, rowY, c.radioText);
            rowY += lineH;
        }
        if (members.isEmpty()) {
            g.drawString(mc.font, Component.translatable("ccnrcom.gui.no_members"), x + 4, rowY, c.textSecondary);
        }
    }

    /** 鼠标按下/释放（来自 Forge InputEvent.MouseButton） */
    public static void handleMouseButton(double mouseX, double mouseY, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Config.HUD_ENABLED.get() || !Config.BUBBLE_CHAT.get()) return;
        int x = winX;
        int y = winY;
        int w = WIDTH;
        int h = collapsed ? TITLE_H : TITLE_H + CONTENT_H;
        boolean inside = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (!inside) return;

        if (action == 1) { // 按下
            // 折叠按钮
            if (mouseX >= x + w - 16 && mouseX <= x + w && mouseY >= y && mouseY <= y + TITLE_H) {
                collapsed = !collapsed;
                return;
            }
            if (mouseY <= y + TITLE_H) {
                dragging = true;
                dragOffX = (int) mouseX - x;
                dragOffY = (int) mouseY - y;
            }
        } else if (action == 0) { // 释放
            if (dragging) {
                dragging = false;
                persistPosition();
            }
        }
    }

    /** 滚轮（来自 InputEvent.MouseScrolled），仅当鼠标在窗口内时消费 */
    public static boolean handleScroll(double mouseX, double mouseY, double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Config.HUD_ENABLED.get() || !Config.BUBBLE_CHAT.get() || collapsed) return false;
        int x = winX;
        int y = winY;
        int w = WIDTH;
        int h = TITLE_H + CONTENT_H;
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return false;
        if (tab != 0) return false;
        scroll -= (float) delta * 12f;
        stick = false;
        return true;
    }

    private static void persistPosition() {
        Config.HUD_WINDOW_X.set(winX);
        Config.HUD_WINDOW_Y.set(winY);
        try {
            Config.SPEC.save();
        } catch (Exception ignored) {
            // 配置跟踪器退出时自动保存
        }
    }
}
