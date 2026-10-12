package com.magicbanana666.eplus;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EPlusConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue BASE_COST;
    public static final ModConfigSpec.DoubleValue COST_FACTOR;
    public static final ModConfigSpec.DoubleValue TREASURE_FACTOR;
    public static final ModConfigSpec.DoubleValue CURSE_FACTOR;
    public static final ModConfigSpec.DoubleValue FLOATING_BOOK_POWER;
    public static final ModConfigSpec.BooleanValue ALLOW_OTHER_ENCHANTMENTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ENCHANTMENTS;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        BASE_COST = builder.comment("The base cost (in experience points) to use for the enchantment formula.").defineInRange("baseCost", 45, 1, 1024);
        COST_FACTOR = builder.comment("A number used when calculating enchantment cost. This number is treated as a % based factor. 0.30 = 30% of the original cost. 1.5 = 150% of the original cost.").defineInRange("costFactor", 1D, 0D, 1024D);
        TREASURE_FACTOR = builder.comment("A factor used to make treasure enchantments like mending cost more to apply. By default they cost 4X more.").defineInRange("treasureFactor", 4D, 0D, 1024D);
        CURSE_FACTOR = builder.comment("A factor used to make curse enchantments like vanishing cost more to apply (and to remove). By default they cost 3X more.").defineInRange("curseFactor", 3D, 0D, 1024D);
        FLOATING_BOOK_POWER = builder.comment("The amount of enchantment power a floating (decorative) book gives. Bookshelves have 1 power.").defineInRange("floatingBookPower", 1D, 0D, 1024D);
        ALLOW_OTHER_ENCHANTMENTS = builder.comment("Also offer enchantments that are not meant for the item (EG Protection on a sword), listed under 'Other enchantments'.").define("allowOtherEnchantments", true);
        builder.pop();
        builder.push("blacklist");
        BLACKLISTED_ITEMS = builder.comment("Items that can't be enchanted with this mod, EG minecraft:diamond_sword. The item tag eplus:blacklisted works too.").defineListAllowEmpty("blacklistedItems", List.of(), () -> "", o -> o instanceof String);
        BLACKLISTED_ENCHANTMENTS = builder.comment("Enchantments that are not available in the advanced table, EG minecraft:mending. The enchantment tag eplus:blacklisted works too.").defineListAllowEmpty("blacklistedEnchantments", List.of(), () -> "", o -> o instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private EPlusConfig() {
    }

    public static int baseCost() {
        return SPEC.isLoaded() ? BASE_COST.get() : 45;
    }

    public static float costFactor() {
        return SPEC.isLoaded() ? COST_FACTOR.get().floatValue() : 1f;
    }

    public static float treasureFactor() {
        return SPEC.isLoaded() ? TREASURE_FACTOR.get().floatValue() : 4f;
    }

    public static float curseFactor() {
        return SPEC.isLoaded() ? CURSE_FACTOR.get().floatValue() : 3f;
    }

    public static float floatingBookPower() {
        return SPEC.isLoaded() ? FLOATING_BOOK_POWER.get().floatValue() : 1f;
    }

    public static boolean allowOtherEnchantments() {
        return !SPEC.isLoaded() || ALLOW_OTHER_ENCHANTMENTS.get();
    }

    public static List<? extends String> blacklistedItems() {
        return SPEC.isLoaded() ? BLACKLISTED_ITEMS.get() : List.of();
    }

    public static List<? extends String> blacklistedEnchantments() {
        return SPEC.isLoaded() ? BLACKLISTED_ENCHANTMENTS.get() : List.of();
    }
}
