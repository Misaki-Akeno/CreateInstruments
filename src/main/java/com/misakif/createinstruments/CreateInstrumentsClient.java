package com.misakif.createinstruments;

import com.misakif.createinstruments.client.DashboardBlockEntityRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateInstruments.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateInstruments.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateInstrumentsClient {

    public CreateInstrumentsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateInstruments.DASHBOARD_BE.get(),
                DashboardBlockEntityRenderer::new);
    }
}
