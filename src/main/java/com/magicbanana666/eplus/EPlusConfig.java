package com.magicbanana666.eplus;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EPlusConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue BASE_COST;
    public static final ModConfigSpec.DoubleValue COST_FACTOR;
    public static final ModConfigSpec.DoubleValue TREASURE_FACTOR;
    public static final ModConfigSpec.DoubleValue CURSE_FACTOR;
    public static final ModConfigSpec.BooleanValue ALLOW_OTHER_ENCHANTMENTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ENCHANTMENTS;
    public static final ModConfigSpec.DoubleValue LUCKY_MINING_TIER_1;
    public static final ModConfigSpec.DoubleValue LUCKY_MINING_TIER_2;
    public static final ModConfigSpec.DoubleValue LUCKY_MINING_TIER_3;
    public static final ModConfigSpec.IntValue FLOOR_BONUS_LEVELS;
    public static final ModConfigSpec.IntValue EMPOWERED_MAX_LEVEL;
    public static final ModConfigSpec.BooleanValue FLIM_FLAM_SAFE_ONLY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLIM_FLAM_BLACKLIST;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        BASE_COST = builder.comment("The base cost (in experience points) to use for the enchantment formula.").defineInRange("baseCost", 45, 1, 1024);
        COST_FACTOR = builder.comment("A number used when calculating enchantment cost. This number is treated as a % based factor. 0.30 = 30% of the original cost. 1.5 = 150% of the original cost.").defineInRange("costFactor", 1D, 0D, 1024D);
        TREASURE_FACTOR = builder.comment("A factor used to make treasure enchantments like mending cost more to apply. By default they cost 4X more.").defineInRange("treasureFactor", 4D, 0D, 1024D);
        CURSE_FACTOR = builder.comment("A factor used to make curse enchantments like vanishing cost more to apply (and to remove). By default they cost 3X more.").defineInRange("curseFactor", 3D, 0D, 1024D);
        ALLOW_OTHER_ENCHANTMENTS = builder.comment("Also offer enchantments that are not meant for the item (EG Protection on a sword), listed under 'Other enchantments'.").define("allowOtherEnchantments", true);
        builder.pop();
        builder.push("blacklist");
        BLACKLISTED_ITEMS = builder.comment("Items that can't be enchanted with this mod, EG minecraft:diamond_sword. The item tag eplus:blacklisted works too.").defineListAllowEmpty("blacklistedItems", List.of(), () -> "", o -> o instanceof String);
        BLACKLISTED_ENCHANTMENTS = builder.comment("Enchantments that are not available in the advanced table, EG minecraft:mending. The enchantment tag eplus:blacklisted works too.").defineListAllowEmpty("blacklistedEnchantments", List.of(), () -> "", o -> o instanceof String);
        builder.pop();
        builder.push("levels");
        FLOOR_BONUS_LEVELS = builder.comment("Extra levels every enchantment can get when the table stands on a 3x3 floor of beacon base blocks (iron, gold, emerald, diamond, netherite...). EG 2: Sharpness VII, Mending III. Curses don't get extra levels.").defineInRange("floorBonusLevels", 2, 0, 254);
        EMPOWERED_MAX_LEVEL = builder.comment("The level every enchantment can go up to when the table stands on a 3x3 floor of netherite blocks and was empowered by right clicking it with an enchanted golden apple.").defineInRange("empoweredMaxLevel", 10, 1, 255);
        builder.pop();
        builder.push("luckyMining");
        LUCKY_MINING_TIER_1 = builder.comment("Chance (0-1) that mining an ore with Lucky Mining I spreads it into a stone block next to it. Mining the same ore in a row raises the chance up to twice this value.").defineInRange("tier1Chance", 0.10D, 0D, 1D);
        LUCKY_MINING_TIER_2 = builder.comment("Same as tier1Chance, for Lucky Mining II.").defineInRange("tier2Chance", 0.15D, 0D, 1D);
        LUCKY_MINING_TIER_3 = builder.comment("Same as tier1Chance, for Lucky Mining III and higher.").defineInRange("tier3Chance", 0.20D, 0D, 1D);
        builder.pop();
        builder.push("flimFlam");
        FLIM_FLAM_SAFE_ONLY = builder.comment("Only allow Flim Flam pranks that don't kill (or at least very rarely).").define("safeOnly", false);
        FLIM_FLAM_BLACKLIST = builder.comment("Flim Flam pranks that never happen. Names: inventory-shuffle, useless-tool, bane, epic-lore, living-rename, squid, sheep-dye, invisible-mobs, sound, snowballs, teleport, mount, encase, creepers, disarm, effect, skyblock").defineListAllowEmpty("blacklist", List.of(), () -> "", o -> o instanceof String);
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

    public static boolean allowOtherEnchantments() {
        return !SPEC.isLoaded() || ALLOW_OTHER_ENCHANTMENTS.get();
    }

    public static List<? extends String> blacklistedItems() {
        return SPEC.isLoaded() ? BLACKLISTED_ITEMS.get() : List.of();
    }

    public static List<? extends String> blacklistedEnchantments() {
        return SPEC.isLoaded() ? BLACKLISTED_ENCHANTMENTS.get() : List.of();
    }

    public static int floorBonusLevels() {
        return SPEC.isLoaded() ? FLOOR_BONUS_LEVELS.get() : 2;
    }

    public static int empoweredMaxLevel() {
        return SPEC.isLoaded() ? EMPOWERED_MAX_LEVEL.get() : 10;
    }

    public static boolean flimFlamSafeOnly() {
        return SPEC.isLoaded() && FLIM_FLAM_SAFE_ONLY.get();
    }

    public static List<? extends String> flimFlamBlacklist() {
        return SPEC.isLoaded() ? FLIM_FLAM_BLACKLIST.get() : List.of();
    }

    /** Base chance of Lucky Mining to spread an ore, for the given enchantment level. */
    public static double luckyMiningChance(int level) {
        return switch (Math.clamp(level, 1, 3)) {
            case 1 -> SPEC.isLoaded() ? LUCKY_MINING_TIER_1.get() : 0.10D;
            case 2 -> SPEC.isLoaded() ? LUCKY_MINING_TIER_2.get() : 0.15D;
            default -> SPEC.isLoaded() ? LUCKY_MINING_TIER_3.get() : 0.20D;
        };
    }
}
