/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom.gui.render;

import com.ccnrcom.CCNRComMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * SDF 圆角矩形渲染（移植自 E33Chat RoundRectRenderer，MIT）。
 * shader 加载失败时回退为普通矩形。
 */
@OnlyIn(Dist.CLIENT)
public class RoundRectRenderer {
    private static ShaderInstance shader;

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation(CCNRComMod.MODID, "rendertype_round_rect"),
                            DefaultVertexFormat.POSITION_COLOR),
                    s -> shader = s);
        } catch (Exception e) {
            LogUtils.getLogger()
                    .error("[CCNR-Com] round rect shader failed to load, falling back to square corners", e);
        }
    }

    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, float radius, int argb) {
        ShaderInstance sh = shader;
        radius = RoundRectParams.clampedRadius(radius, x2 - x1, y2 - y1);
        if (sh == null || radius <= 0) {
            g.fill(x1, y1, x2, y2, argb);
            return;
        }
        g.flush();

        Matrix4f pose = g.pose().last().pose();
        // u_Rect 必须与烘焙后的顶点同空间（顶点已乘 pose），SDF 矩形需跟随 pose 缩放
        float poseScale = Math.abs(pose.m00());
        Vector4f center = pose.transform(new Vector4f((x1 + x2) / 2f, (y1 + y2) / 2f, 0f, 1f));
        sh.safeGetUniform("u_Rect").set(center.x(), center.y(), (x2 - x1) / 2f * poseScale, (y2 - y1) / 2f * poseScale);
        sh.safeGetUniform("u_Radius").set(RoundRectParams.scaledRadius(radius, poseScale));

        float a = (argb >>> 24) / 255f;
        float r = (argb >> 16 & 0xFF) / 255f;
        float gr = (argb >> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> sh);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(pose, x1, y1, 0).color(r, gr, b, a).endVertex();
        bb.vertex(pose, x1, y2, 0).color(r, gr, b, a).endVertex();
        bb.vertex(pose, x2, y2, 0).color(r, gr, b, a).endVertex();
        bb.vertex(pose, x2, y1, 0).color(r, gr, b, a).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();
    }
}
