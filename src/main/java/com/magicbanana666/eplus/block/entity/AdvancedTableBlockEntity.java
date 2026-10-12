package com.magicbanana666.eplus.block.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.EnchLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The advanced table keeps a separate one-slot inventory for every player. */
public class AdvancedTableBlockEntity extends BookBlockEntity {

    private static final Codec<StoredInventory> STORED_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("Owner").forGetter(StoredInventory::owner),
            ItemStack.CODEC.fieldOf("Item").forGetter(StoredInventory::stack)
    ).apply(i, StoredInventory::new));

    private final Map<UUID, TableInventory> inventories = new HashMap<>();

    public AdvancedTableBlockEntity(BlockPos pos, BlockState state) {
        super(EPlusRegistry.ADVANCED_TABLE_BE.get(), pos, state);
    }

    public TableInventory getInventory(Player player) {
        return this.inventories.computeIfAbsent(player.getUUID(), uuid -> new TableInventory());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        final ValueOutput.TypedOutputList<StoredInventory> list = output.list("InvList", STORED_CODEC);
        this.inventories.forEach((owner, inv) -> {
            if (!inv.getItem(0).isEmpty()) {
                list.add(new StoredInventory(owner, inv.getItem(0)));
            }
        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventories.clear();
        input.listOrEmpty("InvList", STORED_CODEC).stream().forEach(stored -> {
            final TableInventory inv = new TableInventory();
            inv.setItem(0, stored.stack());
            this.inventories.put(stored.owner(), inv);
        });
    }

    /** Breaking the table drops the items of every player. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            for (final TableInventory inventory : this.inventories.values()) {
                Containers.dropContents(this.level, pos, inventory);
            }
        }
        this.inventories.clear();
    }

    private record StoredInventory(UUID owner, ItemStack stack) {
    }

    public class TableInventory extends SimpleContainer {

        TableInventory() {
            super(1);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return EnchLogic.canEnchant(stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AdvancedTableBlockEntity.this.setChanged();
        }
    }
}
