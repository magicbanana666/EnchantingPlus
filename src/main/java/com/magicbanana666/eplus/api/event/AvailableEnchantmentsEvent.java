package com.magicbanana666.eplus.api.event;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.Event;

/**
 * Fired on {@code NeoForge.EVENT_BUS}, on both the client and server, to determine the enchantments that
 * can be applied to an item at the advanced table.
 */
public class AvailableEnchantmentsEvent extends Event {

    private final ItemStack stack;
    private List<Holder<Enchantment>> enchantments;

    public AvailableEnchantmentsEvent(ItemStack stack, List<Holder<Enchantment>> enchantments) {
        this.stack = stack;
        this.enchantments = enchantments;
    }

    public List<Holder<Enchantment>> getEnchantments() {
        return this.enchantments;
    }

    public void setEnchantments(List<Holder<Enchantment>> enchantments) {
        this.enchantments = enchantments;
    }

    public ItemStack getStack() {
        return this.stack;
    }
}
