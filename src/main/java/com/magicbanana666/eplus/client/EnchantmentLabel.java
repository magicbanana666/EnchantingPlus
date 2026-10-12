package com.magicbanana666.eplus.client;

import com.magicbanana666.eplus.EnchLogic;
import com.magicbanana666.eplus.menu.EnchantmentLogicController;
import com.magicbanana666.eplus.network.SliderUpdatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** One row in the advanced table: the enchantment name with a slider to pick its level. */
public class EnchantmentLabel {

    public static final int HEIGHT = 18;
    public static final int WIDTH = 143;
    private static final int COLOR_BACKGROUND_LOCKED = 0x44d10841;
    private static final int COLOR_BACKGROUND_AVAILABLE = 0x445aaeae;
    private static final int COLOR_TEXT = 0xFFAAFF00;

    private final AdvancedTableScreen parent;
    private final EnchantmentLogicController logic;
    private final Holder<Enchantment> enchantment;
    /** Set for the row that only shows a category title. */
    private final Component header;

    private int x;
    private int y;
    private int sliderX;
    private boolean dragging = false;
    private boolean visible = true;

    public EnchantmentLabel(AdvancedTableScreen parent, EnchantmentLogicController logic, Holder<Enchantment> enchantment) {
        this.parent = parent;
        this.logic = logic;
        this.enchantment = enchantment;
        this.header = null;
    }

    /** A row showing a category title instead of an enchantment. */
    public EnchantmentLabel(AdvancedTableScreen parent, EnchantmentLogicController logic, Component header) {
        this.parent = parent;
        this.logic = logic;
        this.enchantment = null;
        this.header = header;
    }

    public boolean isHeader() {
        return this.header != null;
    }

    public void extract(GuiGraphicsExtractor graphics, Font font) {
        if (!this.visible) {
            return;
        }
        if (this.header != null) {
            graphics.text(font, this.header, this.x + (WIDTH - font.width(this.header)) / 2, this.y + 6, 0xFF404040, false);
            return;
        }
        final int maxLevel = this.enchantment.value().getMaxLevel();
        final int currentLevel = this.getCurrentLevel();
        final int indexX = this.dragging ? this.sliderX : currentLevel <= maxLevel ? (int) (this.x + 1 + (WIDTH - 6) * (currentLevel / (double) maxLevel)) : this.x + 1 + WIDTH - 6;
        graphics.fill(this.x + 1, this.y + 2, this.x + WIDTH, this.y + HEIGHT, this.isLocked() ? COLOR_BACKGROUND_LOCKED : COLOR_BACKGROUND_AVAILABLE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, AdvancedTableScreen.TEXTURE, indexX, this.y + 2, this.isSelected() ? 5 : 0, 197, 5, 16, 256, 256);
        graphics.text(font, this.getDisplayName(), this.x + 7, this.y + 6, COLOR_TEXT, true);
    }

    public boolean isSelected() {
        return this.parent.selected == this;
    }

    public int getCurrentLevel() {
        return this.logic.getCurrentLevel(this.enchantment);
    }

    /** The translated name of the enchantment. If the enchantment is of level 0, the level bit is cut off. */
    public Component getDisplayName() {
        final int level = this.getCurrentLevel();
        // Plain name + level numeral (getFullname would color it gray)
        final MutableComponent name = this.enchantment.value().description().copy();
        if (level > 0) {
            name.append(" ").append(Component.translatable("enchantment.level." + level));
        }
        if (EnchLogic.isCurse(this.enchantment)) {
            name.withStyle(ChatFormatting.RED);
        }
        return name;
    }

    /** Moves the slider to a position relative to the start of the label, changing the level of the enchantment. */
    public void updateSlider(int relX) {
        if (this.isLocked()) {
            return;
        }
        final int min = this.x + 1;
        final int max = min + WIDTH - 6;
        // Updates the position of the slider. -2 to center the cursor on the mouse position.
        this.sliderX = Math.max(min, Math.min(max, min + relX - 2));
        final float index = relX / (float) (WIDTH - 10);
        final int maxLevel = this.enchantment.value().getMaxLevel();
        final int initialLevel = this.logic.getInitialLevel(this.enchantment);
        final int updatedLevel = Math.max(0, Math.min(maxLevel, Math.round(maxLevel * index)));
        // Damaged items can't lose enchantments.
        if (updatedLevel < initialLevel && this.logic.getInputStack().isDamaged()) {
            return;
        }
        if (this.logic.requestLevel(this.enchantment, updatedLevel)) {
            ClientPacketDistributor.sendToServer(new SliderUpdatePayload(this.enchantment, updatedLevel));
        }
    }

    public boolean isLocked() {
        return this.header != null || this.logic.isLocked(this.enchantment);
    }

    public Component getDescription() {
        final Identifier id = this.enchantment.unwrapKey().map(key -> key.identifier()).orElse(Identifier.withDefaultNamespace("unknown"));
        final String key = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        final String modName = ModList.get().getModContainerById(id.getNamespace()).map(c -> c.getModInfo().getDisplayName()).orElse(id.getNamespace());
        return Component.translatable("tooltip.eplus.missing", modName, key);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && this.header == null && this.x <= mouseX && this.x + WIDTH >= mouseX && this.y <= mouseY && this.y + HEIGHT >= mouseY;
    }

    public Holder<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
