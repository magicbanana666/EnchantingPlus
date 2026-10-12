package com.magicbanana666.eplus.network;

import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player moved the slider of an enchantment. */
public record SliderUpdatePayload(Holder<Enchantment> enchantment, int level) implements CustomPacketPayload {

    public static final Type<SliderUpdatePayload> TYPE = new Type<>(EnchantingPlus.id("slider_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT), SliderUpdatePayload::enchantment,
            ByteBufCodecs.VAR_INT, SliderUpdatePayload::level,
            SliderUpdatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SliderUpdatePayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof AdvancedTableMenu menu) {
            menu.logic.requestLevel(payload.enchantment, payload.level);
        }
    }
}
