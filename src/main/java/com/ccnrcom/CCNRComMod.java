/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import com.ccnrcom.client.ClientKeybinds;
import com.ccnrcom.compat.PlasmoVoiceCompat;
import com.ccnrcom.gui.store.CommsMessageStore;
import com.ccnrcom.network.ChannelStatePacket;
import com.ccnrcom.network.CommsChatPacket;
import com.ccnrcom.network.RadioReceivePacket;
import com.ccnrcom.network.VoiceFeedbackPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CCNRComMod.MODID)
public class CCNRComMod {
    public static final String MODID = "ccnrcom";
    private static final String PROTOCOL_VERSION = "1";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private PlasmoVoiceCompat voiceCompat;

    public CCNRComMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(ClientKeybinds::register);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(ModCommands::register);
        MinecraftForge.EVENT_BUS.addListener(Permissions::onGatherNodes);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                RadioReceivePacket.class,
                RadioReceivePacket::encode,
                RadioReceivePacket::decode,
                RadioReceivePacket::handle);
        CHANNEL.registerMessage(
                id++,
                VoiceFeedbackPacket.class,
                VoiceFeedbackPacket::encode,
                VoiceFeedbackPacket::decode,
                VoiceFeedbackPacket::handle);
        CHANNEL.registerMessage(
                id++, CommsChatPacket.class, CommsChatPacket::encode, CommsChatPacket::decode, CommsChatPacket::handle);
        CHANNEL.registerMessage(
                id++,
                ChannelStatePacket.class,
                ChannelStatePacket::encode,
                ChannelStatePacket::decode,
                ChannelStatePacket::handle);
    }

    /** 客户端：初始化 GUI（历史上限、悬浮窗位置） */
    private void clientSetup(FMLClientSetupEvent event) {
        CommsMessageStore.get().setMaxPerBucket(Config.HISTORY_LIMIT.get());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            ChannelSync.syncAll(sp.getServer());
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            ChannelSync.syncAll(sp.getServer());
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        // 注意：不能直接引用 PlasmoVoiceCompat 类（其字段引用了 PV 类型，
        // PV 未安装时加载该类会 NoClassDefFoundError），必须先用 ModList 判断
        if (ModList.get().isLoaded("plasmovoice")) {
            try {
                // 构造函数内部会以 PV addon 方式 load(this)，PV 自动注册事件监听
                voiceCompat = PlasmoVoiceCompat.loadServer();
                LOGGER.info("[CCNR-Com] Plasmo Voice integration enabled");
            } catch (Throwable t) {
                LOGGER.error("[CCNR-Com] Failed to enable Plasmo Voice integration", t);
            }
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        // PlasmoVoiceCompat 的生命周期（注销等）由 PV 的 addon 系统自动管理
        voiceCompat = null;
        ChannelManager.reset();
    }
}
