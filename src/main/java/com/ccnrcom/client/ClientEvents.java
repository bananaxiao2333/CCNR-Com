/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import com.ccnrcom.CCNRComMod;
import com.ccnrcom.Config;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.ccnrcom.gui.screen.CommsChatScreen;
import com.ccnrcom.gui.screen.CommsHudWindow;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端渲染与输入事件（HUD 绘制、按键、客户端命令） */
@Mod.EventBusSubscriber(modid = CCNRComMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    /** 注册圆角矩形 shader */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        RoundRectRenderer.registerShaders(event);
    }

    /** 客户端命令：/ccnr chat | theme | hud */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher()
                .register(Commands.literal("ccnr")
                        .then(Commands.literal("chat").executes(ctx -> {
                            Minecraft.getInstance().setScreen(new CommsChatScreen());
                            return 1;
                        }))
                        .then(Commands.literal("hud")
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .executes(ctx -> setHud(StringArgumentType.getString(ctx, "state"))))
                                .executes(ctx -> setHud("toggle")))
                        .then(Commands.literal("theme")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String mode = StringArgumentType.getString(ctx, "mode")
                                                    .toLowerCase(Locale.ROOT);
                                            if (!mode.equals("auto") && !mode.equals("light") && !mode.equals("dark")) {
                                                ctx.getSource()
                                                        .sendFailure(
                                                                Component.literal("用法: /ccnr theme <auto|light|dark>"));
                                                return 0;
                                            }
                                            Config.THEME_MODE.set(mode);
                                            Minecraft mc = Minecraft.getInstance();
                                            if (mc.player != null) {
                                                mc.player.displayClientMessage(
                                                        Component.literal("主题已切换: " + mode), false);
                                            }
                                            return 1;
                                        }))));
    }

    private static int setHud(String state) {
        boolean on =
                switch (state.toLowerCase(Locale.ROOT)) {
                    case "on" -> true;
                    case "off" -> false;
                    default -> !Config.HUD_ENABLED.get();
                };
        Config.HUD_ENABLED.set(on);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("语音/通讯 HUD: " + (on ? "开" : "关")), false);
        }
        return 1;
    }

    /** 快捷键：打开对讲面板 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (ClientKeybinds.CHAT_KEY != null && ClientKeybinds.CHAT_KEY.consumeClick() && mc.screen == null) {
            mc.setScreen(new CommsChatScreen());
        }
    }

    /** 鼠标：HUD 悬浮窗拖拽/折叠 */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        CommsHudWindow.handleMouseButton(scaledMouseX(mc), scaledMouseY(mc), event.getAction());
    }

    /** 滚轮：HUD 悬浮窗内滚动 */
    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (CommsHudWindow.handleScroll(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        CommsHudWindow.render(
                event.getGuiGraphics(),
                (int) scaledMouseX(Minecraft.getInstance()),
                (int) scaledMouseY(Minecraft.getInstance()),
                event.getPartialTick());
    }

    private static double scaledMouseX(Minecraft mc) {
        return mc.mouseHandler.xpos()
                * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth();
    }

    private static double scaledMouseY(Minecraft mc) {
        return mc.mouseHandler.ypos()
                * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight();
    }
}
