package com.magicbanana666.eplus.menu;

import java.util.Map;

import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.EnchLogic;
import com.magicbanana666.eplus.block.entity.AdvancedTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AdvancedTableMenu extends AbstractContainerMenu {

    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final Map<EquipmentSlot, Identifier> ARMOR_ICONS = Map.of(
            EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);

    // Slot index ranges
    private static final int INV_START = 1;
    private static final int INV_END = 37;
    private static final int ARMOR_END = 41;

    public final EnchantmentLogicController logic;
    private final Container tableInventory;

    /** Client side constructor. */
    public AdvancedTableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, new SimpleContainer(1), data.readBlockPos());
    }

    public AdvancedTableMenu(int containerId, Inventory inventory, Container tableInventory, BlockPos pos) {
        super(EPlusRegistry.ADVANCED_TABLE_MENU.get(), containerId);
        this.tableInventory = tableInventory;
        tableInventory.startOpen(inventory.player);
        this.logic = new EnchantmentLogicController(inventory.player, inventory.player.level(), pos, tableInventory);

        // Enchantment slot
        this.addSlot(new Slot(tableInventory, 0, 37, 17) {
            @Override
            public void setChanged() {
                super.setChanged();
                AdvancedTableMenu.this.logic.onItemUpdated();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchLogic.canEnchant(stack);
            }
        });
        // Hotbar
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(inventory, x, 43 + 18 * x, 149));
        }
        // Inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(inventory, x + y * 9 + 9, 43 + 18 * x, 91 + y * 18));
            }
        }
        // Armor slots
        for (int y = 0; y < 4; y++) {
            this.addSlot(new EquipSlot(inventory, ARMOR_SLOTS[y], 39 - y, 7, 24 + y * 19));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        final Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            final ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0 || index >= INV_END) {
                // Table or armor -> player inventory
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else {
                final Slot enchantSlot = this.slots.get(0);
                final EquipmentSlot equip = player.getEquipmentSlotForItem(stack);
                if (!enchantSlot.hasItem() && enchantSlot.mayPlace(stack)) {
                    enchantSlot.setByPlayer(stack.split(1));
                    if (stack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                    }
                    else {
                        slot.setChanged();
                    }
                    // Only ever move one item into the table, don't let shift-click repeat.
                    return ItemStack.EMPTY;
                }
                else if (equip.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get(INV_END + 3 - equip.getIndex()).hasItem()) {
                    if (!this.moveItemStackTo(stack, INV_END + 3 - equip.getIndex(), INV_END + 4 - equip.getIndex(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
                else if (index < INV_START + 9) {
                    // Hotbar -> main inventory
                    if (!this.moveItemStackTo(stack, INV_START + 9, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                else if (!this.moveItemStackTo(stack, INV_START, INV_START + 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().getBlockEntity(this.logic.getPos()) instanceof AdvancedTableBlockEntity || player.level().isClientSide()) {
            return player.isAlive() && player.distanceToSqr(Vec3.atCenterOf(this.logic.getPos())) <= 64.0D;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.tableInventory.stopOpen(player);
    }

    /** Copy of the (package private) vanilla armor slot. */
    private static class EquipSlot extends Slot {

        private final Player owner;
        private final EquipmentSlot slot;

        EquipSlot(Inventory inventory, EquipmentSlot slot, int index, int x, int y) {
            super(inventory, index, x, y);
            this.owner = inventory.player;
            this.slot = slot;
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            this.owner.onEquipItem(this.slot, previous, stack);
            super.setByPlayer(stack, previous);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(this.slot, this.owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            final ItemStack stack = this.getItem();
            return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(player);
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return ARMOR_ICONS.get(this.slot);
        }
    }
}
