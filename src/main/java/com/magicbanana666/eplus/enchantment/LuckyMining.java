package com.magicbanana666.eplus.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import com.magicbanana666.eplus.EPlusConfig;
import com.magicbanana666.eplus.EnchantingPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Lucky Mining: mining an ore has a chance to turn a stone block next to it into the same ore.
 * Mining the same ore in a row raises the chance, up to twice the base chance.
 * <p>
 * Ported from <a href="https://github.com/Buuz135/Lucky-Mining-MC">Lucky Mining</a> by Buuz135 (MIT).
 */
public final class LuckyMining {

    public static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, EnchantingPlus.id("lucky_mining"));
    /** Blocks that can be spread. */
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, EnchantingPlus.id("lucky_mining/ores"));
    /** Blocks that can be turned into ores. */
    public static final TagKey<Block> STONES = TagKey.create(Registries.BLOCK, EnchantingPlus.id("lucky_mining/stones"));

    private static final long STREAK_TIMEOUT_MILLIS = 3_000L;
    private static final int MAX_SCALING_STEPS = 10;
    private static final DustColorTransitionOptions SPREAD_PARTICLE = new DustColorTransitionOptions(0x54F841, 0xFFD000, 0.5F);
    private static final Map<ServerPlayer, MiningStreak> MINING_STREAKS = new WeakHashMap<>();

    private LuckyMining() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LuckyMining::onBlockBroken);
    }

    private static void onBlockBroken(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player) {
            tryToSpread(level, player, event.getPos(), event.getState());
        }
    }

    private static void tryToSpread(ServerLevel level, ServerPlayer player, BlockPos minedPos, BlockState minedState) {
        final long now = Util.getMillis();
        MiningStreak streak = MINING_STREAKS.get(player);
        if (streak != null && (streak.block() != minedState.getBlock() || now - streak.lastBreakMillis() > STREAK_TIMEOUT_MILLIS)) {
            MINING_STREAKS.remove(player);
            streak = null;
        }

        if (!minedState.is(ORES) || !player.hasCorrectToolForDrops(minedState)) {
            return;
        }

        // The enchantment is data driven, so a datapack may have removed it.
        final Optional<Holder.Reference<Enchantment>> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ENCHANTMENT);
        if (enchantment.isEmpty()) {
            return;
        }
        final int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), player.getMainHandItem());
        if (enchantmentLevel <= 0) {
            return;
        }

        final double tierChance = EPlusConfig.luckyMiningChance(enchantmentLevel);
        final int scalingSteps = streak == null ? 0 : streak.scalingSteps();
        final double maximumChance = Math.min(1.0, tierChance * 2.0);
        final double currentChance = Math.min(maximumChance, tierChance + scalingSteps * tierChance / 10.0);
        MINING_STREAKS.put(player, new MiningStreak(minedState.getBlock(), Math.min(MAX_SCALING_STEPS, scalingSteps + 1), now));

        if (level.getRandom().nextDouble() >= currentChance) {
            return;
        }

        final List<BlockPos> candidates = new ArrayList<>();
        for (final Direction direction : Direction.values()) {
            final BlockPos candidate = minedPos.relative(direction);
            if (level.getBlockState(candidate).is(STONES)) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        final BlockPos target = candidates.get(level.getRandom().nextInt(candidates.size()));
        if (!level.setBlockAndUpdate(target, minedState)) {
            return;
        }

        spawnParticles(level, target);
        level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 10.0F, 1.0F);
    }

    private static void spawnParticles(ServerLevel level, BlockPos target) {
        for (int xOffset = 0; xOffset <= 10; xOffset++) {
            for (int zOffset = 0; zOffset <= 10; zOffset++) {
                final double x = target.getX() + xOffset / 10.0;
                final double z = target.getZ() + zOffset / 10.0;
                final double risingSpeed = 0.2 + level.getRandom().nextDouble() * 0.4;
                final double stillSpeed = 0.01 + level.getRandom().nextDouble() * 0.04;
                level.sendParticles(SPREAD_PARTICLE, x, target.getY(), z, 0, 0.0, 1.0, 0.0, risingSpeed);
                level.sendParticles(SPREAD_PARTICLE, x, target.getY() + 1.0, z, 0, 0.0, 1.0, 0.0, stillSpeed);
            }
        }
    }

    private record MiningStreak(Block block, int scalingSteps, long lastBreakMillis) {
    }
}
