package com.magicbanana666.eplus.api.event;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.Event;

/**
 * Fired on {@code NeoForge.EVENT_BUS}, on the client and the server, when the experience cost of adding
 * levels of an enchantment at the advanced table is calculated.
 */
public class EnchantmentCostEvent extends Event {

    private final Holder<Enchantment> enchantment;
    private final int level;
    private final int originalCost;
    private int cost;

    public EnchantmentCostEvent(int cost, Holder<Enchantment> enchantment, int level) {
        this.cost = cost;
        this.originalCost = cost;
        this.enchantment = enchantment;
        this.level = level;
    }

    public Holder<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public int getLevel() {
        return this.level;
    }

    public int getCost() {
        return this.cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getOriginalCost() {
        return this.originalCost;
    }
}
