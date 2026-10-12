package com.magicbanana666.eplus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.magicbanana666.eplus.api.event.AvailableEnchantmentsEvent;
import com.magicbanana666.eplus.api.event.EnchantmentCostEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.common.NeoForge;

public final class EnchLogic {

    /** Items in this tag can't be enchanted at the advanced table. */
    public static final TagKey<Item> BLACKLISTED_ITEMS = TagKey.create(Registries.ITEM, EnchantingPlus.id("blacklisted"));
    /** Enchantments in this tag are never offered by the advanced table. */
    public static final TagKey<Enchantment> BLACKLISTED_ENCHANTMENTS = TagKey.create(Registries.ENCHANTMENT, EnchantingPlus.id("blacklisted"));

    private EnchLogic() {
    }

    public static boolean isItemBlacklisted(ItemStack stack) {
        return stack.is(BLACKLISTED_ITEMS) || EPlusConfig.blacklistedItems().contains(stack.typeHolder().getRegisteredName());
    }

    public static boolean isEnchantmentBlacklisted(Holder<Enchantment> enchantment) {
        return enchantment.is(BLACKLISTED_ENCHANTMENTS) || EPlusConfig.blacklistedEnchantments().contains(enchantment.getRegisteredName());
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /** Whether the item can go in the table at all. */
    public static boolean canEnchant(ItemStack stack) {
        return !stack.isEmpty() && !isItemBlacklisted(stack) && (stack.isEnchantable() || stack.isEnchanted() || isBook(stack));
    }

    public static boolean isCurse(Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.CURSE);
    }

    public static boolean isTreasure(Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.TREASURE) || isCurse(enchantment);
    }

    public static int calculateNewEnchCost(Holder<Enchantment> enchantment, int level) {
        // Base cost is equal to roughly 2.5 levels of EXP.
        float cost = EPlusConfig.baseCost();
        // Cost is multiplied up to 10, based on rarity of the enchant. Rarer the enchant, higher the cost.
        cost *= Math.max(11 - enchantment.value().getWeight(), 1);
        // Linear cost increase based on level.
        cost *= level;
        // The cost factor is applied.
        cost *= EPlusConfig.costFactor();
        // Curses cost even more to apply
        if (isCurse(enchantment)) {
            cost *= EPlusConfig.curseFactor();
        }
        // Treasures cost more to apply
        else if (isTreasure(enchantment)) {
            cost *= EPlusConfig.treasureFactor();
        }
        final EnchantmentCostEvent event = NeoForge.EVENT_BUS.post(new EnchantmentCostEvent((int) cost, enchantment, level));
        return event.getCost();
    }

    public static List<Holder<Enchantment>> getValidEnchantments(ItemStack stack, Level level, BlockPos pos) {
        List<Holder<Enchantment>> enchList = new ArrayList<>();
        if (canEnchant(stack)) {
            final boolean allowOther = EPlusConfig.allowOtherEnchantments();
            for (final Holder<Enchantment> enchantment : level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList()) {
                if (isEnchantmentBlacklisted(enchantment) || !(allowOther || isIntendedFor(stack, enchantment))) {
                    continue;
                }
                if (!isTreasure(enchantment) || isCurseAvailable(level, pos, enchantment) || isTreasuresAvailable(enchantment, level, pos)) {
                    enchList.add(enchantment);
                }
            }
            // Enchantments meant for the item first, then the rest (each part sorted by id).
            enchList.sort(Comparator.<Holder<Enchantment>, Boolean>comparing(e -> !isIntendedFor(stack, e)).thenComparing(Holder::getRegisteredName));
            enchList = NeoForge.EVENT_BUS.post(new AvailableEnchantmentsEvent(stack, enchList)).getEnchantments();
        }
        return enchList;
    }

    /** Whether the enchantment is meant for this item (books take every enchantment). */
    public static boolean isIntendedFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return isBook(stack) || stack.supportsEnchantment(enchantment);
    }

    public static boolean isCurseAvailable(Level level, BlockPos pos, Holder<Enchantment> enchantment) {
        return isCurse(enchantment) && isWickedNight(level, pos);
    }

    /** Treasure enchantments need daylight and a 3x3 floor of beacon base blocks under the table. */
    public static boolean isTreasuresAvailable(Holder<Enchantment> enchantment, Level level, BlockPos pos) {
        if (isCurse(enchantment) || !isTreasure(enchantment)) {
            return false;
        }
        return isTreasureSetup(level, pos);
    }

    public static boolean isTreasureSetup(Level level, BlockPos pos) {
        if (!level.isBrightOutside()) {
            return false;
        }
        final BlockPos down = pos.below();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.getBlockState(down.offset(x, 0, z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Curses can only be added or removed around midnight on a full moon. */
    public static boolean isWickedNight(Level level, BlockPos pos) {
        final float skyAngle = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos) / 360f;
        final boolean isNightRange = skyAngle > 0.40f && skyAngle < 0.60f;
        return isNightRange && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON;
    }

    /** Sum of the enchanting power of the bookshelves around the table. */
    public static float getEnchantingPower(Level level, BlockPos pos) {
        float power = 0;
        for (final BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                final BlockPos shelf = pos.offset(offset);
                power += level.getBlockState(shelf).getEnchantPowerBonus(level, shelf);
            }
        }
        return power;
    }

    public static Identifier descriptionKeyId(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(k -> k.identifier()).orElse(Identifier.withDefaultNamespace("unknown"));
    }

    // The following code was adapted from OpenModsLib https://github.com/OpenMods/OpenModsLib

    public static int getExperience(Player player) {
        return (int) (getExperienceForLevels(player.experienceLevel) + player.experienceProgress * player.getXpNeededForNextLevel());
    }

    public static void removeExperience(Player player, int amount) {
        addExperience(player, -amount);
    }

    public static void addExperience(Player player, int amount) {
        final int experience = Math.max(0, getExperience(player) + amount);
        player.totalExperience = experience;
        player.experienceLevel = getLevelForExperience(experience);
        final int expForLevel = getExperienceForLevels(player.experienceLevel);
        player.experienceProgress = (float) (experience - expForLevel) / (float) player.getXpNeededForNextLevel();
    }

    public static int getExperienceForLevels(int level) {
        if (level == 0) {
            return 0;
        }
        if (level > 0 && level < 17) {
            return level * level + 6 * level;
        }
        else if (level > 16 && level < 32) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int getLevelForExperience(int experience) {
        int level = 0;
        while (getExperienceForLevels(level) <= experience) {
            level++;
        }
        return level - 1;
    }
}
