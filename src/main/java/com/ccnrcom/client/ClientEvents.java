/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import com.ccnrcom.CCNRComMod;
import com.ccnrcom.Config;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** 客户端渲染事件（HUD 绘制；Plasmo Voice 存在时才启用语音 HUD） */
@Mod.EventBusSubscriber(modid = CCNRComMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    /** 注册圆角矩形 shader */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        RoundRectRenderer.registerShaders(event);
    }

    /** 客户端命令：/ccnr theme <auto|light|dark> */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher()
                .register(Commands.literal("ccnr")
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

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        if (!ModList.get().isLoaded("plasmovoice")) return;
        ClientVoiceHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
