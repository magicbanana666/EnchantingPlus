package com.magicbanana666.eplus.block;

import com.mojang.serialization.MapCodec;
import com.magicbanana666.eplus.EPlusConfig;
import com.magicbanana666.eplus.EPlusRegistry;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.block.entity.DecorativeBookBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A floating book. Counts as a bookshelf for enchanting tables, can be raised with a feather and lowered
 * with an iron ingot, and gives a comparator signal while it is open.
 */
public class DecorativeBookBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(5.6, 9.6, 5.6, 10.4, 14.4, 10.4);
    private final BookType type;

    public DecorativeBookBlock(BookType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public BookType getBookType() {
        return this.type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new DecorativeBookBlock(this.type, p));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof DecorativeBookBlockEntity book) {
            return SHAPE.move(0, book.getHeight(), 0);
        }
        return SHAPE;
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, BlockGetter level, BlockPos pos) {
        return EPlusConfig.floatingBookPower();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecorativeBookBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, EPlusRegistry.DECORATIVE_BOOK_BE.get(), BookBlockEntity::bookAnimationTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        final float delta = stack.is(Items.FEATHER) ? 0.05f : stack.is(Items.IRON_INGOT) ? -0.05f : 0f;
        if (delta == 0f) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DecorativeBookBlockEntity book && book.moveHeight(delta)) {
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, delta > 0 ? 1.2f : 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BookBlockEntity book && book.isOpen() ? 15 : 0;
    }
}
