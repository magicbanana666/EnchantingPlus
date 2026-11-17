package com.magicbanana666.eplus.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import com.magicbanana666.eplus.EnchLogic;
import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import com.magicbanana666.eplus.menu.EnchantmentLogicController;
import com.magicbanana666.eplus.network.EnchantPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class AdvancedTableScreen extends AbstractContainerScreen<AdvancedTableMenu> {

    public static final Identifier TEXTURE = EnchantingPlus.id("textures/gui/enchant.png");
    private static final String[] TIPS = { "description", "books", "treasure", "curse", "storage", "inventory", "armor" };
    private static final int VISIBLE_ROWS = 4;
    private static final int INFO_WIDTH = 120;

    private final EnchantmentLogicController logic;
    private final List<EnchantmentLabel> labels = new ArrayList<>();
    private List<Holder<Enchantment>> shownEnchantments = List.of();
    private final String tip = TIPS[RandomSource.create().nextInt(TIPS.length)];

    public EnchantmentLabel selected;
    private int listOffset = 0;
    private boolean isSliding = false;
    private int sliderY = 1;
    private ItemButton enchantButton;
    private boolean showingCurseIcon;

    public AdvancedTableScreen(AdvancedTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 235, 182);
        this.logic = menu.logic;
        this.titleLabelX = 32;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        this.isSliding = false;
        this.showingCurseIcon = EnchLogic.isWickedNight(this.logic.getLevel(), this.logic.getPos());
        final ItemStack icon = enchantButtonIcon(this.showingCurseIcon);
        this.enchantButton = this.addRenderableWidget(new ItemButton(this.leftPos + 35, this.topPos + 38, icon, button -> this.onEnchant()));
        this.updateLabels();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        final boolean cursesAvailable = EnchLogic.isWickedNight(this.logic.getLevel(), this.logic.getPos());
        if (cursesAvailable != this.showingCurseIcon) {
            this.showingCurseIcon = cursesAvailable;
            this.enchantButton.icon = enchantButtonIcon(cursesAvailable);
        }
        this.updateLabels();
    }

    private static ItemStack enchantButtonIcon(boolean cursesAvailable) {
        final ItemStack icon = new ItemStack(cursesAvailable ? Items.BONE : Items.ENCHANTED_BOOK);
        if (cursesAvailable) {
            icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return icon;
    }

    private void onEnchant() {
        if (this.logic.hasChanges() && this.logic.canAfford()) {
            ClientPacketDistributor.sendToServer(EnchantPayload.INSTANCE);
        }
    }

    /** Rebuilds the rows when the valid enchantments change, then places the visible ones. */
    public void updateLabels() {
        final List<Holder<Enchantment>> valid = this.logic.getValidEnchantments();
        if (!valid.equals(this.shownEnchantments)) {
            this.shownEnchantments = List.copyOf(valid);
            this.labels.clear();
            this.selected = null;
            boolean otherHeader = false;
            for (final Holder<Enchantment> enchantment : valid) {
                // Enchantments that are not meant for the item come last, under their own title.
                if (!otherHeader && !EnchLogic.isIntendedFor(this.logic.getInputStack(), enchantment)) {
                    this.labels.add(new EnchantmentLabel(this, this.logic, Component.translatable("gui.eplusplus.other_enchantments")));
                    otherHeader = true;
                }
                this.labels.add(new EnchantmentLabel(this, this.logic, enchantment));
            }
        }
        this.listOffset = Math.max(0, Math.min(this.listOffset, this.labels.size() - VISIBLE_ROWS));
        for (int i = 0; i < this.labels.size(); i++) {
            final EnchantmentLabel label = this.labels.get(i);
            final int row = i - this.listOffset;
            label.setPos(this.leftPos + 61, this.topPos + 15 + row * 18);
            label.setVisible(row >= 0 && row < VISIBLE_ROWS);
        }
        final int maxOffset = this.labels.size() - VISIBLE_ROWS;
        if (!this.isSliding) {
            this.sliderY = maxOffset > 0 ? 1 + 55 * this.listOffset / maxOffset : 1;
        }
        this.enchantButton.active = this.logic.hasChanges();
    }

    private EnchantmentLabel getLabelUnderMouse(double mouseX, double mouseY) {
        for (final EnchantmentLabel label : this.labels) {
            if (label.isMouseOver(mouseX, mouseY)) {
                return label;
            }
        }
        return null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        for (final EnchantmentLabel label : this.labels) {
            label.extract(graphics, this.font);
        }
        // Scroll bar
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 206, this.topPos + 16 + this.sliderY, this.isSliding || this.labels.size() <= VISIBLE_ROWS ? 12 : 0, 182, 12, 15, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractInfoBox(graphics);
        if (this.enchantButton.isMouseOver(mouseX, mouseY)) {
            final String key = !this.logic.canAfford() ? "gui.eplusplus.tooltip.tooexpensive" : !this.logic.hasChanges() ? "gui.eplusplus.tooltip.nochange" : "gui.eplusplus.tooltip.enchant";
            graphics.setTooltipForNextFrame(Component.translatable(key), mouseX, mouseY);
        }
        else if (this.isSneakDown()) {
            final EnchantmentLabel label = this.getLabelUnderMouse(mouseX, mouseY);
            if (label != null) {
                graphics.setTooltipForNextFrame(this.font.split(label.getDescription(), this.width / 3), mouseX, mouseY);
            }
        }
    }

    private boolean isSneakDown() {
        return InputConstants.isKeyDown(this.minecraft.options.keyShift.getKey().getValue());
    }

    /** The box to the left of the table with the experience, cost and a tip. */
    private void extractInfoBox(GuiGraphicsExtractor graphics) {
        final List<Component> info = new ArrayList<>();
        if (this.logic.getInputStack().isEmpty()) {
            info.add(Component.translatable("gui.eplusplus.info.noitem"));
        }
        else if (this.shownEnchantments.isEmpty()) {
            info.add(Component.translatable("gui.eplusplus.info.noench"));
        }
        else {
            final boolean infinite = this.minecraft.player.hasInfiniteMaterials();
            info.add(infinite ? Component.translatable("eplusplus.info.infinity") : Component.translatable("eplusplus.info.playerxp", EnchLogic.getExperience(this.minecraft.player)));
            info.add(Component.translatable("eplusplus.info.costxp", this.logic.getCost()));
            info.add(Component.translatable("eplusplus.info.power", String.format(Locale.ROOT, "%.2f", this.logic.getEnchantmentPower())));
            if (!this.logic.canAfford()) {
                info.add(Component.empty());
                info.add(Component.translatable("gui.eplusplus.info.tooexpensive").withStyle(ChatFormatting.RED));
            }
        }
        info.add(Component.empty());
        info.add(Component.translatable("eplusplus.info.tip.prefix").withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("eplusplus.info.tip." + this.tip, this.minecraft.options.keyShift.getTranslatedKeyMessage()).withStyle(ChatFormatting.WHITE)));

        // Keep the box left of the table, also on small screens.
        final int maxWidth = Math.max(60, Math.min(INFO_WIDTH, this.leftPos - 12));
        final List<FormattedCharSequence> lines = new ArrayList<>();
        for (final Component line : info) {
            if (line.getString().isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
            }
            else {
                lines.addAll(this.font.split(line, maxWidth));
            }
        }
        int width = 0;
        for (final FormattedCharSequence line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        final int height = lines.size() * 10 - 2;
        final int x = Math.max(4, this.leftPos - width - 8);
        final int y = this.topPos + 27;
        graphics.nextStratum();
        TooltipRenderUtil.extractTooltipBackground(graphics, x, y, width, height, null);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(this.font, lines.get(i), x, y + i * 10, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        final EnchantmentLabel label = this.getLabelUnderMouse(event.x(), event.y());
        if (label != null && !label.isLocked()) {
            this.selected = label;
            label.setDragging(true);
            label.updateSlider((int) event.x() - (label.getX() + 1));
            return true;
        }
        if (this.labels.size() > VISIBLE_ROWS && event.x() > this.leftPos + 206 && event.x() < this.leftPos + 218
                && event.y() > this.topPos + 16 && event.y() < this.topPos + 87) {
            this.isSliding = true;
            this.dragScrollbar(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.selected != null) {
            if (event.x() < this.selected.getX() || event.x() > this.selected.getX() + EnchantmentLabel.WIDTH) {
                this.selected.setDragging(false);
                this.selected = null;
            }
            else {
                this.selected.updateSlider((int) event.x() - (this.selected.getX() + 1));
            }
            return true;
        }
        if (this.isSliding) {
            this.dragScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private void dragScrollbar(double mouseY) {
        this.sliderY = Math.max(1, Math.min(56, (int) mouseY - (this.topPos + 16) - 7));
        final int maxOffset = this.labels.size() - VISIBLE_ROWS;
        this.listOffset = Math.round((this.sliderY - 1) / 55f * maxOffset);
        this.updateLabels();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.selected != null) {
            this.selected.setDragging(false);
            this.selected = null;
        }
        if (this.isSliding) {
            this.isSliding = false;
            this.updateLabels();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.labels.size() > VISIBLE_ROWS && x >= this.leftPos + 61 && x <= this.leftPos + 218 && y >= this.topPos + 15 && y <= this.topPos + 87) {
            this.listOffset -= (int) Math.signum(scrollY);
            this.updateLabels();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    /** A button showing an item instead of text. */
    private static class ItemButton extends Button.Plain {

        private ItemStack icon;

        ItemButton(int x, int y, ItemStack icon, OnPress onPress) {
            super(x, y, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractContents(graphics, mouseX, mouseY, a);
            graphics.item(this.icon, this.getX() + 2, this.getY() + 2);
        }
    }
}
