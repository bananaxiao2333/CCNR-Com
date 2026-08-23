/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.compat;

import com.ccnrcom.CCNRComMod;
import com.ccnrcom.ChannelManager;
import com.ccnrcom.Config;
import com.ccnrcom.network.VoiceFeedbackPacket;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.source.ServerPlayerSource;
import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationEndEvent;
import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationStartEvent;
import su.plo.voice.api.server.event.audio.source.ServerSourceAudioPacketEvent;
import su.plo.voice.api.server.event.audio.source.ServerSourcePacketEvent;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.api.server.socket.UdpConnection;
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket;
import su.plo.voice.proto.packets.tcp.clientbound.SourceInfoPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.server.ModVoiceServer;

/**
 * Plasmo Voice 联动（以 PV Addon 形式注册，PV 自动管理生命周期）。
 *
 * 1) 语音转发：PV 发送语音包时触发 {@link ServerSourceAudioPacketEvent}，
 *    我们把说话者的语音包以 0 距离（全音量）额外转发给同频道玩家，实现远程对讲；
 *    同时转发 {@link SourceInfoPacket}（远端客户端只有收到它才知道音源存在）和结束包。
 * 2) 开麦反馈：通过激活事件向玩家发送 ActionBar 提示（频道、对讲状态）。
 *
 * 只有安装了 Plasmo Voice 时本类才会被加载（由 CCNRComMod 在 ModList 判断后实例化）。
 */
@Addon(
        id = "ccnrcom",
        name = "CCNR-Com",
        scope = AddonLoaderScope.ANY_SERVER,
        version = "1.0.0",
        authors = {"CCNR"},
        dependencies = {})
public class PlasmoVoiceCompat implements AddonInitializer {

    private static final Logger LOGGER = LogManager.getLogger();

    private final PlasmoVoiceServer voiceServer;

    /** 转发帧计数（用于低频日志） */
    private long forwardCounter = 0L;

    private PlasmoVoiceCompat(PlasmoVoiceServer voiceServer) {
        this.voiceServer = voiceServer;
    }

    /** 服务端侧加载（CCNRComMod 在服务器启动时调用）：语音转发 + 开麦反馈 */
    public static PlasmoVoiceCompat loadServer() {
        ModVoiceServer voice = ModVoiceServer.INSTANCE;
        if (voice == null) {
            throw new IllegalStateException("Plasmo Voice server not available");
        }
        PlasmoVoiceCompat compat = new PlasmoVoiceCompat(voice);
        // 注册为 PV addon：load() 会校验 @Addon 注解并自动注册/注销本类的事件监听
        voice.getAddonManager().load(compat);
        return compat;
    }

    @Override
    public void onAddonInitialize() {
        LOGGER.info("[CCNR-Com] Plasmo Voice addon initialized");
    }

    @Override
    public void onAddonShutdown() {
        // nothing to clean up
    }

    /** 语音数据包：转发给同频道且不在近距听音范围内的玩家 */
    @EventSubscribe
    public void onAudioPacket(ServerSourceAudioPacketEvent event) {
        if (voiceServer == null) return;
        if (!Config.VOICE_ENABLED.get()) return;
        if (!(event.getSource() instanceof ServerPlayerSource)) return;

        ServerPlayerSource source = (ServerPlayerSource) event.getSource();
        VoiceServerPlayer speaker = source.getPlayer();
        String channel = channelOf(speaker);
        if (channel == null) return;

        UUID speakerId = uuidOf(speaker);
        SourceAudioPacket packet = event.getPacket();
        ServerPos3d sourcePos = source.getPosition();

        int forwarded = 0;
        for (UdpConnection conn : voiceServer.getUdpConnectionManager().getConnections()) {
            VoiceServerPlayer listener = (VoiceServerPlayer) conn.getPlayer();
            if (!channel.equals(channelOf(listener))) continue;
            if (speakerId.equals(uuidOf(listener))) continue;
            // 对讲机无视距离：同频道玩家（除说话者本人）一律转发
            if (!needsRadioForward(sourcePos, listener)) continue;

            // 客户端音量按 实际距离/包距离 计算，包距离必须大于实际距离才能听到；
            // 这里按收听者距离放大（short 上限 32767），实现近满音量远程对讲
            conn.sendPacket(new SourceAudioPacket(
                    packet.getSequenceNumber(),
                    packet.getSourceState(),
                    packet.getData(),
                    packet.getSourceId(),
                    radioDistance()));
            forwarded++;
        }
        if (forwarded > 0 && (++forwardCounter % 50) == 0) {
            LOGGER.info(
                    "[CCNR-Com] radio voice: forwarding from {} to {} listeners on channel {}",
                    speakerId,
                    forwarded,
                    channel);
        }
    }

    /** 音源信息包 / 语音结束包：同样转发，让远端客户端知道音源并正常收尾 */
    @EventSubscribe
    public void onSourcePacket(ServerSourcePacketEvent event) {
        if (voiceServer == null) return;
        if (!Config.VOICE_ENABLED.get()) return;
        if (!(event.getSource() instanceof ServerPlayerSource)) return;
        if (!(event.getPacket() instanceof SourceInfoPacket) && !(event.getPacket() instanceof SourceAudioEndPacket))
            return;

        ServerPlayerSource source = (ServerPlayerSource) event.getSource();
        VoiceServerPlayer speaker = source.getPlayer();
        String channel = channelOf(speaker);
        if (channel == null) return;

        UUID speakerId = uuidOf(speaker);
        ServerPos3d sourcePos = source.getPosition();

        for (UdpConnection conn : voiceServer.getUdpConnectionManager().getConnections()) {
            VoiceServerPlayer listener = (VoiceServerPlayer) conn.getPlayer();
            if (!channel.equals(channelOf(listener))) continue;
            if (speakerId.equals(uuidOf(listener))) continue;
            if (!needsRadioForward(sourcePos, listener)) continue;

            conn.sendPacket(event.getPacket());
        }
    }

    /** 开麦：ActionBar 显示频道与对讲状态，播放"滴"声 */
    @EventSubscribe
    public void onActivationStart(PlayerServerActivationStartEvent event) {
        if (voiceServer == null) return;
        if (!Config.VOICE_ENABLED.get() || !Config.ACTION_BAR_FEEDBACK.get()) return;
        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        ServerPlayer sp = player.getInstance().getInstance();
        String channel = ChannelManager.get(sp.getUUID()).orElse(null);
        if (channel != null) {
            sendFeedback(sp, "ccnrcom.voice.talking", channel, 1.8F);
        } else {
            sendFeedback(sp, "ccnrcom.voice.no_channel", "", 0F);
        }
    }

    /** 关麦：ActionBar 显示对讲结束，播放"滴"声 */
    @EventSubscribe
    public void onActivationEnd(PlayerServerActivationEndEvent event) {
        if (voiceServer == null) return;
        if (!Config.VOICE_ENABLED.get() || !Config.ACTION_BAR_FEEDBACK.get()) return;
        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        ServerPlayer sp = player.getInstance().getInstance();
        String channel = ChannelManager.get(sp.getUUID()).orElse("");
        sendFeedback(sp, "ccnrcom.voice.talk_end", channel, 1.2F);
    }

    private void sendFeedback(ServerPlayer sp, String key, String arg, float pitch) {
        MinecraftServer server = sp.getServer();
        if (server == null) return;
        server.execute(() -> CCNRComMod.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> sp), new VoiceFeedbackPacket(key, arg, pitch)));
    }

    /**
     * 对讲机无视距离：包距离直接取 short 上限 32767，
     * 客户端增益 = 1 - 实际距离/包距离 ≈ 1，canHear 恒成立 → 满音量。
     */
    private short radioDistance() {
        return 32767;
    }

    /**
     * 判断是否需要对 listener 做对讲机转发：
     * 对讲机无视距离——同频道玩家（除本人外）一律转发，0 格也是满音量；
     * 仅跨维度（关）或超出 voiceRange 时跳过。
     */
    private boolean needsRadioForward(ServerPos3d sourcePos, VoiceServerPlayer listener) {
        ServerPos3d listenerPos = new ServerPos3d();
        listener.getInstance().getServerPosition(listenerPos);

        boolean sameWorld = sourcePos.getWorld() != null && sourcePos.getWorld().equals(listenerPos.getWorld());
        if (!sameWorld) {
            return Config.CROSS_DIMENSION.get();
        }

        int range = Config.VOICE_RANGE.get();
        if (range < 0) return true;

        double distSq = sourcePos.distanceSquared(listenerPos);
        return distSq <= (double) range * (double) range;
    }

    private static String channelOf(VoiceServerPlayer player) {
        return ChannelManager.get(uuidOf(player)).orElse(null);
    }

    private static UUID uuidOf(VoiceServerPlayer player) {
        ServerPlayer sp = player.getInstance().getInstance();
        return sp.getUUID();
    }
}
