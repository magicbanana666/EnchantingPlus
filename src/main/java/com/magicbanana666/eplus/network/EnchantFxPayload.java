package com.magicbanana666.eplus.network;

import com.magicbanana666.eplus.EnchantingPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: a player just enchanted an item at the table at {@code pos}, play the enchanting animation.
 * The handler is registered on the client only (see EPlusClient).
 */
public record EnchantFxPayload(BlockPos pos, ItemStack item, int playerId) implements CustomPacketPayload {

    public static final Type<EnchantFxPayload> TYPE = new Type<>(EnchantingPlus.id("enchant_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantFxPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EnchantFxPayload::pos,
            ItemStack.OPTIONAL_STREAM_CODEC, EnchantFxPayload::item,
            ByteBufCodecs.VAR_INT, EnchantFxPayload::playerId,
            EnchantFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
