/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.screen;

import com.ccnrcom.Config;
import com.ccnrcom.chat.CommsMessage;
import com.ccnrcom.client.ClientChannelState;
import com.ccnrcom.gui.render.BubbleLayout;
import com.ccnrcom.gui.render.CommsScrollbar;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.ccnrcom.gui.store.BlockList;
import com.ccnrcom.gui.store.CommsMessageStore;
import com.ccnrcom.gui.theme.CommsTheme;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 全屏气泡对话面板（E33Chat 风格）：消息/频道/搜索，输入发送，右键菜单 */
@OnlyIn(Dist.CLIENT)
public class CommsChatScreen extends Screen {

    private enum Filter {
        ALL,
        RADIO,
        ADMIN,
        OOC
    }

    private enum SendMode {
        RADIO,
        ADMIN,
        OOC
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private int tab = 0; // 0 消息 1 频道 2 搜索
    private Filter filter = Filter.ALL;
    private SendMode sendMode = SendMode.RADIO;
    private float scroll = 0f;
    private boolean stickToBottom = true;

    private EditBox input;
    private EditBox channelInput;
    private EditBox searchInput;
    private float partialTick;
    private final List<Button> filterChips = new ArrayList<>();
    private final List<Button> modeButtons = new ArrayList<>();
    private Button channelSetButton;
    private ContextMenu menu;

    public CommsChatScreen() {
        super(Component.translatable("ccnrcom.gui.title"));
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;

        addRenderableWidget(Button.builder(Component.translatable("ccnrcom.gui.tab.messages"), b -> setTab(0))
                .bounds(8, 4, 60, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("ccnrcom.gui.tab.channels"), b -> setTab(1))
                .bounds(72, 4, 60, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("ccnrcom.gui.tab.search"), b -> setTab(2))
                .bounds(136, 4, 60, 16)
                .build());
        addRenderableWidget(
                Button.builder(Component.translatable("ccnrcom.gui.tab.settings"), b -> Minecraft.getInstance()
                                .setScreen(new CommsConfigScreen()))
                        .bounds(w - 84, 4, 76, 16)
                        .build());

        int iy = h - 30;
        String[] modes = {"ccnrcom.gui.sendmode.radio", "ccnrcom.gui.sendmode.admin", "ccnrcom.gui.sendmode.ooc"};
        for (int i = 0; i < modes.length; i++) {
            final int mi = i;
            modeButtons.add(addRenderableWidget(Button.builder(Component.translatable(modes[i]), btn -> {
                        sendMode = SendMode.values()[mi];
                        menu = null;
                    })
                    .bounds(8 + i * 46, iy, 42, 20)
                    .build()));
        }
        input = new EditBox(
                this.font, 150, iy + 3, w - 150 - 74, 14, Component.translatable("ccnrcom.gui.input.placeholder"));
        input.setMaxLength(Config.MAX_MESSAGE_LENGTH.get());
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.translatable("ccnrcom.gui.send"), b -> sendInput())
                .bounds(w - 66, iy, 58, 20)
                .build());

        channelInput = new EditBox(this.font, 60, 52, w - 160, 14, Component.translatable("ccnrcom.gui.channel.hint"));
        channelInput.setMaxLength(32);
        channelSetButton = addRenderableWidget(Button.builder(
                        Component.translatable("ccnrcom.gui.channel.set"), b -> sendChannel(channelInput.getValue()))
                .bounds(w - 94, 52, 86, 18)
                .build());
        searchInput =
                new EditBox(this.font, 8, 30, w - 16, 14, Component.translatable("ccnrcom.gui.search.placeholder"));
        searchInput.setMaxLength(64);
    }

    private void setTab(int t) {
        tab = t;
        scroll = 0;
        stickToBottom = true;
        menu = null;
    }

    @Override
    public void tick() {
        input.tick();
        channelInput.tick();
        searchInput.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.partialTick = partialTick;
        CommsTheme.Colors c = CommsTheme.colors();
        g.fill(0, 0, this.width, this.height, 0x66000000);
        RoundRectRenderer.fill(
                g,
                4,
                2,
                this.width - 4,
                this.height - 4,
                CommsTheme.RADIUS_LARGE,
                CommsTheme.alphaBlend(c.panelBg, 0xF2));

        g.drawString(this.font, Component.translatable("ccnrcom.gui.title"), 200, 8, c.textPrimary);
        for (Button b : modeButtons) {
            b.render(g, mouseX, mouseY, partialTick);
        }

        if (tab == 0) {
            renderMessages(g, mouseX, mouseY);
        } else if (tab == 1) {
            renderChannels(g, mouseX, mouseY);
        } else {
            renderSearch(g, mouseX, mouseY);
        }

        input.render(g, mouseX, mouseY, partialTick);
        if (tab == 1) {
            channelInput.render(g, mouseX, mouseY, partialTick);
            channelSetButton.render(g, mouseX, mouseY, partialTick);
        }
        if (tab == 2) {
            searchInput.render(g, mouseX, mouseY, partialTick);
        }
        if (menu != null) {
            menu.render(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    // ---------- 消息 tab ----------
    private List<CommsMessage> visibleMessages() {
        List<CommsMessage> out = new ArrayList<>();
        for (CommsMessage m : CommsMessageStore.get().all()) {
            if (BlockList.get().isBlocked(m.getSenderId())) continue;
            switch (filter) {
                case RADIO:
                    if (m.getType() != CommsMessage.Type.RADIO) continue;
                    break;
                case ADMIN:
                    if (m.getType() != CommsMessage.Type.ADMIN) continue;
                    break;
                case OOC:
                    if (m.getType() != CommsMessage.Type.OOC) continue;
                    break;
                default:
                    break;
            }
            out.add(m);
        }
        return out;
    }

    private void renderMessages(GuiGraphics g, int mouseX, int mouseY) {
        CommsTheme.Colors c = CommsTheme.colors();
        int x = 16;
        int y = 32;
        int w = this.width - 32;
        int bottom = this.height - 36;

        // 过滤 chips（手动渲染 + 点击）
        filterChips.clear();
        String[] filters = {
            "ccnrcom.gui.filter.all", "ccnrcom.gui.filter.radio", "ccnrcom.gui.filter.admin", "ccnrcom.gui.filter.ooc"
        };
        int fx = x;
        for (int i = 0; i < filters.length; i++) {
            final Filter f = Filter.values()[i];
            int fw = this.font.width(Component.translatable(filters[i])) + 12;
            Button b = Button.builder(Component.translatable(filters[i]), btn -> {
                        filter = f;
                        scroll = 0;
                        stickToBottom = true;
                    })
                    .bounds(fx, y, fw, 14)
                    .build();
            b.active = filter != f;
            filterChips.add(b);
            b.render(g, mouseX, mouseY, partialTick);
            fx += fw + 4;
        }
        y += 20;

        List<CommsMessage> msgs = visibleMessages();
        if (msgs.isEmpty()) {
            g.drawString(this.font, Component.translatable("ccnrcom.gui.no_messages"), x, y + 20, c.textSecondary);
            return;
        }

        List<List<String>> wraps = new ArrayList<>();
        int contentH = 0;
        for (CommsMessage m : msgs) {
            List<String> lines =
                    BubbleLayout.wrapText(m.getText(), s -> this.font.width(s), w - BubbleLayout.AVATAR_SIZE - 16);
            wraps.add(lines);
            contentH += BubbleLayout.messageStride(lines.size());
        }
        int viewH = bottom - y;
        int maxScroll = Math.max(0, contentH - viewH);
        if (stickToBottom) {
            scroll = maxScroll;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        stickToBottom = scroll >= maxScroll - 2;

        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        int rowY = y;
        for (int i = 0; i < msgs.size(); i++) {
            CommsMessage m = msgs.get(i);
            List<String> lines = wraps.get(i);
            int blockH = BubbleLayout.messageStride(lines.size());
            if (rowY + blockH >= y - 2 && rowY <= bottom + 2) {
                drawBubble(g, m, lines, x, rowY, w, c);
            }
            rowY += blockH;
        }
        g.pose().popPose();

        if (maxScroll > 0) {
            int trackH = viewH;
            int thumbH = CommsScrollbar.thumbHeight(trackH, contentH);
            int thumbY = CommsScrollbar.thumbY(y, trackH, thumbH, (int) scroll, maxScroll);
            int trackX = this.width - 16;
            g.fill(trackX, y, trackX + CommsScrollbar.WIDTH, y + trackH, CommsTheme.alphaBlend(c.panelBorder, 0x40));
            g.fill(
                    trackX,
                    thumbY,
                    trackX + CommsScrollbar.WIDTH,
                    thumbY + thumbH,
                    CommsTheme.alphaBlend(c.textSecondary, 0x90));
        }
    }

    private void drawBubble(
            GuiGraphics g, CommsMessage m, List<String> lines, int x, int y, int w, CommsTheme.Colors c) {
        int avatarX = x;
        int textX = x + BubbleLayout.AVATAR_SIZE + 6;
        int textW = w - BubbleLayout.AVATAR_SIZE - 6;
        int bubbleH = BubbleLayout.messageHeight(lines.size());

        int nameColor = BubbleLayout.nameColorFor(m.getType(), c);
        RoundRectRenderer.fill(
                g,
                avatarX,
                y + 2,
                avatarX + BubbleLayout.AVATAR_SIZE,
                y + 2 + BubbleLayout.AVATAR_SIZE,
                CommsTheme.RADIUS_MEDIUM,
                nameColor);
        String initial = m.getSenderName().isEmpty()
                ? "?"
                : m.getSenderName().substring(0, 1).toUpperCase(Locale.ROOT);
        g.drawString(
                this.font,
                initial,
                avatarX + (BubbleLayout.AVATAR_SIZE - this.font.width(initial)) / 2,
                y + 3,
                0xFFFFFFFF);

        int bubbleColor = BubbleLayout.bubbleColorFor(m.getType(), c);
        int padX = CommsTheme.BUBBLE_PAD_X;
        int padY = CommsTheme.BUBBLE_PAD_Y;
        int textY = y + padY;
        int maxLineW = textW - padX * 2;
        RoundRectRenderer.fill(
                g,
                textX,
                y,
                textX + textW,
                y + bubbleH,
                CommsTheme.RADIUS_MEDIUM,
                CommsTheme.alphaBlend(bubbleColor, 0xC8));

        String time = LocalTime.ofSecondOfDay((m.getTimestamp() / 1000) % 86400).format(TIME);
        String nameLine = m.getSenderName();
        if (m.getType() == CommsMessage.Type.RADIO && !m.getChannel().isEmpty()) {
            nameLine = m.getChannel() + " | " + nameLine;
        }
        g.drawString(this.font, nameLine, textX + padX, textY, nameColor);
        g.drawString(this.font, time, textX + textW - padX - this.font.width(time), textY, c.textSecondary);

        int lineY = textY + BubbleLayout.LINE_H;
        for (String line : lines) {
            g.drawString(this.font, line, textX + padX, lineY, c.textPrimary);
            lineY += BubbleLayout.LINE_H;
        }
    }

    // ---------- 频道 tab ----------
    private void renderChannels(GuiGraphics g, int mouseX, int mouseY) {
        CommsTheme.Colors c = CommsTheme.colors();
        int x = 16;
        int w = this.width - 32;
        String mine = ClientChannelState.getMyChannel();
        g.drawString(
                this.font,
                Component.translatable("ccnrcom.gui.channel.current", mine.isEmpty() ? "-" : mine),
                x,
                36,
                c.textPrimary);

        int y = 80;
        List<String> channels = CommsMessageStore.get().knownChannels();
        if (!mine.isEmpty() && !channels.contains(mine)) {
            channels = new ArrayList<>(channels);
            channels.add(0, mine);
        }
        for (String ch : channels) {
            RoundRectRenderer.fill(
                    g, x, y, x + w, y + 18, CommsTheme.RADIUS_MEDIUM, CommsTheme.alphaBlend(c.panelBgAlt, 0x90));
            g.drawString(this.font, ch, x + 8, y + 5, c.radioText);
            if (ch.equalsIgnoreCase(mine)) {
                g.drawString(this.font, "✓", x + w - 16, y + 5, c.textPrimary);
            }
            y += 22;
        }
        if (channels.isEmpty()) {
            g.drawString(this.font, Component.translatable("ccnrcom.gui.no_messages"), x, y + 4, c.textSecondary);
        }
    }

    private void sendChannel(String channel) {
        String ch = channel == null ? "" : channel.trim();
        if (ch.isEmpty()) return;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand("channel " + ch);
            channelInput.setValue("");
        }
    }

    // ---------- 搜索 tab ----------
    private void renderSearch(GuiGraphics g, int mouseX, int mouseY) {
        CommsTheme.Colors c = CommsTheme.colors();
        searchInput.render(g, mouseX, mouseY, partialTick);
        String q = searchInput.getValue() == null ? "" : searchInput.getValue();
        List<CommsMessage> results =
                q.isBlank() ? List.of() : CommsMessageStore.get().search(q, null, null);
        int y = 56;
        int x = 16;
        int w = this.width - 32;
        for (CommsMessage m : results) {
            if (y > this.height - 40) break;
            String line = "[" + m.getSenderName() + "] " + m.getText();
            if (this.font.width(line) > w) {
                line = this.font.plainSubstrByWidth(line, w - 6) + "…";
            }
            g.drawString(this.font, line, x, y, BubbleLayout.nameColorFor(m.getType(), c));
            y += 12;
        }
        if (results.isEmpty() && !q.isBlank()) {
            g.drawString(this.font, Component.translatable("ccnrcom.gui.no_messages"), x, y + 4, c.textSecondary);
        }
    }

    // ---------- 输入发送 ----------
    private void sendInput() {
        String text = input.getValue() == null ? "" : input.getValue().trim();
        if (text.isEmpty()) return;
        if (Minecraft.getInstance().player != null) {
            switch (sendMode) {
                case ADMIN:
                    Minecraft.getInstance().player.connection.sendCommand("a " + text);
                    break;
                case OOC:
                    Minecraft.getInstance().player.connection.sendCommand("o " + text);
                    break;
                default:
                    Minecraft.getInstance().player.connection.sendCommand("r " + text);
                    break;
            }
        }
        input.setValue("");
    }

    // ---------- 交互 ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu != null) {
            menu.handleClick(mouseX, mouseY);
            return true;
        }
        // 过滤 chips（手动注册的按钮）
        for (Button chip : filterChips) {
            if (chip.isMouseOver(mouseX, mouseY)) {
                chip.onPress();
                return true;
            }
        }
        if (button == 1 && tab == 0) {
            CommsMessage hit = hitMessage(mouseX, mouseY);
            if (hit != null) {
                menu = new ContextMenu(hit, mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 0) {
            scroll -= (float) delta * 16f;
            stickToBottom = false;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.isFocused() && keyCode == 257) {
            sendInput();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 命中检测：返回鼠标所在的消息 */
    private CommsMessage hitMessage(double mouseX, double mouseY) {
        List<CommsMessage> msgs = visibleMessages();
        int x = 16;
        int y = 52;
        int contentTop = (int) (y - scroll);
        for (CommsMessage m : msgs) {
            List<String> lines = BubbleLayout.wrapText(
                    m.getText(), s -> this.font.width(s), this.width - 32 - BubbleLayout.AVATAR_SIZE - 16);
            int blockH = BubbleLayout.messageStride(lines.size());
            int top = contentTop;
            int btm = top + blockH;
            if (mouseX >= x && mouseX <= this.width - 16 && mouseY >= top && mouseY <= btm) {
                return m;
            }
            contentTop += blockH;
        }
        return null;
    }

    // ---------- 右键菜单 ----------
    private class ContextMenu {
        final CommsMessage anchorMessage;
        final int menuX;
        final int menuY;
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        ContextMenu(CommsMessage m, double mx, double my) {
            anchorMessage = m;
            menuX = (int) mx;
            menuY = (int) my;
            labels.add(Component.translatable("ccnrcom.gui.menu.copy").getString());
            actions.add(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.keyboardHandler.setClipboard(m.getText());
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.translatable("ccnrcom.gui.copied"), true);
                }
            });
            if (m.getType() == CommsMessage.Type.RADIO && !m.getChannel().isEmpty()) {
                labels.add(Component.translatable("ccnrcom.gui.menu.switch_channel")
                        .getString());
                actions.add(() -> sendChannel(m.getChannel()));
            }
            boolean blocked = m.getSenderId() != null && BlockList.get().isBlocked(m.getSenderId());
            labels.add(Component.translatable(blocked ? "ccnrcom.gui.menu.unblock" : "ccnrcom.gui.menu.block")
                    .getString());
            actions.add(() -> {
                if (m.getSenderId() != null) {
                    if (blocked) {
                        BlockList.get().unblock(m.getSenderId());
                    } else {
                        BlockList.get().block(m.getSenderId());
                    }
                }
            });
        }

        void render(GuiGraphics g) {
            CommsTheme.Colors c = CommsTheme.colors();
            int w = 110;
            int h = labels.size() * 18 + 6;
            int x = Math.min(menuX, CommsChatScreen.this.width - w - 8);
            int y = Math.min(menuY, CommsChatScreen.this.height - h - 8);
            RoundRectRenderer.fill(
                    g, x, y, x + w, y + h, CommsTheme.RADIUS_MEDIUM, CommsTheme.alphaBlend(c.panelBg, 0xEE));
            for (int i = 0; i < labels.size(); i++) {
                g.drawString(font, labels.get(i), x + 8, y + 5 + i * 18, c.textPrimary);
            }
        }

        void handleClick(double mouseX, double mouseY) {
            int w = 110;
            int h = labels.size() * 18 + 6;
            int x = Math.min(menuX, CommsChatScreen.this.width - w - 8);
            int y = Math.min(menuY, CommsChatScreen.this.height - h - 8);
            int idx = (int) ((mouseY - y) / 18);
            if (mouseX >= x && mouseX <= x + w && idx >= 0 && idx < labels.size()) {
                actions.get(idx).run();
            }
            menu = null;
        }
    }
}
