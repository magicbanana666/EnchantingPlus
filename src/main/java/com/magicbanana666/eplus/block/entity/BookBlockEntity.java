package com.magicbanana666.eplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A block entity with a floating book that opens and turns towards nearby players, like the enchanting table. */
public abstract class BookBlockEntity extends BlockEntity {

    private static final RandomSource RANDOM = RandomSource.create();

    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;

    protected BookBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isOpen() {
        return this.open >= 1f;
    }

    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state, BookBlockEntity entity) {
        entity.oOpen = entity.open;
        entity.oRot = entity.rot;
        final Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            final double xd = player.getX() - (pos.getX() + 0.5);
            final double zd = player.getZ() - (pos.getZ() + 0.5);
            entity.tRot = (float) Mth.atan2(zd, xd);
            entity.open += 0.1F;
            if (entity.open < 0.5F || RANDOM.nextInt(40) == 0) {
                final float old = entity.flipT;
                do {
                    entity.flipT = entity.flipT + (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (old == entity.flipT);
            }
        }
        else {
            entity.tRot += 0.02F;
            entity.open -= 0.1F;
        }
        while (entity.rot >= (float) Math.PI) {
            entity.rot -= (float) (Math.PI * 2);
        }
        while (entity.rot < (float) -Math.PI) {
            entity.rot += (float) (Math.PI * 2);
        }
        while (entity.tRot >= (float) Math.PI) {
            entity.tRot -= (float) (Math.PI * 2);
        }
        while (entity.tRot < (float) -Math.PI) {
            entity.tRot += (float) (Math.PI * 2);
        }
        float rotDir = entity.tRot - entity.rot;
        while (rotDir >= (float) Math.PI) {
            rotDir -= (float) (Math.PI * 2);
        }
        while (rotDir < (float) -Math.PI) {
            rotDir += (float) (Math.PI * 2);
        }
        entity.rot += rotDir * 0.4F;
        entity.open = Mth.clamp(entity.open, 0.0F, 1.0F);
        entity.time++;
        entity.oFlip = entity.flip;
        float diff = (entity.flipT - entity.flip) * 0.4F;
        diff = Mth.clamp(diff, -0.2F, 0.2F);
        entity.flipA = entity.flipA + (diff - entity.flipA) * 0.9F;
        entity.flip = entity.flip + entity.flipA;
        // Decorative books give a comparator signal while they are open.
        if (entity.oOpen != entity.open && (entity.open == 0f || entity.open == 1f) && !level.isClientSide()) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }
}
