package com.magicbanana666.eplus.block.entity;

import com.magicbanana666.eplus.EPlusRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class DecorativeBookBlockEntity extends BookBlockEntity {

    public static final float MAX_HEIGHT = 0.35f;
    private float height = 0f;

    public DecorativeBookBlockEntity(BlockPos pos, BlockState state) {
        super(EPlusRegistry.DECORATIVE_BOOK_BE.get(), pos, state);
    }

    public float getHeight() {
        return this.height;
    }

    /** Moves the book up (feather) or down (iron ingot). Returns whether it moved. */
    public boolean moveHeight(float delta) {
        final float newHeight = Math.max(-MAX_HEIGHT, Math.min(MAX_HEIGHT, Math.round((this.height + delta) * 100f) / 100f));
        if (newHeight == this.height) {
            return false;
        }
        this.height = newHeight;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("Height", this.height);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.height = input.getFloatOr("Height", 0f);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
