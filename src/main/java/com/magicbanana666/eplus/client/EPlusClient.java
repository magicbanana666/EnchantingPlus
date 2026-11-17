package com.magicbanana666.eplus.client;

import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.client.fx.EnchantFxRender;
import com.magicbanana666.eplus.client.fx.EnchantRitual;
import com.magicbanana666.eplus.network.EnchantFxPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EnchantingPlus.MOD_ID, dist = Dist.CLIENT)
public final class EPlusClient {

    public EPlusClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(EPlusClient::registerScreens);
        modBus.addListener(EPlusClient::registerRenderers);
        modBus.addListener(EPlusClient::registerPayloadHandlers);
        modBus.addListener(RegisterRenderPipelinesEvent.class, EnchantFxRender::registerPipelines);
        container.registerConfig(ModConfig.Type.CLIENT, EPlusClientConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(EnchantRitual::onClientTick);
        NeoForge.EVENT_BUS.addListener(EnchantRitual::onLevelUnload);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EPlusRegistry.ADVANCED_TABLE_MENU.get(), AdvancedTableScreen::new);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(EnchantFxPayload.TYPE, EnchantRitual::handle);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EPlusRegistry.ADVANCED_TABLE_BE.get(), BookRenderer::new);
    }
}
