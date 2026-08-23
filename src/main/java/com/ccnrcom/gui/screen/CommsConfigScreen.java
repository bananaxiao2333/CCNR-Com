/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.screen;

import com.ccnrcom.Config;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.ccnrcom.gui.theme.CommsTheme;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** APP 风格设置屏：直接读写 ForgeConfigSpec（可即时生效） */
@OnlyIn(Dist.CLIENT)
public class CommsConfigScreen extends Screen {

    public CommsConfigScreen() {
        super(Component.translatable("ccnrcom.config.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 110;
        int y = 40;
        addRow(
                cx,
                y,
                "ccnrcom.config.theme",
                () -> CommsTheme.mode().name().toLowerCase(),
                v -> Config.THEME_MODE.set(v));
        y += 24;
        addRow(
                cx,
                y,
                "ccnrcom.config.bubble_chat",
                () -> String.valueOf(Config.BUBBLE_CHAT.get()),
                v -> Config.BUBBLE_CHAT.set(Boolean.parseBoolean(v)));
        y += 24;
        addRow(
                cx,
                y,
                "ccnrcom.config.voice_hud",
                () -> String.valueOf(Config.HUD_ENABLED.get()),
                v -> Config.HUD_ENABLED.set(Boolean.parseBoolean(v)));
        y += 24;
        addRow(
                cx,
                y,
                "ccnrcom.config.history",
                () -> String.valueOf(Config.HISTORY_LIMIT.get()),
                v -> Config.HISTORY_LIMIT.set(Integer.parseInt(v)));
        y += 24;
        addRow(
                cx,
                y,
                "ccnrcom.config.beep",
                () -> String.valueOf(Config.BEEP_ON_RECEIVE.get()),
                v -> Config.BEEP_ON_RECEIVE.set(Boolean.parseBoolean(v)));
        y += 24;
        addRow(
                cx,
                y,
                "ccnrcom.config.actionbar",
                () -> String.valueOf(Config.ACTION_BAR_FEEDBACK.get()),
                v -> Config.ACTION_BAR_FEEDBACK.set(Boolean.parseBoolean(v)));
        y += 36;
        addRenderableWidget(Button.builder(Component.translatable("ccnrcom.gui.tab.messages"), b -> onClose())
                .bounds(this.width / 2 - 40, y, 80, 20)
                .build());
    }

    private void addRow(
            int cx, int y, String labelKey, java.util.function.Supplier<String> current, Consumer<String> setter) {
        addRenderableWidget(Button.builder(Component.translatable(labelKey), b -> {})
                .bounds(cx, y, 140, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("▶ " + current.get()), b -> {
                    String val = current.get();
                    setter.accept(nextValue(val));
                    b.setMessage(Component.literal("▶ " + current.get()));
                    saveConfig();
                })
                .bounds(cx + 144, y, 76, 20)
                .build());
    }

    /** 值循环：bool 翻转；theme 三态；history 档位 */
    private static String nextValue(String val) {
        switch (val) {
            case "true":
                return "false";
            case "false":
                return "true";
            case "auto":
                return "light";
            case "light":
                return "dark";
            case "dark":
                return "auto";
            case "50":
                return "100";
            case "100":
                return "200";
            case "200":
                return "500";
            case "500":
                return "50";
            default:
                return val;
        }
    }

    private static void saveConfig() {
        try {
            Config.SPEC.save();
        } catch (Exception ignored) {
            // 配置跟踪器会在退出时自动保存
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CommsTheme.Colors c = CommsTheme.colors();
        g.fill(0, 0, this.width, this.height, 0x66000000);
        RoundRectRenderer.fill(
                g,
                this.width / 2 - 240,
                10,
                this.width / 2 + 240,
                this.height - 10,
                CommsTheme.RADIUS_LARGE,
                CommsTheme.alphaBlend(c.panelBg, 0xF2));
        g.drawString(
                this.font,
                Component.translatable("ccnrcom.config.title"),
                this.width / 2 - this.font.width(Component.translatable("ccnrcom.config.title")) / 2,
                20,
                c.textPrimary);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
