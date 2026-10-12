package com.magicbanana666.eplus.network;

import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player pressed the enchant button. */
public record EnchantPayload() implements CustomPacketPayload {

    public static final EnchantPayload INSTANCE = new EnchantPayload();
    public static final Type<EnchantPayload> TYPE = new Type<>(EnchantingPlus.id("enchant"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof AdvancedTableMenu menu) {
            menu.logic.enchantItem();
        }
    }
}
