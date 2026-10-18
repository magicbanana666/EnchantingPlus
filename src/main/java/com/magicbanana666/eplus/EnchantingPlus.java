package com.magicbanana666.eplus;

import com.mojang.logging.LogUtils;
import com.magicbanana666.eplus.network.EnchantPayload;
import com.magicbanana666.eplus.network.SliderUpdatePayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * Enchanting Plus: pick exactly which enchantments go on an item at the Advanced Enchantment Table.
 */
@Mod(EnchantingPlus.MOD_ID)
public final class EnchantingPlus {

    public static final String MOD_ID = "eplus";
    public static final Logger LOG = LogUtils.getLogger();

    public EnchantingPlus(IEventBus modBus, ModContainer container) {
        EPlusRegistry.register(modBus);
        modBus.addListener(EnchantingPlus::registerPayloads);
        container.registerConfig(ModConfig.Type.SYNCED, EPlusConfig.SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SliderUpdatePayload.TYPE, SliderUpdatePayload.STREAM_CODEC, SliderUpdatePayload::handle);
        registrar.playToServer(EnchantPayload.TYPE, EnchantPayload.STREAM_CODEC, EnchantPayload::handle);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
