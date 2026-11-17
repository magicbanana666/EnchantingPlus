package com.magicbanana666.eplus.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client settings, in config/eplus-client.toml. */
public final class EPlusClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENCHANT_ANIMATION;
    public static final ModConfigSpec.BooleanValue CLOSE_SCREEN_ON_ENCHANT;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("animation");
        ENCHANT_ANIMATION = builder.comment("Play an animation at the advanced table when an item is enchanted: the item floats over the table, rune circles light up, and it ends in a burst of light.").define("enchantAnimation", true);
        CLOSE_SCREEN_ON_ENCHANT = builder.comment("Close the table screen after you enchant, so you can watch the animation. The item stays in your slot of the table.").define("closeScreenOnEnchant", true);
        builder.pop();
        SPEC = builder.build();
    }

    private EPlusClientConfig() {
    }

    public static boolean enchantAnimation() {
        return !SPEC.isLoaded() || ENCHANT_ANIMATION.get();
    }

    public static boolean closeScreenOnEnchant() {
        return !SPEC.isLoaded() || CLOSE_SCREEN_ON_ENCHANT.get();
    }
}
