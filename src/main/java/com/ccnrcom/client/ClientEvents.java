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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端渲染与输入事件（HUD 绘制、按键、客户端命令）。
 * 注意：注册在 Forge 事件总线（Bus.FORGE）——RenderGuiOverlayEvent / TickEvent /
 * RegisterClientCommandsEvent / RegisterShadersEvent / LivingDeathEvent 均由
 * MinecraftForge.EVENT_BUS 派发；此前挂在默认 MOD 总线上，语音说话者 HUD、
 * /ccnr 命令等全部失效（E33Chat 的监听器用
 * MinecraftForge.EVENT_BUS.register 注册所以正常）。
 */
@Mod.EventBusSubscriber(modid = CCNRComMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    /** 注册圆角矩形 shader */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        RoundRectRenderer.registerShaders(event);
    }

    /** 客户端命令：/ccnr hud | theme */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher()
                .register(Commands.literal("ccnr")
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
                                                        .sendFailure(Component.translatable("ccnrcom.theme.usage"));
                                                return 0;
                                            }
                                            Config.THEME_MODE.set(mode);
                                            Minecraft mc = Minecraft.getInstance();
                                            if (mc.player != null) {
                                                mc.player.displayClientMessage(
                                                        Component.translatable("ccnrcom.theme.changed", mode), false);
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
            mc.player.displayClientMessage(Component.translatable(on ? "ccnrcom.hud.on" : "ccnrcom.hud.off"), false);
        }
        return 1;
    }

    /** 本地玩家死亡：清除自己发出的对讲机通讯记录 */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getEntity() == mc.player) {
            com.ccnrcom.gui.store.CommsMessageStore.get()
                    .removeSenderOfType(mc.player.getUUID(), com.ccnrcom.chat.CommsMessage.Type.RADIO);
        }
    }

    /** 语音说话者 HUD（需要 Plasmo Voice） */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        if (!ModList.get().isLoaded("plasmovoice")) return;
        ClientVoiceHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
