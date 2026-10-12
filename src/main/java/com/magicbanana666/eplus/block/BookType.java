package com.magicbanana666.eplus.block;

import net.minecraft.util.StringRepresentable;

/** The looks of the decorative (floating) books. Each one has its own block and book texture. */
public enum BookType implements StringRepresentable {

    EPLUS("eplus"),
    VANILLA("vanilla"),
    PRISMARINE("prismarine"),
    NETHER("nether"),
    TARTARITE("tartarite"),
    WHITE("white"),
    METAL("metal");

    private final String name;

    BookType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
