package com.magicbanana666.eplus.block;

import com.magicbanana666.eplus.EPlusConfig;
import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.EnchLogic;
import com.magicbanana666.eplus.block.entity.AdvancedTableBlockEntity;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);
    private static final Component TITLE = Component.translatable("block.eplus.advanced_table");

    public AdvancedTableBlock(Properties properties) {
        super(properties);
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

    /** An enchanted golden apple empowers a table that stands on a netherite floor. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.ENCHANTED_GOLDEN_APPLE) || !(level.getBlockEntity(pos) instanceof AdvancedTableBlockEntity table) || table.isEmpowered()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!EnchLogic.isNetheriteFloor(level, pos)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("eplus.info.empower.floor"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            stack.consume(1, player);
            table.setEmpowered(true);
            serverLevel.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 80, 0.4, 0.5, 0.4, 0.4);
            player.sendOverlayMessage(Component.translatable("eplus.info.empower.done", EPlusConfig.empoweredMaxLevel()));
        }
        return InteractionResult.SUCCESS;
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
