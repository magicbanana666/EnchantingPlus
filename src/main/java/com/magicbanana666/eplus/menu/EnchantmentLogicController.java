package com.magicbanana666.eplus.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.magicbanana666.eplus.EnchLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * The enchanting state of one player at an advanced table. Exists on both sides: the client updates it
 * while the sliders move, the server mirrors it through {@link com.magicbanana666.eplus.network.SliderUpdatePayload}
 * and validates everything again before enchanting.
 */
public class EnchantmentLogicController {

    private final Player player;
    private final Level level;
    private final BlockPos pos;
    private final Container inventory;

    private ItemStack inputStack = ItemStack.EMPTY;
    // All valid enchantments for the current item.
    private List<Holder<Enchantment>> validEnchantments = new ArrayList<>();
    // The original enchantments.
    private Map<Holder<Enchantment>, Integer> initialEnchantments = new LinkedHashMap<>();
    // The new enchantments that will go on the item.
    private Map<Holder<Enchantment>, Integer> itemEnchantments = new LinkedHashMap<>();
    private float enchantmentPower;
    private int cost;

    public EnchantmentLogicController(Player player, Level level, BlockPos pos, Container inventory) {
        this.player = player;
        this.level = level;
        this.pos = pos;
        this.inventory = inventory;
        this.onItemUpdated();
    }

    public void onItemUpdated() {
        this.inputStack = this.inventory.getItem(0);
        this.initialEnchantments = new LinkedHashMap<>();
        for (final var entry : EnchantmentHelper.getEnchantmentsForCrafting(this.inputStack).entrySet()) {
            this.initialEnchantments.put(entry.getKey(), entry.getIntValue());
        }
        this.itemEnchantments = new LinkedHashMap<>(this.initialEnchantments);
        this.validEnchantments = EnchLogic.getValidEnchantments(this.inputStack, this.level, this.pos);
        this.calculateState();
    }

    public void calculateState() {
        this.enchantmentPower = EnchLogic.getEnchantingPower(this.level, this.pos);
        float newCost = 0;
        // Calculate cost of new enchantments
        for (final Map.Entry<Holder<Enchantment>, Integer> newEntry : this.itemEnchantments.entrySet()) {
            final int original = this.initialEnchantments.getOrDefault(newEntry.getKey(), 0);
            final int newLevels = newEntry.getValue() - original;
            // Removing levels gives no experience back.
            if (newLevels > 0) {
                newCost += EnchLogic.calculateNewEnchCost(newEntry.getKey(), newLevels);
            }
        }
        // Calculate cost of removed curses
        for (final Map.Entry<Holder<Enchantment>, Integer> existing : this.initialEnchantments.entrySet()) {
            if (EnchLogic.isCurse(existing.getKey()) && existing.getValue() > 0) {
                final int currentCurseLevel = this.itemEnchantments.getOrDefault(existing.getKey(), 0);
                if (currentCurseLevel < existing.getValue()) {
                    newCost += EnchLogic.calculateNewEnchCost(existing.getKey(), existing.getValue() - currentCurseLevel);
                }
            }
        }
        // Apply bookshelf discount
        if (this.getEnchantmentPower() > 0) {
            newCost -= newCost * this.getEnchantmentPower() / 100f;
        }
        this.cost = Math.max(0, Math.round(newCost));
    }

    public int getCurrentLevel(Holder<Enchantment> enchant) {
        return this.itemEnchantments.getOrDefault(enchant, 0);
    }

    public int getInitialLevel(Holder<Enchantment> enchant) {
        return this.initialEnchantments.getOrDefault(enchant, 0);
    }

    /** Whether the enchantment can't be changed right now (it conflicts with another one, or is above its max level). */
    public boolean isLocked(Holder<Enchantment> enchantment) {
        if (this.getCurrentLevel(enchantment) > enchantment.value().getMaxLevel()) {
            return true;
        }
        for (final Map.Entry<Holder<Enchantment>, Integer> other : this.itemEnchantments.entrySet()) {
            if (!other.getKey().equals(enchantment) && other.getValue() > 0 && !Enchantment.areCompatible(other.getKey(), enchantment)) {
                return true;
            }
        }
        return false;
    }

    /** Changes the level of an enchantment if that is allowed. Returns whether it changed. */
    public boolean requestLevel(Holder<Enchantment> enchantment, int level) {
        if (!this.isValidEnchantment(enchantment) || this.isLocked(enchantment)) {
            return false;
        }
        final int initial = this.getInitialLevel(enchantment);
        level = Math.max(0, Math.min(level, Math.max(initial, enchantment.value().getMaxLevel())));
        // Damaged items can't have enchantments taken off.
        if (level < initial && this.inputStack.isDamaged()) {
            level = initial;
        }
        if (level == this.getCurrentLevel(enchantment)) {
            return false;
        }
        this.updateEnchantment(enchantment, level);
        return true;
    }

    public void updateEnchantment(Holder<Enchantment> enchantment, int level) {
        // If the level is set to below 1, remove it from the item.
        if (level < 1) {
            this.itemEnchantments.remove(enchantment);
        }
        // If the enchantment is valid for item, update it.
        else if (this.validEnchantments.contains(enchantment)) {
            this.itemEnchantments.put(enchantment, level);
        }
        this.calculateState();
    }

    public boolean isValidEnchantment(Holder<Enchantment> enchantment) {
        return this.validEnchantments.contains(enchantment);
    }

    public List<Holder<Enchantment>> getValidEnchantments() {
        return this.validEnchantments;
    }

    public Map<Holder<Enchantment>, Integer> getInitialEnchantments() {
        return this.initialEnchantments;
    }

    public Map<Holder<Enchantment>, Integer> getCurrentEnchantments() {
        return this.itemEnchantments;
    }

    public boolean hasChanges() {
        return !this.inputStack.isEmpty() && !this.itemEnchantments.equals(this.initialEnchantments);
    }

    public boolean canAfford() {
        return this.player.hasInfiniteMaterials() || EnchLogic.getExperience(this.player) >= this.cost;
    }

    /** Server side: applies the chosen enchantments and takes the experience. */
    public void enchantItem() {
        if (!this.hasChanges() || !this.canAfford()) {
            return;
        }
        if (!this.player.hasInfiniteMaterials() && this.cost > 0) {
            EnchLogic.removeExperience(this.player, this.cost);
            if (this.player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetExperiencePacket(serverPlayer.experienceProgress, serverPlayer.totalExperience, serverPlayer.experienceLevel));
            }
        }
        final ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (final Map.Entry<Holder<Enchantment>, Integer> entry : this.itemEnchantments.entrySet()) {
            if (entry.getValue() > 0) {
                enchantments.set(entry.getKey(), entry.getValue());
            }
        }
        final ItemEnchantments result = enchantments.toImmutable();
        ItemStack stack = this.inputStack.copy();
        if (stack.is(Items.BOOK) && !result.isEmpty()) {
            stack = stack.transmuteCopy(Items.ENCHANTED_BOOK);
            stack.remove(DataComponents.ENCHANTMENTS);
        }
        else if (stack.is(Items.ENCHANTED_BOOK) && result.isEmpty()) {
            stack = stack.transmuteCopy(Items.BOOK);
            stack.remove(DataComponents.STORED_ENCHANTMENTS);
        }
        EnchantmentHelper.setEnchantments(stack, result);
        this.inventory.setItem(0, stack);
        this.onItemUpdated();
    }

    public int getCost() {
        return this.cost;
    }

    public float getEnchantmentPower() {
        return Math.min(this.enchantmentPower, 30f);
    }

    public ItemStack getInputStack() {
        return this.inputStack;
    }

    public Container getInventory() {
        return this.inventory;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Level getLevel() {
        return this.level;
    }

    public Player getPlayer() {
        return this.player;
    }
}
