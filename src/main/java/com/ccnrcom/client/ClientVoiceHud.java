/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.client;

import com.ccnrcom.Config;
import com.ccnrcom.gui.render.RoundRectRenderer;
import com.ccnrcom.gui.theme.CommsTheme;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import su.plo.voice.api.client.audio.source.ClientAudioSource;
import su.plo.voice.client.ModVoiceClient;
import su.plo.voice.proto.data.audio.source.PlayerSourceInfo;

/**
 * 语音说话者 HUD（屏幕侧边）。
 * 对讲机转发与近距语音都会激活对应的玩家音源，统一显示谁在说话；
 * 色条亮度随有效音量，近场/对讲机区分（对讲带标签、稍暗、排后面）。
 */
@OnlyIn(Dist.CLIENT)
public class ClientVoiceHud {

    /** 各说话者最近一次活跃时间戳（宽限期判断用） */
    private static final Map<UUID, Long> LAST_ACTIVE = new ConcurrentHashMap<>();
    /** 说话停止后条目保留时长（毫秒），避免音量瞬时波动导致条目闪烁消失 */
    private static final long GRACE_MS = 1000L;
    /** 活跃期间色条最低亮度（避免音量接近 0 时条目不可见） */
    private static final float MIN_BAR_LEVEL = 0.12F;

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Config.HUD_ENABLED.get()) return;
        ModVoiceClient voice = ModVoiceClient.INSTANCE;
        if (voice == null || voice.getSourceManager() == null) return;

        long now = System.currentTimeMillis();
        UUID selfId = mc.player.getUUID();
        List<Entry> entries = new ArrayList<>();
        for (ClientAudioSource<?> source : voice.getSourceManager().getSources()) {
            if (source == null || source.isClosed()) continue;
            if (!(source.getSourceInfo() instanceof PlayerSourceInfo)) continue;
            PlayerSourceInfo info = (PlayerSourceInfo) source.getSourceInfo();
            UUID id = info.getPlayerInfo().getPlayerId();
            if (id.equals(selfId)) continue;

            if (source.isActivated()) {
                LAST_ACTIVE.put(id, now);
            }
            // 宽限期：最近 1 秒内说过话就继续显示，避免瞬时音量波动导致条目闪烁消失
            Long last = LAST_ACTIVE.get(id);
            if (last == null || now - last > GRACE_MS) continue;

            // 色条亮度 = 有效音量（跟随距离/增益），但保持最低可见亮度
            float bar = Math.max(source.getEffectiveVolume(), MIN_BAR_LEVEL);
            // 是否我频道上的对讲机说话者（区别于近场语音）
            boolean radio = ClientChannelState.isOnMyChannel(id);
            entries.add(new Entry(info.getPlayerInfo().getPlayerNick(), bar, radio));
        }
        // 清理超过宽限期的缓存
        LAST_ACTIVE.entrySet().removeIf(e -> now - e.getValue() > GRACE_MS);
        if (entries.isEmpty()) return;

        // 近场语音优先显示（排前面），对讲机排后面；超出上限时优先保留近场
        entries.sort(Comparator.comparing((Entry e) -> e.radio ? 1 : 0).thenComparing(e -> e.name));
        int max = Config.HUD_MAX_ENTRIES.get();
        if (entries.size() > max) {
            entries = new ArrayList<>(entries.subList(0, max));
        }

        boolean left = "left".equalsIgnoreCase(Config.HUD_POSITION.get());
        int offsetX = Config.HUD_OFFSET_X.get();
        double scale = Config.HUD_SCALE.get();
        int baseColor = Config.HUD_COLOR.get();
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        double minAlpha = Config.HUD_MIN_ALPHA.get();

        int scaledW = mc.getWindow().getGuiScaledWidth();
        double anchorX = left ? offsetX : scaledW - offsetX;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        if (scale != 1.0D) {
            pose.scale((float) scale, (float) scale, 1.0F);
        }

        Component radioLabel = Component.translatable("ccnrcom.hud.radio");
        int y = (int) (Config.HUD_OFFSET_Y.get() / scale);
        for (Entry e : entries) {
            float v = Math.max(0.0F, Math.min(1.0F, e.volume));
            // 音量电平 → 饱和度/亮度：背景从暗到亮
            float dim = e.radio ? 0.75F : 1.0F;
            int rr = (int) (r * (0.2F + 0.8F * v) * dim);
            int gg = (int) (g * (0.2F + 0.8F * v) * dim);
            int bb = (int) (b * (0.2F + 0.8F * v) * dim);
            int alpha = (int) (255.0 * (minAlpha + (1.0 - minAlpha) * v));
            int color = (alpha << 24) | (rr << 16) | (gg << 8) | bb;

            // 对讲机条目：名字后加标签（近场没有）
            String text = e.radio ? e.name + " " + radioLabel.getString() : e.name;
            int w = mc.font.width(text) + 12;
            int h = 14;
            int drawX = (int) (anchorX / scale) - (left ? 0 : w);
            RoundRectRenderer.fill(guiGraphics, drawX, y, drawX + w, y + h, CommsTheme.RADIUS_MEDIUM, color);
            guiGraphics.drawString(mc.font, text, drawX + 6, y + 3, e.radio ? 0xFFE8E8E8 : 0xFFFFFFFF);
            y += h + 2;
        }

        pose.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Entry {
        final String name;
        final float volume;
        final boolean radio;

        Entry(String name, float volume, boolean radio) {
            this.name = name;
            this.volume = volume;
            this.radio = radio;
        }
    }
}
