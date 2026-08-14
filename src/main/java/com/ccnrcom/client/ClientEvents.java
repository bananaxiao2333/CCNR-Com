package com.ccnrcom.client;

import com.ccnrcom.CCNRComMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** 客户端渲染事件（HUD 绘制；Plasmo Voice 存在时才启用语音 HUD） */
@Mod.EventBusSubscriber(modid = CCNRComMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        if (!ModList.get().isLoaded("plasmovoice")) return;
        ClientVoiceHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
