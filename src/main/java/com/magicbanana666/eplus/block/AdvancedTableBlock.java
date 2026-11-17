package com.magicbanana666.eplus.block;

import com.mojang.serialization.MapCodec;
import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.block.entity.AdvancedTableBlockEntity;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class AdvancedTableBlock extends BaseEntityBlock {

    public static final MapCodec<AdvancedTableBlock> CODEC = simpleCodec(AdvancedTableBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);
    private static final Component TITLE = Component.translatable("block.eplus.advanced_table");

    public AdvancedTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, EPlusRegistry.ADVANCED_TABLE_BE.get(), AdvancedTableBlock::clientTick) : null;
    }

    private static void clientTick(Level level, BlockPos pos, BlockState state, AdvancedTableBlockEntity table) {
        BookBlockEntity.bookAnimationTick(level, pos, state, table);
        // Ambient visuals are owned by BookRenderer. Do not layer the legacy
        // full-moon portal ring or treasure dust over its permanent aura.
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        for (final BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(16) == 0 && EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                        offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1.0F, offset.getZ() + random.nextFloat() - 0.5);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AdvancedTableBlockEntity table) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AdvancedTableMenu(id, inventory, table.getInventory(p), pos), TITLE), pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
